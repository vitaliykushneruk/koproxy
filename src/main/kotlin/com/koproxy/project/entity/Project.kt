package com.koproxy.project.entity

import org.springframework.data.annotation.Id

data class Project(

    @Id
    var id: Long? = null,

    var prefix: String,

    var infoSystemCode: String,

    var proxyMode: String
)
