package com.koproxy.config

import com.koproxy.config.filter.JwtProxyWebFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.password.StandardPasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityProxyConfig {

    @Bean
    @ConditionalOnProperty(prefix = "epa", name = ["crash"], havingValue = "false", matchIfMissing = true)
    fun securityFilterChainPermitAll(http: ServerHttpSecurity): SecurityWebFilterChain? {
        return http.csrf().disable().authorizeExchange().anyExchange().permitAll().and().build()
    }

    @Bean
    @ConditionalOnProperty(prefix = "epa", name = ["crash"], havingValue = "true")
    fun securityFilterChain(http: ServerHttpSecurity, jwtProxyWebFilter: JwtProxyWebFilter): SecurityWebFilterChain? {
        return http
            .csrf().disable()
            .authorizeExchange()
            .pathMatchers("/login.html").permitAll()
            .anyExchange().authenticated()
            .and()
            .formLogin()
            .loginPage("/login.html")
            .and()
            .addFilterAfter(jwtProxyWebFilter, SecurityWebFiltersOrder.AUTHORIZATION)
            .build()
    }

    @Bean
    fun passwordEncoder(): StandardPasswordEncoder? {
        return StandardPasswordEncoder()
    }
}
