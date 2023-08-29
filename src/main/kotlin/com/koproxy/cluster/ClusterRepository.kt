package com.koproxy.cluster

import com.koproxy.cluster.entity.Cluster
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository

@Repository
class ClusterRepository(private val client: DatabaseClient) {
    fun findAll(): Flow<Cluster> =
        client.sql("SELECT * FROM cluster").map { row ->
            Cluster(
                row.get("id", java.lang.Long::class.java) as Long,
                row.get("prefix_name", java.lang.String::class.java) as String,
            )
        }
            .flow()
}
