package com.koproxy.utils.jwt

import com.koproxy.config.HttpClientProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultHeader
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import java.net.InetAddress
import java.security.KeyStore
import java.util.Date
import java.util.UUID
import kotlin.collections.HashMap

@Component
class JwtTokenUtil(val properties: HttpClientProperties) {

    val JWT_TOKEN_VALIDITY = (5 * 60 * 60).toLong()

    fun generateToken(userDetails: UserDetails): String? {
        val claims: MutableMap<String, Any> = HashMap()
        claims.put("method", "login")
        claims.put("ip", InetAddress.getLocalHost().hostAddress)
        claims.put("iss", "https://passport.api.com.koproxy/passport")
        claims.put("channel", "internal")
        claims.put("nonce", UUID.randomUUID())
        claims.put("auth:service", "default")
        claims.put("auth:module", "DataStore")
        claims.put("realm", "/staff")
        return doGenerateToken(claims, userDetails.username)
    }

    private fun doGenerateToken(claims: Map<String, Any>, subject: String): String? {
        val header = DefaultHeader()
        header["typ"] = "JWT"
        header["kid"] = "https://passport.api.com.koproxy/passport"

        val appKeyStore: KeyStore = KeyStore.getInstance("PKCS12")
        val passwordToCharArray = properties.ssl.mtlsKeyPassword!!.toCharArray()
        appKeyStore.load(
            ResourceUtils.getFile(properties.ssl.mtlsKey!!).inputStream(),
            passwordToCharArray,
        )

        val key = appKeyStore.getKey("1", passwordToCharArray)
        key.algorithm
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
            .setHeader(header)
            .setId(UUID.randomUUID().toString())
            .setAudience("https://passport.api.com.koproxy/passport")
            .signWith(key, SignatureAlgorithm.RS256)
            .compact()
    }
}
