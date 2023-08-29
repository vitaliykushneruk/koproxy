package com.koproxy.controller

import com.koproxy.utils.ProxyUtil
import com.koproxy.utils.proxy.Proxy
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/koproxy")
class ProxyController(private val proxyUtil: ProxyUtil) {

    @GetMapping
    fun getProxys(): MutableSet<Proxy> {
        return proxyUtil.getProxysList()
    }
}
