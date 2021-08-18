package com.herman.castreceiver.bean

data class DeviceInfo(
  var id: String,  // 设备唯一标志
  var code: String?, //投屏码
  var name: String, // 设备名称
  var type: DeviceType     //设备类型
)

enum class DeviceType {
  TV, PC
}