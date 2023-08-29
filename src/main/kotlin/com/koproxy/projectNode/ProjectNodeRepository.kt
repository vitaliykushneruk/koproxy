package com.koproxy.projectNode

import com.koproxy.projectNode.entity.ProjectNode
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository

@Repository
class ProjectNodeRepository(private val client: DatabaseClient) {

    fun findAll(): Flow<ProjectNode> =
        client.sql("SELECT * FROM project_node").map { row ->
            ProjectNode(
                row.get("id", java.lang.Long::class.java) as Long,
                row.get("project_id", java.lang.Long::class.java) as Long,
                row.get("cluster_id", java.lang.Long::class.java) as Long,
                row.get("backend_url", java.lang.String::class.java) as String?,
                row.get("general", java.lang.Boolean::class.java) as Boolean?,
                row.get("frontend_url", java.lang.String::class.java) as String?,
                row.get("bundle_id", java.lang.Long::class.java) as Long?,
                row.get("token", java.lang.String::class.java) as String?,
                row.get("backend_urls", java.lang.String::class.java) as String?,
            )
        }.flow()

    fun findByClusterId(clusterId: Long): Flow<ProjectNode> =
        client.sql("SELECT * FROM project_node WHERE cluster_id=:cluster_id").bind("cluster_id", clusterId).map { row ->
            ProjectNode(
                row.get("id", java.lang.Long::class.java) as Long,
                row.get("project_id", java.lang.Long::class.java) as Long,
                row.get("cluster_id", java.lang.Long::class.java) as Long,
                row.get("backend_url", java.lang.String::class.java) as String?,
                row.get("general", java.lang.Boolean::class.java) as Boolean?,
                row.get("frontend_url", java.lang.String::class.java) as String?,
                row.get("bundle_id", java.lang.Long::class.java) as Long?,
                row.get("token", java.lang.String::class.java) as String?,
            )
        }.flow()
}
