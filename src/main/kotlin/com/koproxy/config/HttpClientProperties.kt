package com.koproxy.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.server.WebServerException
import org.springframework.util.ResourceUtils
import org.springframework.validation.annotation.Validated
import reactor.netty.resources.ConnectionProvider
import reactor.netty.tcp.SslProvider.DefaultConfigurationType
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchProviderException
import java.time.Duration
import javax.net.ssl.TrustManagerFactory

@Validated
@ConfigurationProperties("proxy.httpclient")
class HttpClientProperties {
    /** The connect timeout in millis, the default is 30m.  */
    var connectTimeout: Int? = 300000

    /** The response timeout.  */
    var responseTimeout: Duration? = null

    /** Pool configuration for Netty HttpClient.  */
    var pool = com.koproxy.config.HttpClientProperties.Pool()

    /** SSL configuration for Netty HttpClient.  */
    var ssl = com.koproxy.config.HttpClientProperties.Ssl()

    class Pool {
        /** Type of pool for HttpClient to use, defaults to ELASTIC.  */
        var type = com.koproxy.config.HttpClientProperties.Pool.PoolType.ELASTIC

        /** The channel pool map name, defaults to proxy.  */
        var name = "proxy"

        /**
         * Only for type FIXED, the maximum number of connections before starting pending
         * acquisition on existing ones.
         */
        var maxConnections = ConnectionProvider.DEFAULT_POOL_MAX_CONNECTIONS

        /** Only for type FIXED, the maximum time in millis to wait for acquiring.  */
        var acquireTimeout = ConnectionProvider.DEFAULT_POOL_ACQUIRE_TIMEOUT

        /**
         * Time in millis after which the channel will be closed. If NULL, there is no max
         * idle time.
         */
        var maxIdleTime: Duration? = null

        /**
         * Duration after which the channel will be closed. If NULL, there is no max life
         * time.
         */
        var maxLifeTime: Duration? = null

        /**
         * Perform regular eviction checks in the background at a specified interval.
         * Disabled by default ([Duration.ZERO])
         */
        var evictionInterval = Duration.ZERO

        /**
         * Enables channel pools metrics to be collected and registered in Micrometer.
         * Disabled by default.
         */
        var isMetrics = false

        enum class PoolType {
            /**
             * Elastic pool type.
             */
            ELASTIC,

            /**
             * Fixed pool type.
             */
            FIXED,

            /**
             * Disabled pool type.
             */
            DISABLED,
        }
    }

    class Ssl {
        // TODO: support configuration of other trust manager factories
        /**
         * Installs the netty InsecureTrustManagerFactory. This is insecure and not
         * suitable for production.
         */
        var isUseInsecureTrustManager = false

        // use netty default SSL timeouts
        /** SSL handshake timeout. Default to 10000 ms  */
        var handshakeTimeout = Duration.ofMillis(10000)

        /** SSL close_notify flush timeout. Default to 3000 ms.  */
        var closeNotifyFlushTimeout = Duration.ofMillis(3000)

        /** SSL close_notify read timeout. Default to 0 ms.  */
        var closeNotifyReadTimeout = Duration.ZERO

        /** The default ssl configuration type. Defaults to TCP.  */
        var defaultConfigurationType = DefaultConfigurationType.TCP

        /** MtlsEnabled HttpClient.  */
        var mtlsEnabled: Boolean = false

        /** mtlsCert path for Netty HttpClient.  */
        var mtlsCert: String? = null

        /** mtlsKey path for Netty HttpClient.  */
        var mtlsKey: String? = null

        /** TrustStore path for Netty HttpClient.  */
        var trustStore: String? = null

        /** TrustStore password  */
        var trustStorePassword: String? = null

        var mtlsKeyPassword: String? = null

        /** TrustStore type for Netty HttpClient, default is JKS.  */
        var trustStoreType = "JKS"

        /** Keystore provider for Netty HttpClient, optional field.  */
        var trustStoreProvider: String? = null

        val trustStoreManagerFactory: TrustManagerFactory?
            get() {
                return try {
                    if (trustStore != null && trustStore!!.isNotEmpty()) {
                        val trustManagerFactory = TrustManagerFactory
                            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                        trustManagerFactory.init(createTrustStore())
                        return trustManagerFactory
                    }
                    null
                } catch (e: Exception) {
                    throw IllegalStateException(e)
                }
            }

        private fun createTrustStore(): KeyStore {
            return try {
                val store = if (trustStoreProvider!!.isNotEmpty()) {
                    KeyStore.getInstance(
                        trustStoreType,
                        trustStoreProvider,
                    )
                } else {
                    KeyStore.getInstance(trustStoreType)
                }
                try {
                    val url = ResourceUtils.getURL(trustStore!!)
                    store.load(
                        url.openStream(),
                        if (trustStorePassword != null) trustStorePassword!!.toCharArray() else null,
                    )
                } catch (e: Exception) {
                    throw WebServerException("Could not load trust store ' " + trustStore + "'", e)
                }
                store
            } catch (e: KeyStoreException) {
                throw WebServerException("Could not load TrustStore for given type and provider", e)
            } catch (e: NoSuchProviderException) {
                throw WebServerException("Could not load TrustStore for given type and provider", e)
            }
        }
    }
}
