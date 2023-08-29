package com.koproxy.user

import com.koproxy.user.entity.UserCrash
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
class UserCrashRepository(private val client: DatabaseClient) {
    fun findAll(): Flow<UserCrash> =
        client.sql("SELECT * FROM user_crash").map { row ->
            UserCrash(
                row.get("id", java.lang.Long::class.java) as Long,
                row.get("login", java.lang.String::class.java) as String,
                row.get("password", java.lang.String::class.java) as String,
                row.get("userName", java.lang.String::class.java) as String,
            )
        }
            .flow()

    fun findByUserName(login: String): Mono<UserCrash> =
        client.sql("SELECT * FROM user_crash WHERE login=:login").bind("login", login).map { row ->
            UserCrash(
                row.get("id", java.lang.Long::class.java) as Long,
                row.get("login", java.lang.String::class.java) as String,
                row.get("password", java.lang.String::class.java) as String,
                row.get("user_name", java.lang.String::class.java) as String,
            )
        }.first()
}
