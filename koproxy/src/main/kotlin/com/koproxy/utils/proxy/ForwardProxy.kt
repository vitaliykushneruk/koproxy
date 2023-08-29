package com.koproxy.utils.proxy

import java.net.URI

data class ForwardProxy(var forwardProxy: URI, var redirectEnable: Boolean = false, var token: String? = null)
