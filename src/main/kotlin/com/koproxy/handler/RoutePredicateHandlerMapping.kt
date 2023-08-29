package com.koproxy.handler

import com.koproxy.propertyEnum.PropertyEnum
import com.koproxy.utils.ProxyUtil
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.handler.AbstractHandlerMapping
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.Locale

class RoutePredicateHandlerMapping(
    private val filteringWebHandler: FilteringWebHandler,
    private val proxyUtil: ProxyUtil,
) : AbstractHandlerMapping() {

    init {
        order = 1
    }

    override fun getHandlerInternal(exchange: ServerWebExchange): Mono<*> {
        return mono {
            val headers = exchange.request.headers
            val token = headers.getFirst(HttpHeaders.AUTHORIZATION)

            val forwardProxy = proxyUtil.getForwardProxy(exchange.request.uri, extractTokenFromAuthHeader(token))
            if (forwardProxy != null) {
                exchange.attributes[PropertyEnum.GET_PROXY.name] = forwardProxy
                return@mono filteringWebHandler
            } else {
                null
            }
        }
    }

    private fun extractTokenFromAuthHeader(authorization: String?): Boolean {
        if (authorization == null) {
            return false
        }
        if (!authorization.lowercase(Locale.getDefault()).startsWith("bearer ")
        ) {
            return false
        }
        return true
    }
}
