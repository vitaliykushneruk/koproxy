package com.koproxy.config

import com.koproxy.handler.FilteringWebHandler
import com.koproxy.handler.RoutePredicateHandlerMapping
import com.koproxy.utils.ProxyUtil
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ResourceUtils
import org.springframework.web.reactive.DispatcherHandler
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.tcp.SslProvider.SslContextSpec
import reactor.netty.tcp.TcpClient
import java.security.KeyStore
import java.time.Duration
import javax.net.ssl.KeyManagerFactory

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(HttpHandlerAutoConfiguration::class, WebFluxAutoConfiguration::class)
@ConditionalOnClass(DispatcherHandler::class)
@EnableConfigurationProperties(HttpClientProperties::class)
@EnableCaching
class ProxySrAutoConfiguration {

    private val log = LoggerFactory.getLogger(ProxySrAutoConfiguration::class.java)

    @Bean
    fun routePredicateHandlerMapping(
        webHandler: FilteringWebHandler,
        proxyUtil: ProxyUtil,
    ): RoutePredicateHandlerMapping {
        return RoutePredicateHandlerMapping(webHandler, proxyUtil)
    }

    @Bean
    fun filteringWebHandler(): FilteringWebHandler {
        return FilteringWebHandler()
    }

    @Bean
    @ConditionalOnMissingBean
    fun httpClient(properties: com.koproxy.config.HttpClientProperties): HttpClient {
        val ssl: com.koproxy.config.HttpClientProperties.Ssl = properties.ssl

        // configure pool resources
        val connectionProvider = buildConnectionProvider(properties)

        var httpClient = HttpClient.create(connectionProvider)

        httpClient.tcpConfiguration { tcpClient: TcpClient ->
            if (properties.connectTimeout != null) {
                return@tcpConfiguration tcpClient.option(
                    ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    properties.connectTimeout,
                )
            }
            tcpClient
        }

        if (ssl.mtlsEnabled || ssl.trustStore != null && ssl.trustStore!!.isNotEmpty() || ssl.isUseInsecureTrustManager) {
            httpClient = httpClient.secure { sslContextSpec: SslContextSpec ->
                // configure ssl
                var sslContextBuilder = SslContextBuilder.forClient()

                if (ssl.trustStore != null && ssl.trustStore!!.isNotEmpty() || ssl.isUseInsecureTrustManager) {
                    if (ssl.isUseInsecureTrustManager) {
                        sslContextBuilder = sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE)
                    } else {
                        sslContextBuilder = sslContextBuilder.trustManager(ssl.trustStoreManagerFactory)
                    }
                }
                if (ssl.mtlsEnabled) {
                    if (ssl.mtlsCert != null && ssl.mtlsCert!!.isNotEmpty()) {
                        try {
                            val appKeyStore: KeyStore = KeyStore.getInstance("PKCS12")
                            val passwordToCharArray = ssl.mtlsKeyPassword!!.toCharArray()
                            appKeyStore.load(
                                ResourceUtils.getFile(ssl.mtlsKey!!).inputStream(),
                                passwordToCharArray,
                            )

                            val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
                            keyManagerFactory.init(appKeyStore, passwordToCharArray)
                            sslContextBuilder = sslContextBuilder.keyManager(keyManagerFactory)
                        } catch (e: Exception) {
                            log.error("", e)
                        }
                    } else {
                        log.error("")
                    }
                }
                sslContextSpec.sslContext(sslContextBuilder).defaultConfiguration(ssl.defaultConfigurationType)
                    .handshakeTimeout(ssl.handshakeTimeout).closeNotifyFlushTimeout(ssl.closeNotifyFlushTimeout)
                    .closeNotifyReadTimeout(ssl.closeNotifyReadTimeout)
            }
        }

        return httpClient
    }

    private fun buildConnectionProvider(properties: HttpClientProperties): ConnectionProvider {
        val pool: HttpClientProperties.Pool = properties.pool
        val connectionProvider: ConnectionProvider = if (pool.type == HttpClientProperties.Pool.PoolType.DISABLED) {
            ConnectionProvider.newConnection()
        } else {
            // create either Fixed or Elastic pool
            val builder = ConnectionProvider.builder(pool.name)
            if (pool.type == HttpClientProperties.Pool.PoolType.FIXED) {
                builder.maxConnections(pool.maxConnections).pendingAcquireMaxCount(-1)
                    .pendingAcquireTimeout(Duration.ofSeconds(pool.acquireTimeout))
            } else {
                // Elastic
                builder.maxConnections(Int.MAX_VALUE).pendingAcquireTimeout(Duration.ofMillis(0))
                    .pendingAcquireMaxCount(-1)
            }
            if (pool.maxIdleTime != null) {
                builder.maxIdleTime(pool.maxIdleTime!!)
            } else {
                builder.maxIdleTime(Duration.ofSeconds(1800))
            }
            if (pool.maxLifeTime != null) {
                builder.maxLifeTime(pool.maxLifeTime!!)
            } else {
                builder.maxLifeTime(Duration.ofSeconds(1800))
            }
            builder.evictInBackground(pool.evictionInterval)
            builder.metrics(pool.isMetrics)
            builder.build()
        }
        return connectionProvider
    }
}
