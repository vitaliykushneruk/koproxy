package com.koproxy.proxy

import com.koproxy.utils.UriAwareAntPathMatcher
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class UriAwareAntPathMatcherTest {

    @Test
    fun match() {
        val pathMatcher = UriAwareAntPathMatcher()
        val pattern = "**koproxy.com.koproxy/ui-api-web/cross/somedomain**"
        val uri = "https://koproxy.com.koproxy/ui-api-web/cross/somedomain"

        assertTrue(pathMatcher.match(pattern, uri))

        val pattern1 = "**/ui-api-web/cross/somedomain**"
        val uri2 = "https://koproxy.com.koproxy/ui-api-web/cross/somedomain"

        assertTrue(pathMatcher.match(pattern1, uri2))
    }
}
