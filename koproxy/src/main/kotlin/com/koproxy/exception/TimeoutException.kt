package com.koproxy.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT, reason = "Response took longer than configured timeout")
class TimeoutException : Exception {
    constructor()
    constructor(message: String?) : super(message)

    /**
     * Disables fillInStackTrace for performance reasons.
     * @return this
     */
    @Synchronized
    override fun fillInStackTrace(): Throwable {
        return this
    }
}
