package com.koproxy.utils.jwt

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

@Disabled
@ExtendWith(MockitoExtension::class)
internal class JwtTokenUtilTest {

    @InjectMocks
    lateinit var jwtTokenUtil: JwtTokenUtil

    @Mock
    lateinit var httpClientProperties: com.koproxy.config.HttpClientProperties

    @Test
    internal fun name() {
        val ssl = com.koproxy.config.HttpClientProperties.Ssl()
        ssl.mtlsKey = "src/test/resources/keystore.p12"
        ssl.mtlsKeyPassword = "123456789"
        Mockito.`when`(httpClientProperties.ssl).thenReturn(ssl)
        val user: UserDetails = User.builder()
            .username("user")
            .password("{noop}user")
            .roles("USER")
            .build()
        assertNotNull(jwtTokenUtil.generateToken(user))
    }
}
