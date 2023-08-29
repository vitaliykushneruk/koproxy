package com.koproxy.config.filter

import com.koproxy.utils.jwt.JwtTokenUtil
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Service
@ConditionalOnProperty(prefix = "epa", name = ["crash"], havingValue = "true")
class JwtProxyWebFilter(val jwtTokenUtil: JwtTokenUtil) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return ReactiveSecurityContextHolder.getContext()
            .doOnNext { sc ->
                run {
                    if (sc.authentication != null) {
                        exchange.mutate().request(
                            exchange.request.mutate().header(
                                "Authorization",
                                "Bearer ${jwtTokenUtil.generateToken(sc.authentication.principal as UserDetails)}",
                            ).build(),
                        ).build()
                    }
                }
            }.then(chain.filter(exchange))
    }
}
