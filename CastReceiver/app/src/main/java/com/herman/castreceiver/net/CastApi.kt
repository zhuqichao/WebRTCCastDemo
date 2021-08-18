package com.herman.castreceiver.net

import com.herman.castreceiver.bean.DeviceInfo
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * @author Herman.
 * @time 2021/8/17
 */
interface CastApi {
  @POST("initDevice")
  fun initDevice(@Body device: DeviceInfo): Call<NetResponse<DeviceInfo>>
}