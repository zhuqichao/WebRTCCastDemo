package com.herman.castreceiver.main

import android.app.Application

/**
 * @author Herman.
 * @time 2021/8/16
 */
class CastApplication : Application() {

  companion object {
    private lateinit var instance: CastApplication

    fun get(): CastApplication {
      return instance
    }
  }

  override fun onCreate() {
    super.onCreate()
    instance = this
    CastService.start()
  }

}