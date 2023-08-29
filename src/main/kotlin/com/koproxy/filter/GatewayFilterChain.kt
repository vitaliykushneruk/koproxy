package com.koproxy.filter

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

interface GatewayFilterChain {
    fun filter(exchange: ServerWebExchange): Mono<Void>
}
