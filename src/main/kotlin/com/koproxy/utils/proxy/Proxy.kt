package com.koproxy.utils.proxy

data class Proxy(var path: String, var url: String, var type: String, var order: Int = 0, var token: String? = null, var needAuth: Boolean = true)
