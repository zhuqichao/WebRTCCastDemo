package com.herman.castreceiver.net

data class NetResponse<T>(
  var result: T?,
  var code: Int,
  var message: String
)