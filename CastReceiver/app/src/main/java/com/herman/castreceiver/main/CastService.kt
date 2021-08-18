package com.herman.castreceiver.main

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.herman.castreceiver.bean.DeviceInfo
import com.herman.castreceiver.bean.DeviceType
import com.herman.castreceiver.bean.SocketState
import com.herman.castreceiver.net.Const
import com.herman.castreceiver.net.NetResult
import com.herman.castreceiver.net.NetWork
import com.herman.castreceiver.utils.DeviceUtils
import io.crossbar.autobahn.websocket.WebSocketConnection
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler
import java.util.*

/**
 * @author Herman.
 * @time 2021/8/16
 */
class CastService : Service() {

  companion object {
    private const val TAG = "CastService"
    val androidId = DeviceUtils.getAndroidId()

    fun start() {
      CastApplication.get().startService(Intent(CastApplication.get().baseContext, CastService::class.java))
    }
  }

  var socketState = SocketState.CLOSED
  var pingCode = ""
  private var webSocketConnection = WebSocketConnection()

  private val onSocketStateListeners = mutableListOf<OnSocketStateListener>()

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "onCreate: ANDROID_ID=${androidId}")
    NetWork.initDevice(DeviceInfo(androidId, "", Build.PRODUCT, DeviceType.TV), object : NetResult<DeviceInfo> {
      override fun onResult(result: DeviceInfo?, code: Int, message: String) {
        pingCode = result?.code ?: ""
        createWebSocket(pingCode)
      }
    })
  }

  private fun createWebSocket(pingCode: String) {
    if (webSocketConnection.isConnected) {
      webSocketConnection.sendClose()
    }
    val uri = "${Const.BASE_URL_WEBSOCKET}$androidId/$pingCode/${DeviceType.TV}"
    Log.d(TAG, "createWebSocket: uri=$uri")
    webSocketConnection.connect(uri, webSocketHandler)
    socketState = SocketState.CONNECTING
    callSocketStateChanged(socketState)
  }

  fun sendSocketMessage(message: String) {
    if (webSocketConnection.isConnected) {
      webSocketConnection.sendMessage(message)
    }
  }

  fun addOnSocketStateListener(listener: OnSocketStateListener) {
    onSocketStateListeners.add(listener)
  }

  fun removeOnSocketStateListener(listener: OnSocketStateListener) {
    onSocketStateListeners.remove(listener)
  }

  private fun callSocketStateChanged(state: SocketState) {
    onSocketStateListeners.forEach {
      it.onState(state)
    }
  }

  private val webSocketHandler = object : WebSocketConnectionHandler() {
    override fun onOpen() {
      Log.d(TAG, "onOpen: WebSocket已连接")
      socketState = SocketState.CONNECTED
      callSocketStateChanged(socketState)
    }

    override fun onMessage(payload: String?) {
      Log.d(TAG, "onMessage: WebSocket消息：$payload")
    }

    override fun onClose(code: Int, reason: String?) {
      Log.d(TAG, "onClose: WebSocket断开连接：code=$code, reason=$reason")
      socketState = SocketState.CLOSED
      callSocketStateChanged(socketState)
    }
  }

  override fun onBind(intent: Intent?): IBinder {
    return LocalBinder()
  }

  inner class LocalBinder : Binder() {
    fun getService(): CastService {
      return this@CastService
    }
  }

}