package com.herman.castreceiver.utils

import android.annotation.SuppressLint
import android.provider.Settings
import com.herman.castreceiver.main.CastApplication

/**
 * @author Herman.
 * @time 2021/8/17
 */
object DeviceUtils {

  @SuppressLint("HardwareIds")
  fun getAndroidId(): String {
    return Settings.Secure.getString(CastApplication.get().contentResolver, Settings.Secure.ANDROID_ID)
  }

}