package com.herman.castreceiver.net

import com.herman.castreceiver.bean.DeviceInfo
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author Herman.
 * @time 2021/8/17
 */
object NetWork {

  private var castApi: CastApi = getRetrofit(Const.BASE_URL_CAST).create(CastApi::class.java)

  fun initDevice(deviceInfo: DeviceInfo, netResult: NetResult<DeviceInfo>) {
    castApi.initDevice(deviceInfo).enqueue(object : Callback<NetResponse<DeviceInfo>> {
      override fun onResponse(call: Call<NetResponse<DeviceInfo>>, response: Response<NetResponse<DeviceInfo>>) {
        if (isOkResponse(response) && response.body()!!.result != null) {
          netResult.onResult(response.body()!!.result, SUCCESS, "")
        } else {
          netResult.onResult(null, response.body()?.code ?: UNKNOW, response.body()?.message ?: "")
        }
      }

      override fun onFailure(call: Call<NetResponse<DeviceInfo>>, t: Throwable) {
        netResult.onResult(null, UNKNOW, t.message ?: "")
      }

    })
  }

  private fun <T> isOkResponse(response: Response<NetResponse<T>>): Boolean {
    return response.isSuccessful && response.body() != null && response.body()!!.code == SUCCESS
  }

  private fun getRetrofit(baseUrl: String): Retrofit {
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .addConverterFactory(GsonConverterFactory.create())
      .client(getOkHttpClient())
      .build()
  }

  private fun getOkHttpClient(): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BODY
    }
    return OkHttpClient.Builder()
      .addInterceptor(logging)
      .retryOnConnectionFailure(true)
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()
  }

}