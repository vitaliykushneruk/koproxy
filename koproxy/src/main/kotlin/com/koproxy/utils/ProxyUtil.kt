package com.koproxy.utils

import com.koproxy.proxy.RoutesService
import com.koproxy.utils.proxy.ForwardProxy
import com.koproxy.utils.proxy.Proxy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.PathMatcher
import java.net.URI

interface ProxyUtil {
    suspend fun getForwardProxy(path: URI, isAuthToken: Boolean = false): ForwardProxy?

    fun getProxys()

    fun getProxysList(): MutableSet<Proxy>
}

@Component
class ProxyUtilImpl(val routesService: RoutesService, val excludeService: ExcludeService) : ProxyUtil {

    private val pathMatcher: PathMatcher = AntPathMatcher()

    private val log = LoggerFactory.getLogger(ProxyUtilImpl::class.java)

    @Value("\${epa.redirect_to_url}")
    private lateinit var redirectToUrl: String

    @Value("\${epa.enabled}")
    private var epaEnabled: Boolean = false

    @Value("\${epa.crash}")
    private var epaCrash: Boolean = false

    @Volatile
    public lateinit var proxys: MutableSet<Proxy>

    override suspend fun getForwardProxy(path: URI, isAuthToken: Boolean): ForwardProxy? {
        return getSimpleMatchingRoute(path.toASCIIString(), isAuthToken)
    }

    @Scheduled(fixedDelay = 60000)
    override fun getProxys() = runBlocking {
        log.info("getProxys")
        proxys = routesService.getProxyAsync().toMutableSet()
    }

    override fun getProxysList(): MutableSet<Proxy> {
        return proxys
    }

    protected suspend fun getSimpleMatchingRoute(
        path: String,
        isAuthToken: Boolean,
    ): ForwardProxy? {
        log.info("path {}", path)
        // Проверяем по паттерну исключений
        val excludePatterns = excludeService.getExcludePattern()
        for (exclude in excludePatterns) {
            if (this.pathMatcher.match(exclude, path)) {
                log.debug("use excludePattern {}", exclude)
                return null
            }
        }

        val proxy: Proxy? = getProxy(path, proxys)
        log.info("proxy {}", proxy)
        val forwardProxy = getRoute(proxy, path, isAuthToken)
        log.info("forwardProxy {}", forwardProxy)
        return forwardProxy
    }

    /***
     * Формируем URL итоговый куда нужно спроскировать, отбрасывая регулярное выражение
     */
    protected fun getRoute(proxy: Proxy?, path: String, isAuthToken: Boolean): ForwardProxy? {
        if (epaEnabled && !epaCrash && isUrlWithOnlyHost(path)) {
            return ForwardProxy(URI.create(redirectToUrl), true)
        }

        if (proxy == null) {
            return null
        }

        var proxyPath = proxyPath(proxy)

        // Если епаа включена и не передан токен авторизации нужно сделать редирект на епу
        if (epaEnabled && !epaCrash && !isAuthToken && proxy.needAuth) {
            val redirectToUrlSubString =
                if (redirectToUrl.endsWith("/")) {
                    redirectToUrl.substring(0, redirectToUrl.length - 1)
                } else {
                    redirectToUrl
                }
            return ForwardProxy(
                URI.create(
                    redirectToUrlSubString + path.replaceFirst(
                        proxyPath.toRegex(),
                        "",
                    ),
                ),
                token = proxy.token,
                redirectEnable = true,
            )
        } else {
            return ForwardProxy(
                URI.create(proxy.url + path.replaceFirst(proxyPath.toRegex(), "")),
                token = proxy.token,
            )
        }
    }

    private fun proxyPath(proxy: Proxy): String {
        var regularExpression = proxy.path

        if (regularExpression.endsWith("/")) {
            regularExpression = regularExpression.substring(0, regularExpression.length - 1)
        }

        regularExpression = regularExpression.replace("**", ".*")
        return regularExpression
    }

    protected suspend fun getProxy(adjustedPath: String, proxys: Set<Proxy>): Proxy? {
        val proxysWithHardCode = mutableSetOf<Proxy>()
        proxysWithHardCode.addAll(proxys)
        val hardCodeProxy = excludeService.getHardCodeProxy()
        proxysWithHardCode.addAll(hardCodeProxy)
        proxysWithHardCode.sortedBy { it.order }.toSet()

        for (proxy in proxysWithHardCode) {
            val proxyPath = proxy.path + "**"
            if (this.pathMatcher.isPattern(proxyPath)) {
                if (this.pathMatcher.match(proxyPath, adjustedPath)) {
                    return proxy
                }
            } else {
                log.error("incorrect pattern {}", proxy.path)
            }
        }
        return null
    }

    private fun isUrlWithOnlyHost(url: String): Boolean {
        val hostPattern = "^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*$".toRegex()
        var host = url.removePrefix("http://").removePrefix("https://")
        host = host.replace(":", "")
        val urlParts = host.split("/")
        return when (urlParts.size) {
            1 -> urlParts[0].matches(hostPattern)
            2 -> urlParts[0].matches(hostPattern) && urlParts[1].isEmpty()
            else -> false
        }
    }
}
