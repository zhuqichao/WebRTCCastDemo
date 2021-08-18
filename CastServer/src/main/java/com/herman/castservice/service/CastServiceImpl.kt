package com.herman.castservice.service

import com.herman.castservice.bean.DeviceInfo
import com.herman.castservice.bean.DeviceType
import com.herman.castservice.model.request.DemoRequest
import com.herman.castservice.model.response.DemoResponse
import com.herman.castservice.model.response.NetResponse
import com.herman.castservice.model.response.SUCCESS
import com.herman.castservice.util.ConversionUtils

class CastServiceImpl : CastService {

    private val devices = hashMapOf<String, DeviceInfo>()

    override fun testAdd(request: DemoRequest): NetResponse<DemoResponse> {
        println(request.toString() + ": num1=" + request.num1 + ", num2=" + request.num2)
        return NetResponse(DemoResponse(request.num1 + request.num2), SUCCESS, "success")
    }

    override fun initDevice(deviceInfo: DeviceInfo): NetResponse<DeviceInfo> {
        println("设备信息上报：$deviceInfo")
        if (devices[deviceInfo.id] == null) {
            deviceInfo.code = ConversionUtils.md5CodeEncode(deviceInfo.id, 4)
            devices[deviceInfo.id] = deviceInfo
        }
        return NetResponse(devices[deviceInfo.id], SUCCESS, "success")
    }

    override fun listDevice(): NetResponse<List<DeviceInfo>> {
        return NetResponse(devices.values.toList().filter { it.type == DeviceType.TV }, SUCCESS, "success")
    }

    override fun checkDevice(code: String): NetResponse<DeviceInfo> {
        val list = devices.values.toList().filter { it.code == code }
        return NetResponse(list.takeIf { it.isNotEmpty() }?.get(0), SUCCESS, "success")
    }
}