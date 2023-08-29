package com.koproxy.proxy

import com.koproxy.cluster.ClusterRepository
import com.koproxy.cluster.entity.Cluster
import com.koproxy.external.ExternalRepository
import com.koproxy.external.entity.ExternalUrl
import com.koproxy.project.ProjectRepository
import com.koproxy.project.entity.Project
import com.koproxy.projectNode.ProjectNodeRepository
import com.koproxy.projectNode.entity.BackOptions
import com.koproxy.projectNode.entity.ProjectNode
import com.koproxy.utils.proxy.Proxy
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.AntPathMatcher
import org.springframework.util.PathMatcher

interface RoutesService {

    suspend fun getProxyAsync(): Set<Proxy>
}

@Service
class RoutesServiceImpl : RoutesService {

    @Value("\${spa.backend-url}")
    private lateinit var backendUrl: String

    @Value("\${spa.frontend-url}")
    private lateinit var frontendUrl: String

    @Autowired
    private lateinit var projectNodeRepository: ProjectNodeRepository

    @Autowired
    private lateinit var clusterRepository: com.koproxy.cluster.ClusterRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var externalRepository: ExternalRepository

    private val pathMatcher: PathMatcher = AntPathMatcher()

    private val log = LoggerFactory.getLogger(RoutesServiceImpl::class.java)

    override suspend fun getProxyAsync(): Set<Proxy> {
        log.info("start getProxy")
        val proxys: MutableSet<Proxy> = mutableSetOf()
        val projectNodeFlow = projectNodeRepository.findAll().toList()
        val clusterFlow = clusterRepository.findAll().toList()
        val projectFlow = projectRepository.findAll().toList()

        projectNodeFlow.forEach {
            val project = projectFlow.first { ij -> ij.id == it.projectId }
            val cluster = clusterFlow.first { ij -> ij.id == it.clusterId }

            if (!it.backendUrl.isNullOrEmpty() && it.backendUrls.isNullOrEmpty()) {
                proxys.addAll(backendEntry(it, project, cluster))
                if (it.general!!) {
                    proxys.addAll(backendDefaultEntry(it, project, cluster))
                }
            }
            if (!it.backendUrls.isNullOrEmpty()) {
                it.backendUrls!!.forEach { ij ->
                    proxys.addAll(backendEntryPrefix(it, ij, project, cluster))
                    if (it.general!!) {
                        proxys.addAll(backendDefaultEntryPrefix(it, ij, project, cluster))
                    }
                }
            }
            if (it.frontendUrl != null) {
                proxys.addAll(frontendEntry(it, project, cluster))
                if (it.general!!) {
                    proxys.addAll(frontendDefaultEntry(it, project, cluster))
                }
            } else {
                proxys.addAll(frontendBundleEntry(it, project, cluster))
            }
        }

        externalRepository.findAll().toList().forEach {
            proxys.addAll(externalLinkEntry(it))
        }

        for (proxy in proxys) {
            if (!pathMatcher.isPattern(proxy.path)) {
                log.error("invalid proxy path {}", proxy.path)
            }
            if (!proxy.url.contains("http") && !proxy.url.contains("https")) {
                log.error("invalid url path {}", proxy)
            }
        }
        log.debug("end getProxy")

        return proxys
    }

    private fun frontendBundleEntry(
        node: ProjectNode,
        project: Project,
        cluster: com.koproxy.cluster.entity.Cluster,
    ): Collection<Proxy> {
        val clusterUrl = getClusterFrontEndUrl(frontendUrl, cluster.prefixName, node.id!!, project.prefix)
        val rewriteDestinationPath = "http://localhost:8091/static/bundle"

        return listOf(
            Proxy("""**$clusterUrl""", rewriteDestinationPath, "FRONTEND", token = node.token),
            Proxy("""**$clusterUrl/""", rewriteDestinationPath, "FRONTEND", token = node.token),
        )
    }

    private fun backendEntry(
        node: ProjectNode,
        project: Project,
        cluster: com.koproxy.cluster.entity.Cluster,
    ): List<Proxy> {
        val nodeBackendUrl = node.backendUrl!!
        val fullUrl = getFullUrl(project.proxyMode, nodeBackendUrl)
        val clusterUrl = getClusterUrl(backendUrl, cluster.prefixName, node.id!!, project.prefix)
        val rewriteDestinationPath = if (nodeBackendUrl.contains("/")) {
            val index = nodeBackendUrl.indexOf("/")
            var path = nodeBackendUrl.substring(index)
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            "$path/api"
        } else {
            "/api"
        }

        return listOf(
            Proxy("""**$clusterUrl/api""", """$fullUrl$rewriteDestinationPath""", "BACKEND", token = node.token),
            Proxy("""**$clusterUrl/api/""", """$fullUrl$rewriteDestinationPath""", "BACKEND", token = node.token),
        )
    }

    private fun backendEntryPrefix(
        node: ProjectNode,
        backOptions: BackOptions,
        project: Project,
        cluster: com.koproxy.cluster.entity.Cluster,
    ): List<Proxy> {
        val nodeBackendUrl = backOptions.url
        val fullUrl = getFullUrl(project.proxyMode, nodeBackendUrl)
        val clusterUrl = getClusterUrl(backendUrl, cluster.prefixName, node.id!!, project.prefix)
        val rewriteDestinationPath = if (nodeBackendUrl.contains("/")) {
            val index = nodeBackendUrl.indexOf("/")
            var path = nodeBackendUrl.substring(index)
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            path
        } else {
            "/"
        }

        return listOf(
            Proxy(
                """**$clusterUrl/${backOptions.prefix}""",
                """$fullUrl$rewriteDestinationPath""",
                "BACKEND",
                token = node.token,
            ),
            Proxy(
                """**$clusterUrl/${backOptions.prefix}/""",
                """$fullUrl$rewriteDestinationPath""",
                "BACKEND",
                token = node.token,
            ),
        )
    }

    private fun backendDefaultEntry(
        node: ProjectNode,
        project: Project,
        cluster: com.koproxy.cluster.entity.Cluster,
    ): List<Proxy> {
        val nodeBackendUrl = node.backendUrl!!
        val fullUrl = getFullUrl(project.proxyMode, nodeBackendUrl)
        val clusterUrl = getClusterBackendEndDefaultUrl(backendUrl, cluster.prefixName, project.prefix)
        val clusterUrl2 = getClusterBackendEndDefault2Url(backendUrl, cluster.prefixName, project.prefix)
        val rewriteDestinationPath = if (nodeBackendUrl.contains("/")) {
            val index = nodeBackendUrl.indexOf("/")
            val path = nodeBackendUrl.substring(index)
            "$path/api"
        } else {
            "/api"
        }

        return listOf(
            Proxy("""**$clusterUrl/api""", """$fullUrl$rewriteDestinationPath""", "BACKEND", token = node.token),
            Proxy("""**$clusterUrl2/api""", """$fullUrl$rewriteDestinationPath""", "BACKEND", token = node.token),
            Proxy("""**$clusterUrl/api/""", """$fullUrl$rewriteDestinationPath""", "BACKEND", token = node.token),
            Proxy("""**$clusterUrl2/api/""", """$fullUrl$rewriteDestinationPath""", "BACKEND", token = node.token),
        )
    }

    private fun backendDefaultEntryPrefix(
        node: ProjectNode,
        backOptions: BackOptions,
        project: Project,
        cluster: com.koproxy.cluster.entity.Cluster,
    ): List<Proxy> {
        val nodeBackendUrl = backOptions.url
        val fullUrl = getFullUrl(project.proxyMode, nodeBackendUrl)
        val clusterUrl = getClusterBackendEndDefaultUrl(backendUrl, cluster.prefixName, project.prefix)
        val clusterUrl2 = getClusterBackendEndDefault2Url(backendUrl, cluster.prefixName, project.prefix)
        val rewriteDestinationPath = if (nodeBackendUrl.contains("/")) {
            val index = nodeBackendUrl.indexOf("/")
            val path = nodeBackendUrl.substring(index)
            path
        } else {
            "/"
        }

        return listOf(
            Proxy(
                """**$clusterUrl/${backOptions.prefix}""",
                """$fullUrl$rewriteDestinationPath""",
                "BACKEND",
                token = node.token,
            ),
            Proxy(
                """**$clusterUrl2/${backOptions.prefix}""",
                """$fullUrl$rewriteDestinationPath""",
                "BACKEND",
                token = node.token,
            ),
            Proxy(
                """**$clusterUrl/${backOptions.prefix}/""",
                """$fullUrl$rewriteDestinationPath""",
                "BACKEND",
                token = node.token,
            ),
            Proxy(
                """**$clusterUrl2/${backOptions.prefix}/""",
                """$fullUrl$rewriteDestinationPath""",
                "BACKEND",
                token = node.token,
            ),
        )
    }

    private fun frontendEntry(
        node: ProjectNode,
        project: Project,
        cluster: com.koproxy.cluster.entity.Cluster,
    ): List<Proxy> {
        val nodeFrontendUrl = node.frontendUrl!!
        val fullUrl = getFullUrl(project.proxyMode, node.frontendUrl!!)
        val clusterUrl = getClusterFrontEndUrl(frontendUrl, cluster.prefixName, node.id!!, project.prefix)
        val rewriteDestinationPath = if (nodeFrontendUrl.contains("/")) {
            val index = nodeFrontendUrl.indexOf("/")
            var path = nodeFrontendUrl.substring(index)
            if (path.endsWith("/")) {
                path = path.dropLast(1)
            }
            path
        } else {
            ""
        }

        return listOf(
            Proxy("""**$clusterUrl""", """$fullUrl$rewriteDestinationPath""", "FRONTEND", token = node.token),
            Proxy("""**$clusterUrl/""", """$fullUrl$rewriteDestinationPath""", "FRONTEND", token = node.token),
        )
    }

    private fun getClusterUrl(baseUrl: String, clusterId: String, nodeId: Long, prefix: String): String {
        return "$baseUrl/$clusterId/$nodeId/$prefix"
    }

    private fun getFullUrl(proxyMode: String, url: String): String {
        val urlWithoutPath = url.split("/")[0]
        return "${proxyMode.toString().lowercase()}://$urlWithoutPath"
    }

    private fun getClusterBackendEndDefault2Url(baseUrl: String, clusterPrefix: String, prefix: String): String {
        return "$baseUrl/$clusterPrefix/$prefix"
    }

    private fun getClusterBackendEndDefaultUrl(baseUrl: String, clusterPrefix: String, prefix: String): String {
        return "$baseUrl/$clusterPrefix/$prefix/**"
    }

    private fun getClusterFrontEndUrl(baseUrl: String, clusterId: String, nodeId: Long, prefix: String): String {
        return "$baseUrl/$clusterId/$prefix-mf/$nodeId"
    }

    private fun getClusterFrontEndDefaultUrl(baseUrl: String, clusterId: String, prefix: String): String {
        return "$baseUrl/$clusterId/$prefix-mf/**"
    }

    private fun getClusterFrontEndDefault2Url(baseUrl: String, clusterId: String, prefix: String): String {
        return "$baseUrl/$clusterId/$prefix-mf"
    }

    private fun frontendDefaultEntry(
        node: ProjectNode,
        project: Project,
        cluster: com.koproxy.cluster.entity.Cluster,
    ): List<Proxy> {
        val nodeFrontendUrl = node.frontendUrl!!
        val fullUrl = getFullUrl(project.proxyMode, node.frontendUrl!!)
        val clusterUrl = getClusterFrontEndDefaultUrl(frontendUrl, cluster.prefixName, project.prefix)
        val clusterUrl2 = getClusterFrontEndDefault2Url(frontendUrl, cluster.prefixName, project.prefix)
        val rewriteDestinationPath = if (nodeFrontendUrl.contains("/")) {
            val index = nodeFrontendUrl.indexOf("/")
            nodeFrontendUrl.substring(index)
        } else {
            ""
        }

        return listOf(
            Proxy("""**$clusterUrl2-mf""", """$fullUrl$rewriteDestinationPath""", "FRONTEND", token = node.token),
            Proxy("""**$clusterUrl-mf""", """$fullUrl$rewriteDestinationPath""", "FRONTEND", token = node.token),
            Proxy("""**$clusterUrl2-mf/""", """$fullUrl$rewriteDestinationPath""", "FRONTEND", token = node.token),
            Proxy("""**$clusterUrl-mf/""", """$fullUrl$rewriteDestinationPath""", "FRONTEND", token = node.token),
        )
    }

    private fun externalLinkEntry(
        externalUrl: ExternalUrl,
    ): List<Proxy> {
        return listOf(
            Proxy("""${getExternalLink(frontendUrl, externalUrl.id)}""", externalUrl.url, "EXTERNAL"),
            Proxy("""${getExternalLink(frontendUrl, externalUrl.id)}/""", externalUrl.url, "EXTERNAL"),
        )
    }

    private fun getExternalLink(baseUrl: String, externalUrlId: Long?): String {
        return "**$baseUrl/externallink/$externalUrlId"
    }
}
