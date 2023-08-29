package com.koproxy.filter.impl

import com.koproxy.filter.GatewayFilter
import com.koproxy.filter.GatewayFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class DefaultGatewayFilterChain : GatewayFilterChain {

    private var index = 0

    private var filters: List<GatewayFilter>

    constructor(filters: List<GatewayFilter>) {
        this.filters = filters
        index = 0
    }

    constructor(
        parent: DefaultGatewayFilterChain,
        index: Int,
    ) {
        filters = parent.getFilters()
        this.index = index
    }

    private fun getFilters(): List<GatewayFilter> {
        return filters
    }

    override fun filter(exchange: ServerWebExchange): Mono<Void> {
        return Mono.defer {
            if (index < filters.size) {
                val filter = filters[index]
                val chain = DefaultGatewayFilterChain(
                    this,
                    index + 1,
                )
                return@defer filter.filter(exchange, chain)
            } else {
                return@defer Mono.empty()
            }
        }
    }
}
