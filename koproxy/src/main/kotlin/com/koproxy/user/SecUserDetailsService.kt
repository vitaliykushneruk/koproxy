package com.koproxy.user

import com.koproxy.user.entity.UserCrash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SecUserDetailsService() : ReactiveUserDetailsService {

    @Autowired
    private lateinit var userCrashRepository: UserCrashRepository

    override fun findByUsername(login: String?): Mono<UserDetails> {
        val userDetailsMono: Mono<UserCrash> = userCrashRepository.findByUserName(login!!)

        return userDetailsMono.cast(UserDetails::class.java)
    }
}
