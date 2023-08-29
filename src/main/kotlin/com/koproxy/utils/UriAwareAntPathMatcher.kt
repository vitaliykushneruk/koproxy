package com.koproxy.utils

import org.springframework.util.AntPathMatcher
import java.net.URI

class UriAwareAntPathMatcher : AntPathMatcher() {
    override fun match(pattern: String, path: String): Boolean {
        val pathHost = URI(path).host
        val patternHost = extractHostFromPattern(pattern)

        return (if (patternHost != null) (patternHost == pathHost) else true) && super.match(
            extractPatternWithoutHost(
                pattern
            ),
            path
        )
    }

    fun extractHostFromPattern(pattern: String): String? {
        val removePrefix = pattern.replace("**", "")
        val uri = URI("http://$removePrefix")
        return uri.host
    }

    fun extractPatternWithoutHost(pattern: String): String {
        val startIndex = pattern.indexOf('/')
        return if (startIndex != -1) "**${pattern.substring(startIndex)}" else "**/"
    }
}
