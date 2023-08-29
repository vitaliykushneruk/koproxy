package com.koproxy.user.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserCrash(

    @Id
    val id: Long? = null,

    var login: String,

    @Column("password")
    var password1: String,

    var userName: String,
) : UserDetails {
    override fun getAuthorities(): MutableCollection<GrantedAuthority> {
        val simpleGrantedAuthority = SimpleGrantedAuthority("ADMIN")
        return mutableListOf(simpleGrantedAuthority)
    }

    override fun getPassword(): String {
        return password1
    }

    override fun getUsername(): String {
        return userName
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}
