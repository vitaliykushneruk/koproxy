package com.koproxy.utils

import com.koproxy.utils.proxy.Proxy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

interface ExcludeService {
    suspend fun getExcludePattern(): Set<String>

    suspend fun getHardCodeProxy(): List<Proxy>
}

@Service
class ExcludeServiceImpl : ExcludeService {

    @Value("\${spa.backend-url}")
    private lateinit var backendUrl: String

    @Value("\${spa.frontend-url}")
    private lateinit var frontendUrl: String

    override suspend fun getExcludePattern(): Set<String> {
        return listOf(
            """**$backendUrl/standname**""",
            """**$backendUrl/standname/**""",
            """**$backendUrl/namespace**""",
            """**$backendUrl/namespace/**""",
            """**/koproxy/**""",
            """**/login**""",
            """**/login/**""",
            """**/login.html**""",
        ).toSet()
    }

    override suspend fun getHardCodeProxy(): List<Proxy> {
        return listOf(
            Proxy("""**$backendUrl/actuator""", "http://localhost:8091/actuator", "BACKEND", 8, needAuth = false),
            Proxy("""**$backendUrl/actuator/""", "http://localhost:8091/actuator", "BACKEND", 9, needAuth = false),
            Proxy("""**$backendUrl""", "http://localhost:8091", "BACKEND", 10),
            Proxy("""**$backendUrl/""", "http://localhost:8091", "BACKEND", 11),
            Proxy("""**$frontendUrl/navbar-mf""", "http://somedomain-ui-navbar:8082", "FRONTEND", 12),
            Proxy("""**$frontendUrl/navbar-mf/""", "http://somedomain-ui-navbar:8082", "FRONTEND", 13),
            Proxy("""**$frontendUrl/app-mf""", "http://somedomain-ui-app:8083", "FRONTEND", 14),
            Proxy("""**$frontendUrl/app-mf/""", "http://somedomain-ui-app:8083", "FRONTEND", 15),
            Proxy("""**$frontendUrl/somedomain-mf-ui-gateway""", "http://somedomain-mf-ui-gateway:8084", "FRONTEND", 14),
            Proxy("""**$frontendUrl/somedomain-mf-ui-gateway/""", "http://somedomain-mf-ui-gateway:8084", "FRONTEND", 15),
            Proxy("""**$frontendUrl/somedomain-ui-config""", "http://somedomain-ui-config:8085", "FRONTEND", 14),
            Proxy("""**$frontendUrl/somedomain-ui-config/""", "http://somedomain-ui-config:8085", "FRONTEND", 15),
            Proxy("""**$frontendUrl/somedomain-ui-widgets""", "http://somedomain-ui-widgets:8081", "FRONTEND", 14),
            Proxy("""**$frontendUrl/somedomain-ui-widgets/""", "http://somedomain-ui-widgets:8081", "FRONTEND", 15),
            Proxy("""**$frontendUrl/static/importmap""", "http://localhost:8091/static/importmap", "FRONTEND", 16),
            Proxy("""**$frontendUrl/static/importmap/""", "http://localhost:8091/static/importmap", "FRONTEND", 17),
            Proxy("""**$frontendUrl""", "http://somedomain-ui-root:9000", "FRONTEND", 18),
            Proxy("""**$frontendUrl/""", "http://somedomain-ui-root:9000", "FRONTEND", 19),
            Proxy("""**/**""", "http://somedomain-ui-root:9000", "FRONTEND", 20),
        )
    }
}
