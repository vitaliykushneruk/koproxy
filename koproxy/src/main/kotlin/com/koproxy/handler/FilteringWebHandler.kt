package com.koproxy.handler

import com.koproxy.filter.GatewayFilter
import com.koproxy.filter.impl.DefaultGatewayFilterChain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebHandler
import reactor.core.publisher.Mono

class FilteringWebHandler : WebHandler {

    @Autowired(required = false)
    private lateinit var gatewayFilter: List<GatewayFilter>

    override fun handle(exchange: ServerWebExchange): Mono<Void> {
        val combined: List<GatewayFilter> = ArrayList(gatewayFilter)
        AnnotationAwareOrderComparator.sort(combined)
        return DefaultGatewayFilterChain(combined).filter(exchange)
    }
}
