package com.koproxy.external

import com.koproxy.external.entity.ExternalUrl
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository

@Repository
class ExternalRepository(private val client: DatabaseClient) {
    fun findAll(): Flow<ExternalUrl> =
        client.sql("SELECT * FROM external_url").map { row ->
            ExternalUrl(
                row.get("id", java.lang.Long::class.java) as Long,
                row.get("name", java.lang.String::class.java) as String,
                row.get("url", java.lang.String::class.java) as String,
            )
        }
            .flow()
}
