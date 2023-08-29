package com.koproxy.proxy

import com.koproxy.utils.ExcludeServiceImpl
import com.koproxy.utils.ProxyUtilImpl
import com.koproxy.utils.proxy.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.net.URI

@ExtendWith(MockitoExtension::class)
class ProxyApplicationTests {

    @InjectMocks
    lateinit var proxyUtil: ProxyUtilImpl

    @Mock
    lateinit var routesServiceImpl: RoutesServiceImpl

    @Spy
    lateinit var excludeServiceImpl: ExcludeServiceImpl

    @Test
    fun contextLoadsDisable() = runBlocking {
        var proxy = setOf(
            Proxy(
                path = """**/ui-api-web/cross/somedomain/dev/1/test/api""",
                url = "http://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = """**/ui-api-web/cross/somedomain/dev/1/test/api/""",
                url = "http://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = """**/ui-api-web/cross/somedomain/dev/test/**/api""",
                url = """http://koproxy.com/api""",
                type = "BACKEND",
            ),
            Proxy(
                path = """**/ui-api-web/cross/somedomain/dev/test/**/api/""",
                url = """http://koproxy.com/api""",
                type = "BACKEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/externallink/21",
                url = "https://koproxy.com/project/TSDS_Spomni_tsds_dynamic_settings_mfDevelopWorkflow?mode=builds#all-projects",
                type = "EXTERNAL",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/externallink/21/",
                url = "https://koproxy.com/project/TSDS_Spomni_tsds_dynamic_settings_mfDevelopWorkflow?mode=builds#all-projects",
                type = "EXTERNAL",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/3/tslg-log-query/api",
                url = "https://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/3/tslg-log-query/api/",
                url = "https://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/3/tslg-log-query-mf/169",
                url = "https://koproxy.com",
                type = "FRONTEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/3/tslg-log-query-mf/169/",
                url = "https://koproxy.com",
                type = "FRONTEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/3/tslg-log-query-mf-mf",
                url = "https://koproxy.com",
                type = "FRONTEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/3/tslg-log-query-mf-mf/",
                url = "https://koproxy.com",
                type = "FRONTEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/3/tslg-log-query-mf/**-mf",
                url = "https://koproxy.com",
                type = "FRONTEND",
            ),
            Proxy(
                path = "**/ui-api-web/cross/somedomain/3/166/standin/api",
                url = "https://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = "**/ui-api-web/cross/somedomain/3/166/standin/api/",
                url = "https://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = "**/ui-api-web/cross/somedomain/3/standin/**/api",
                url = "https://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = "**/ui-api-web/cross/somedomain/3/standin/**/api/",
                url = "https://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = "**/ui/cross/somedomain/1/dko-deals/api/",
                url = "https://koproxy.com/api",
                type = "BACKEND",
            ),
            Proxy(
                path = """**/ui-api-web/cross/somedomain""",
                url = "http://localhost:8091",
                type = "BACKEND",
            ),
            Proxy(
                path = """**/ui-api-web/cross/somedomain/""",
                url = "http://localhost:8091",
                type = "BACKEND",
            ),
        )
        ReflectionTestUtils.setField(excludeServiceImpl, "backendUrl", "/ui-api-web/cross/somedomain")
        ReflectionTestUtils.setField(excludeServiceImpl, "frontendUrl", "/ui/cross/somedomain")
        ReflectionTestUtils.setField(proxyUtil, "epaEnabled", true)
        ReflectionTestUtils.setField(
            proxyUtil,
            "redirectToUrl",
            "https://koproxy.com/ui/cross/somedomain/dv/",
        )
//        Mockito.`when`(routesServiceImpl.getProxyAsync()).thenReturn(proxy.toMutableSet())
        ReflectionTestUtils.setField(proxyUtil, "proxys", proxy.toMutableSet())
        assertNull(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/namespace"),
                true,
            ),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/dev/1/test/api/messages?apa=dsds"),
                true,
            )!!.forwardProxy,
            URI.create("http://koproxy.com/api/messages?apa=dsds"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/1/dko-deals/api"),
                true,
            )!!.forwardProxy,
            URI.create("https://koproxy.com/api"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/dev/1/test/api/messages/messages?apa=dsds"),
                true,
            )!!.forwardProxy,
            URI.create("http://koproxy.com/api/messages/messages?apa=dsds"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/dev/1/test/api?apa=dsds"),
                true,
            )!!.forwardProxy,
            URI.create("http://koproxy.com/api?apa=dsds"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/dev/test/olo/api?apa=dsds"),
                true,
            )!!.forwardProxy,
            URI.create("http://koproxy.com/api?apa=dsds"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/dev/test/olo/bobo?apa=dsds"),
                true,
            )!!.forwardProxy,
            URI.create("http://localhost:8091/dev/test/olo/bobo?apa=dsds"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui-api-web/cross/somedomain/bububu"), true)!!.forwardProxy,
            URI.create("http://localhost:8091/bububu"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui-api-web/cross/somedomain"), true)!!.forwardProxy,
            URI.create("http://localhost:8091"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain?alolo=ololo"),
                true,
            )!!.forwardProxy,
            URI.create("http://localhost:8091?alolo=ololo"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui/cross/somedomain/externallink/21"), true)!!.forwardProxy,
            URI.create("https://koproxy.com/project/TSDS_Spomni_tsds_dynamic_settings_mfDevelopWorkflow?mode=builds#all-projects"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/3/tslg-log-query/api?apa=dsds&ara=llo"),
                true,
            )!!.forwardProxy,
            URI.create("https://koproxy.com/api?apa=dsds&ara=llo"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/3/tslg-log-query-mf/169?apa=dsds&ara=llo"),
                true,
            )!!.forwardProxy,
            URI.create("https://koproxy.com?apa=dsds&ara=llo"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/3/tslg-log-query-mf-mf"),
                true,
            )!!.forwardProxy,
            URI.create("https://koproxy.com"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/3/tslg-log-query-mf/ququ-mf?apa=dsds&ara=llo"),
                true,
            )!!.forwardProxy,
            URI.create("https://koproxy.com?apa=dsds&ara=llo"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/3/166/standin/api"),
                true,
            )!!.forwardProxy,
            URI.create("https://koproxy.com/api"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui-api-web/cross/somedomain/3/standin/ololo/api"),
                true,
            )!!.forwardProxy,
            URI.create("https://koproxy.com/api"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui/cross/somedomain"), true)!!.forwardProxy,
            URI.create("http://somedomain-ui-root:9000"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui/cross/somedomain/apa"), true)!!.forwardProxy,
            URI.create("http://somedomain-ui-root:9000/apa"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/apa/apa?alo=ololo"),
                true,
            )!!.forwardProxy,
            URI.create("http://somedomain-ui-root:9000/apa/apa?alo=ololo"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui/cross/somedomain/apa?alo=olol"), true)!!.forwardProxy,
            URI.create("http://somedomain-ui-root:9000/apa?alo=olol"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui/cross/somedomain?alo=olol"), true)!!.forwardProxy,
            URI.create("http://somedomain-ui-root:9000?alo=olol"),
        )

        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui/cross/somedomain/navbar-mf/apa"), true)!!.forwardProxy,
            URI.create("http://somedomain-ui-navbar:8082/apa"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/navbar-mf/apa?alo=ololo"),
                true,
            )!!.forwardProxy,
            URI.create("http://somedomain-ui-navbar:8082/apa?alo=ololo"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/navbar-mf?alo=olol"),
                true,
            )!!.forwardProxy,
            URI.create("http://somedomain-ui-navbar:8082?alo=olol"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost:8080/ui/cross/somedomain/navbar-mf"), true)!!.forwardProxy,
            URI.create("http://somedomain-ui-navbar:8082"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/static/importmap/importmap-root.json"),
                true,
            )!!.forwardProxy,
            URI.create("http://localhost:8091/static/importmap/importmap-root.json"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/static/importmap/dev/importmap.json"),
                true,
            )!!.forwardProxy,
            URI.create("http://localhost:8091/static/importmap/dev/importmap.json"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("localhost:8080/ui/cross/somedomain/navbar-mf/inno-navbar.js"),
                true,
            )!!.forwardProxy,
            URI.create("http://somedomain-ui-navbar:8082/inno-navbar.js"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(
                URI.create("http://localhost:8080/ui/dsfds/fsdfsd/fsdf/sdf/dsf/"),
                true,
            )!!.forwardProxy,
            URI.create("http://somedomain-ui-root:9000"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("http://localhost:8080"), true)!!.forwardProxy,
            URI.create("https://koproxy.com/ui/cross/somedomain/dv/"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("http://localhost:8080/"), true)!!.forwardProxy,
            URI.create("https://koproxy.com/ui/cross/somedomain/dv/"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost"), true)!!.forwardProxy,
            URI.create("https://koproxy.com/ui/cross/somedomain/dv/"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("http:localhost"), true)!!.forwardProxy,
            URI.create("https://koproxy.com/ui/cross/somedomain/dv/"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost/"), true)!!.forwardProxy,
            URI.create("https://koproxy.com/ui/cross/somedomain/dv/"),
        )
        assertEquals(
            proxyUtil.getForwardProxy(URI.create("localhost/tsts"), true)!!.forwardProxy,
            URI.create("http://somedomain-ui-root:9000"),
        )
    }
}
