package com.koproxy.cluster.entity

import org.springframework.data.annotation.Id

data class Cluster(

    @Id
    var id: Long? = null,

    var prefixName: String
)
