package com.herman.castreceiver.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.herman.castreceiver.R
import com.herman.castreceiver.base.BaseActivity
import com.herman.castreceiver.bean.SocketState
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

  companion object {
    private const val TAG = "MainActivity"
  }

  private var service: CastService? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    bindService(Intent(this, CastService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService(serviceConnection)
  }

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      Log.d(TAG, "onServiceConnected: ")
      this@MainActivity.service = (service as CastService.LocalBinder).getService()
      pingCode.text = this@MainActivity.service?.pingCode
      this@MainActivity.service?.addOnSocketStateListener(onSocketStateListener)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      this@MainActivity.service?.removeOnSocketStateListener(onSocketStateListener)
    }
  }

  private val onSocketStateListener = object : OnSocketStateListener {
    override fun onState(state: SocketState) {
      when (state) {
        SocketState.CONNECTED -> {
          pingCode.text = service?.pingCode
          socketState.text = "服务器已连接"
        }
        SocketState.CONNECTING -> {
          pingCode.text = service?.pingCode
          socketState.text = "正在连接服务器"
        }
        SocketState.CLOSED -> {
          pingCode.text = service?.pingCode
          socketState.text = "已断开服务器"
        }
        SocketState.CONNECT_FAIL -> {
          pingCode.text = service?.pingCode
          socketState.text = "连接服务器失败"
        }
      }
    }
  }

}