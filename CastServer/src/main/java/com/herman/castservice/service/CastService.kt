package com.herman.castservice.service

import com.herman.castservice.bean.DeviceInfo
import com.herman.castservice.model.request.DemoRequest
import com.herman.castservice.model.response.DemoResponse
import com.herman.castservice.model.response.NetResponse

interface CastService {

    fun testAdd(request: DemoRequest): NetResponse<DemoResponse>

    fun initDevice(deviceInfo: DeviceInfo): NetResponse<DeviceInfo>

    fun listDevice(): NetResponse<List<DeviceInfo>>

    fun checkDevice(code: String): NetResponse<DeviceInfo>
}