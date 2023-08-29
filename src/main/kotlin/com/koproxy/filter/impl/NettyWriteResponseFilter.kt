package com.koproxy.filter.impl

import com.koproxy.filter.GatewayFilter
import com.koproxy.filter.GatewayFilterChain
import com.koproxy.propertyEnum.PropertyEnum
import io.netty.buffer.ByteBuf
import org.apache.commons.logging.LogFactory
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.Connection

@Service
class NettyWriteResponseFilter(private val streamingMediaTypes: List<MediaType>) :
    GatewayFilter, Ordered {

    private val WRITE_RESPONSE_FILTER_ORDER = -1

    private val log = LogFactory.getLog(
        NettyWriteResponseFilter::class.java,
    )

    override fun getOrder(): Int {
        return WRITE_RESPONSE_FILTER_ORDER
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return chain.filter(exchange)
            .doOnError { throwable -> cleanup(exchange) }
            .then(
                Mono.defer {
                    val connection =
                        exchange.getAttribute<Connection>(PropertyEnum.CLIENT_RESPONSE_CONN_ATTR.name)
                            ?: return@defer Mono.empty()
                    if (log.isTraceEnabled) {
                        log.trace(
                            "NettyWriteResponseFilter start inbound: " +
                                connection.channel().id().asShortText() + ", outbound: " +
                                exchange.logPrefix,
                        )
                    }
                    val response = exchange.response

                    val body = connection
                        .inbound()
                        .receive()
                        .retain()
                        .map { byteBuf: ByteBuf ->
                            wrap(
                                byteBuf,
                                response,
                            )
                        }
                    var contentType: MediaType? = null
                    try {
                        contentType = response.headers.contentType
                    } catch (e: Exception) {
                        if (log.isTraceEnabled) {
                            log.trace("invalid media type", e)
                        }
                    }
                    if (isStreamingMediaType(contentType)) {
                        response.writeAndFlushWith(
                            body.map<Flux<DataBuffer>> { data: DataBuffer? ->
                                Flux.just(
                                    data,
                                )
                            },
                        )
                    } else {
                        response.writeWith(body)
                    }
                },
            ).doOnCancel { cleanup(exchange) }
    }

    protected fun wrap(byteBuf: ByteBuf, response: ServerHttpResponse): DataBuffer {
        val bufferFactory = response.bufferFactory()
        if (bufferFactory is NettyDataBufferFactory) {
            return bufferFactory.wrap(byteBuf)
        } else if (bufferFactory is DefaultDataBufferFactory) {
            val buffer: DataBuffer = bufferFactory.allocateBuffer(byteBuf.readableBytes())
            buffer.write(byteBuf.nioBuffer())
            byteBuf.release()
            return buffer
        }
        throw IllegalArgumentException("Unkown DataBufferFactory type " + bufferFactory.javaClass)
    }

    private fun cleanup(exchange: ServerWebExchange) {
        val connection = exchange.getAttribute<Connection>(PropertyEnum.CLIENT_RESPONSE_CONN_ATTR.name)
        if (connection != null && connection.channel().isActive && !connection.isPersistent) {
            connection.dispose()
        }
    }

    // TODO: use framework if possible
    private fun isStreamingMediaType(@Nullable contentType: MediaType?): Boolean {
        if (contentType != null) {
            for (i in streamingMediaTypes.indices) {
                if (streamingMediaTypes[i].isCompatibleWith(contentType)) {
                    return true
                }
            }
        }
        return false
    }
}
