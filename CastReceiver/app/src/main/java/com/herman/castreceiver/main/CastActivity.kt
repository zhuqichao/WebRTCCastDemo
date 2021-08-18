package com.herman.castreceiver.main

import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import com.google.gson.JsonObject
import com.herman.castreceiver.R
import com.herman.castreceiver.base.BaseActivity
import com.herman.castreceiver.base.LoadingFragment
import com.herman.castreceiver.bean.*
import com.herman.castreceiver.net.Const
import com.herman.castreceiver.webrtc.PeerConnectionClient
import kotlinx.android.synthetic.main.activity_cast.*
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.RendererCommon.RendererEvents
import java.util.*
import kotlin.math.ceil

/**
 * @author Herman.
 * @time 2021/8/18
 */
class CastActivity : BaseActivity() {

  companion object {
    private const val TAG = "CastActivity"
  }

  private var castOfferId = ""
  private var service: CastService? = null

  private val proxyVideoSink = ProxyVideoSink()
  private val screenSize = IntArray(2)
  private val loadingFragment: LoadingFragment = LoadingFragment.newInstance()
  private var closeCastDialog: Dialog? = null
  private var peerConnectionClient: PeerConnectionClient? = null

  private val mStuns: List<IceServer> = listOf(
    IceServer.builder(Const.URI_STUN1).createIceServer(),
    IceServer.builder(Const.URI_STUN2).createIceServer(),
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_cast)
    setCastInfo()
    initSurfaceView()
    bindService(Intent(this, CastService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

  }

  private fun setCastInfo() {
    if (intent.hasExtra(Const.EXTRA_KEY_CAST_OFFER_ID)) {
      castOfferId = intent.getStringExtra(Const.EXTRA_KEY_CAST_OFFER_ID) ?: ""
      setStateCastLinking()
    } else {
      finish()
    }
  }

  private fun initSurfaceView() {
    getScreenSize()
    surface.holder.setKeepScreenOn(true)
    surface.setEnableHardwareScaler(true)
    val rootEglBase = EglBase.create()
    surface.init(rootEglBase?.eglBaseContext, object : RendererEvents {
      override fun onFirstFrameRendered() {}
      override fun onFrameResolutionChanged(width: Int, height: Int, rotation: Int) {
        runOnUiThread { changeSurfaceSize(width, height) }
      }
    })
    proxyVideoSink.setTarget(surface)
  }

  private fun changeSurfaceSize(width: Int, height: Int) {
    //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
    val max: Float = (width.toFloat() / screenSize[0]).coerceAtLeast(height.toFloat() / screenSize[1])
    surface.layoutParams.apply {
      //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
      this.width = ceil((width.toFloat() / max).toDouble()).toInt()
      this.height = ceil((height.toFloat() / max).toDouble()).toInt()
      //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
      surface.layoutParams = this
    }
  }

  private fun getScreenSize() {
    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getRealMetrics(metrics)
    screenSize[0] = metrics.widthPixels
    screenSize[1] = metrics.heightPixels
  }

  private fun setStateCastLinking() {
    surface.visibility = View.GONE
    setFragment(R.id.fragmentContent, loadingFragment)
    loadingFragment.setLoadingText("投屏正在连接...")
  }

  private fun setStateCasting() {
    surface.visibility = View.VISIBLE
    removeFragment(loadingFragment)
  }

  private fun setCandidate(candidate: Candidate) {
    val iceCandidate = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
    peerConnectionClient?.addRemoteIceCandidate(iceCandidate)
  }

  private fun createPeerConnect(desc: Desc) {
    peerConnectionClient?.close()
    val factory = PeerConnectionClient.initPeerConnectFactory(applicationContext)
    val parameters: PeerConnectionClient.PeerConnectionParameters = PeerConnectionClient.PeerConnectionParameters.default(mStuns, true)
    peerConnectionClient = PeerConnectionClient(parameters, connectionEvent).apply {
      createPeerConnection(factory)
      setRemoteDescription(SessionDescription(SessionDescription.Type.fromCanonicalForm("offer"), desc.sdp))
      createAnswer()
    }
  }

  private fun showCloseCastDialog() {
    val builder = AlertDialog.Builder(this).apply {
      setTitle("提示")
      setMessage("是否结束投屏？")
      setNegativeButton("确定") { dialog, witch -> finish() }
      setPositiveButton("取消", null)
    }
    closeCastDialog = builder.create()
    closeCastDialog?.show()
  }

  override fun onBackPressed() {
    showCloseCastDialog()
  }

  override fun onDestroy() {
    super.onDestroy()
    service?.removeOnCastMessageListener(onCastMessageListener)
    unbindService(serviceConnection)
    sendClose(castOfferId)
  }

  private fun sendReady(id: String) {
    val head = MessageHead(CastService.androidId, id, MessageType.READY)
    service?.sendSocketMessage(MessageSend(head, JsonObject()))
  }

  private fun sendClose(id: String) {
    val head = MessageHead(CastService.androidId, id, MessageType.CLOSE)
    service?.sendSocketMessage(MessageSend(head, CastClose("设备端关闭投屏")))
  }

  private fun sendSdpAnswer(id: String, sessionDescription: SessionDescription) {
    val head = MessageHead(CastService.androidId, id, MessageType.DESC)
    service?.sendSocketMessage(MessageSend(head, Desc("answer", sessionDescription.description)))
  }

  private fun sendLocalIceCandidate(id: String, iceCandidate: IceCandidate) {
    val head = MessageHead(CastService.androidId, id, MessageType.CANDIDATE)
    service?.sendSocketMessage(MessageSend(head, Candidate(iceCandidate.sdpMLineIndex, iceCandidate.sdp, iceCandidate.sdpMid)))
  }

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      Log.d(TAG, "onServiceConnected: ")
      this@CastActivity.service = (service as CastService.LocalBinder).getService()
      this@CastActivity.service?.addOnCastMessageListener(onCastMessageListener)
      sendReady(castOfferId)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      finish()
    }
  }

  private val onCastMessageListener = object : OnCastMessageListener {
    override fun onCastClosed(castClose: CastClose) {
      runOnUiThread {
        showToast("close")
        closeCastDialog?.dismiss()
        finish()
      }
    }

    override fun onCastDesc(desc: Desc) {
      createPeerConnect(desc)
    }

    override fun onCastCandidate(candidate: Candidate) {
      setCandidate(candidate)
    }
  }

  private val connectionEvent = object : PeerConnectionClient.PeerConnectionEvents {
    override fun onLocalDescription(sdp: SessionDescription?) {
      sdp?.let { sendSdpAnswer(castOfferId, it) }
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
      candidate?.let { sendLocalIceCandidate(castOfferId, it) }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>?) {
      candidates?.let { peerConnectionClient?.removeRemoteIceCandidates(it) }
    }

    override fun onIceConnected() {
      runOnUiThread { setStateCasting() }
    }

    override fun onAddTrack(mediaStreams: Array<MediaStream>?) {
      mediaStreams?.takeIf { it.isNotEmpty() && it[0].videoTracks.isNotEmpty() }?.let {
        it[0].videoTracks[0].apply {
          setEnabled(true)
          addSink(surface)
        }
      }
    }

    override fun onIceDisconnected() {
      runOnUiThread { setStateCastLinking() }
    }

    override fun onPeerConnectionClosed() {
    }

    override fun onPeerConnectionStatsReady(reports: RTCStatsReport?) {
    }

    override fun onPeerConnectionError(description: String?) {
    }

  }

  private class ProxyVideoSink : VideoSink {
    private var target: VideoSink? = null

    @Synchronized
    override fun onFrame(frame: VideoFrame) {
      if (target == null) {
        Log.d("TAG", "Dropping frame in proxy because target is null.")
        return
      }
      target!!.onFrame(frame)
    }

    @Synchronized
    fun setTarget(target: VideoSink?) {
      this.target = target
    }
  }

}