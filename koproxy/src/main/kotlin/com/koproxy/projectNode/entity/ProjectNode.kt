package com.koproxy.projectNode.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.data.annotation.Id

data class ProjectNode(

    @Id
    var id: Long? = null,

    var projectId: Long? = null,

    var clusterId: Long? = null,

    var backendUrl: String?,

    var general: Boolean?,

    var frontendUrl: String?,

    var bundleId: Long? = null,

    var token: String? = null,

    var backendUrls: List<BackOptions>? = null,
) {
    constructor(
        id: Long?,
        projectId: Long?,
        clusterId: Long?,
        backendUrl: String?,
        general: Boolean?,
        frontendUrl: String?,
        bundleId: Long?,
        token: String?,
        backendUrls: String?,
    ) : this(
        id,
        projectId,
        clusterId,
        backendUrl,
        general,
        frontendUrl,
        bundleId,
        token,
        stringBackendUrlsToJson(backendUrls),
    )
}

@Serializable
data class BackOptions(

    val url: String,

    val prefix: String,
)

fun stringBackendUrlsToJson(backendUrls: String?): List<BackOptions>? {
    return backendUrls?.let { Json.decodeFromString<List<BackOptions>>(backendUrls) }
}
