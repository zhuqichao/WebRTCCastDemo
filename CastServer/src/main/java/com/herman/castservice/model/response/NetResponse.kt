package com.herman.castservice.model.response

data class NetResponse<T>(
        var result: T?,
        var code: Int,
        var message: String
)