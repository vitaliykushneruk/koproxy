package com.koproxy.project

import com.koproxy.project.entity.Project
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository

@Repository
class ProjectRepository(private val client: DatabaseClient) {
    fun findAll(): Flow<Project> =
        client.sql("SELECT * FROM project").map { row ->
            Project(
                row.get("id", java.lang.Long::class.java) as Long,
                row.get("prefix", java.lang.String::class.java) as String,
                row.get("info_system_code", java.lang.String::class.java) as String,
                row.get("proxy_mode", java.lang.String::class.java) as String,
            )
        }
            .flow()
}
