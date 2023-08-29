package com.koproxy.filter.impl

import com.koproxy.filter.GatewayFilter
import com.koproxy.filter.GatewayFilterChain
import com.koproxy.propertyEnum.PropertyEnum
import com.koproxy.utils.proxy.ForwardProxy
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Service
class RouteToRequestUrlFilter : GatewayFilter, Ordered {

    val ROUTE_TO_URL_FILTER_ORDER = 10000

    private val log = LoggerFactory.getLogger(RouteToRequestUrlFilter::class.java)

    override fun getOrder(): Int {
        return ROUTE_TO_URL_FILTER_ORDER
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val forwardProxy: ForwardProxy? = exchange.getAttribute(PropertyEnum.GET_PROXY.name)
        if (forwardProxy == null) {
            log.error("proxy not found")
            return chain.filter(exchange)
        }

        exchange.attributes[PropertyEnum.GATEWAY_REQUEST_URL_ATTR.name] = forwardProxy
        return chain.filter(exchange)
    }
}
