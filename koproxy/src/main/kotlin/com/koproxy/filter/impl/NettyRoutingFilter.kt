package com.koproxy.filter.impl

import com.koproxy.config.HttpClientProperties
import com.koproxy.filter.GatewayFilter
import com.koproxy.filter.GatewayFilterChain
import com.koproxy.propertyEnum.PropertyEnum
import com.koproxy.utils.proxy.ForwardProxy
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.timeout.TimeoutException
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBuffer
import org.springframework.core.io.buffer.NettyDataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.AbstractServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClientResponse
import java.time.Duration

@Service
class NettyRoutingFilter(val httpClient: HttpClient, val properties: com.koproxy.config.HttpClientProperties) :
    GatewayFilter, Ordered {

    @Value("\${epa.enabled}")
    private var epaEnabled: Boolean = false

    @Value("\${epa.crash}")
    private var epaCrash: Boolean = false

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }

    private val log = LogFactory.getLog(NettyRoutingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val requestUrl = exchange.getRequiredAttribute<ForwardProxy>(PropertyEnum.GATEWAY_REQUEST_URL_ATTR.name)

        val scheme = requestUrl.forwardProxy.scheme

        val token = requestUrl.token

        if (!"http".equals(scheme, ignoreCase = true) && !"https".equals(
                scheme,
                ignoreCase = true,
            )
        ) {
            return chain.filter(exchange)
        }

        val request = exchange.request

        val url = requestUrl.forwardProxy.toASCIIString()

        val method = HttpMethod.valueOf(request.methodValue)

        val httpHeadersRequest = DefaultHttpHeaders()

        val headersRequest = exchange.request.headers

        headersRequest.forEach { name: String?, values: List<String> ->
            httpHeadersRequest[name] = values
            httpHeadersRequest.remove(HttpHeaders.HOST)
        }
        httpHeadersRequest["Admin-console"] = "true"

        if (!epaEnabled && !epaCrash) {
            httpHeadersRequest["Authorization"] =
                "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik9BalFoODMtU2g2S0VmTktpNGdDRmQxZVZmclVXZHl0VW5rUHdnRkRTVzQifQ.eyJleHAiOjE3OTY2ODk0ODUsImlhdCI6MTYxNjU4OTE4NSwianRpIjoiNjE4NjVhMDYtNWM4Mi00NjYxLWJjMzItZGUxYmE5YmU0OWMzIiwiaXNzIjoiaXN0YXJvc3RpbkBpbm5vLnRlY2giLCJzdWIiOiJxd2VydHkiLCJjaGFubmVsIjoiaW50ZXJuYWwiLCJub25jZSI6IjMyOWFjMzFlZDVmNjlmMzliNmE5ZDNlOTU3M2NlYjY5Iiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSJ9.pIq7kgXQLrrgfJ8xiyM59SR973tSd8Qt2eE4jjWlxxNWLg0CdZRyigjryvLm4tO52GouN11KsStmgzHok--Ak9zt35SlvsUhngdrVVLuCul-L49l1X-d6MiFxv2UQ6tIh8M-UNUV_StxBH_ojsw3vav3HDzte_sNS1mcJf9is0PPNb-IcFMLDH4lhFG7HorC_cPLgi2MuINs3AUi5-4fkZNCOGgElJDrjUtsMbvDtUxnlo5C-EsDaCkJQnaQRNDbnYaysCCTtjLR8BEIxbS484YG_r6MDbbVzBxsxR7t9HIqWSXyhWAjoBBgaYOM9CnBWdVxMb2s-UsdrUKFdTLGZQh_UC9Znr3k9UqHlpRtDzSxA2Ce1Lzxo59b_sivE6-aTO5UMmRy4AzONj1vomrJwaF9EBiMmODM-MKsK8kmUlUt0xR2Llw419XuahpEZI2XOWMjcgpgoK7U8cI5DFixNIvQIdotXhBIIZfpjGfPlFE9slO_l4GGQ--EoEkUtH-I0QYz85ax34ZlsSbOo6bcAYyZCFZmpdHCsVHKCFpmfz-D2k6iJQ0aDXO4aneppiWNacg_umjdXM4Be5Rc2uW8LFAFkK5cTSPZT06YpxpLaG15R9W-yZXBMbCE_-AxynOTsVOU8tPk53RaxdhY_MKr0PBHG28ohJd_pKQiJzDj6uc"
        } else if (token != null) {
            httpHeadersRequest["Authorization"] = "Bearer $token"
        }

        if (epaCrash) {
            log.info("JWT: ${httpHeadersRequest["Authorization"]}")
        }

        // redirect
        if (requestUrl.redirectEnable) {
            exchange.response.statusCode = HttpStatus.MOVED_PERMANENTLY
            exchange.response.headers.set(
                HttpHeaders.LOCATION,
                url,
            )
            return chain.filter(exchange)
        }

        val httpClientResponse = httpClient.headers { headers -> headers.add(httpHeadersRequest) }.request(method)
            .uri(url)
            .send { req, nettyOutbound ->
                nettyOutbound.send(request.body.map { dataBuffer -> getByteBuf(dataBuffer) })
            }
            .responseConnection { res, connection ->
                exchange.attributes[PropertyEnum.CLIENT_RESPONSE_CONN_ATTR.name] = connection
                val response = exchange.response
                setResponseStatus(res, response)

                val httpHeaderResponse = HttpHeaders()

                res.responseHeaders().forEach { header ->
                    httpHeaderResponse[header.key] = header.value
                }

                response.headers.putAll(httpHeaderResponse)
                Mono.just(res)
            }

        val responseTimeout: Duration? = getResponseTimeout()
        if (responseTimeout != null) {
            httpClientResponse
                .timeout(
                    responseTimeout,
                    Mono.error(com.koproxy.exception.TimeoutException("Response took longer than timeout: $responseTimeout")),
                )
                .onErrorMap(
                    TimeoutException::class.java,
                ) { th ->
                    ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        th.message,
                        th,
                    )
                }
        }

        return httpClientResponse.then(chain.filter(exchange))
    }

    protected fun getByteBuf(dataBuffer: DataBuffer): ByteBuf {
        if (dataBuffer is NettyDataBuffer) {
            return dataBuffer.nativeBuffer
        } else if (dataBuffer is DefaultDataBuffer) {
            return Unpooled.wrappedBuffer(dataBuffer.nativeBuffer)
        }
        throw IllegalArgumentException("Unable to handle DataBuffer of type " + dataBuffer.javaClass)
    }

    private fun setResponseStatus(clientResponse: HttpClientResponse, response: ServerHttpResponse) {
        var response = response
        val status = HttpStatus.resolve(clientResponse.status().code())
        if (status != null) {
            response.statusCode = status
        } else {
            while (response is ServerHttpResponseDecorator) {
                response = response.delegate
            }
            if (response is AbstractServerHttpResponse) {
                response.rawStatusCode =
                    clientResponse.status().code()
            } else {
                throw IllegalStateException(
                    "Unable to set status code " + clientResponse.status().code() +
                        " on response of type " + response.javaClass.name,
                )
            }
        }
    }

    private fun getResponseTimeout(): Duration? {
        return if (properties.responseTimeout != null) {
            properties.responseTimeout
        } else {
            null
        }
    }
}
