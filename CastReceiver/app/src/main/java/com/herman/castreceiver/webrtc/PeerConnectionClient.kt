package com.herman.castreceiver.webrtc

import android.content.Context
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.PeerConnectionFactory.InitializationOptions
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

class PeerConnectionClient(private val peerConnectionParameters: PeerConnectionParameters, private val events: PeerConnectionEvents) {

  companion object {

    private const val TAG = "PeerConnectionClient"
    private const val OFFER_TO_RECEIVE_AUDIO = "OfferToReceiveAudio"
    private const val OFFER_TO_RECEIVE_VIDEO = "OfferToReceiveVideo"

    fun initPeerConnectFactory(context: Context): PeerConnectionFactory {
      val mDecoderFactory: VideoDecoderFactory = SoftwareVideoDecoderFactory()
      val mEncoderFactory: VideoEncoderFactory = SoftwareVideoEncoderFactory()
      val options = InitializationOptions.builder(context).createInitializationOptions()
      PeerConnectionFactory.initialize(options)
      return PeerConnectionFactory.builder()
        .setVideoDecoderFactory(mDecoderFactory)
        .setVideoEncoderFactory(mEncoderFactory)
        .createPeerConnectionFactory()
    }
  }

  private var mPeerConnection: PeerConnection? = null
  private var mFactory: PeerConnectionFactory? = null
  private var localVideoTrack: VideoTrack? = null
  private var remoteVideoTrack: VideoTrack? = null
  private var localAudioTrack: AudioTrack? = null
  private var remoteAudioTrack: AudioTrack? = null
  private val pcObserver = PCObserver()
  private val sdpObserver = SDPObserver()
  private var sdpMediaConstraints: MediaConstraints? = null
  private var isInitiator = false
  private var localSdp: SessionDescription? = null
  private val statsTimer = Timer()
  private var isError = false
  private val dataChannelEnabled: Boolean = peerConnectionParameters.dataChannelParameters != null
  private var dataChannel: DataChannel? = null

  fun createPeerConnection(factory: PeerConnectionFactory) {
    mFactory = factory
    try {
      createMediaConstraintsInternal()
      createPeerConnectionInternal()
    } catch (e: Exception) {
      reportError("Failed to create peer connection: " + e.message)
      throw e
    }
  }

  fun createAndSetLocalMediaStream(audioSource: AudioSource?, videoSource: VideoSource?, localSink: VideoSink?) {
    if (videoSource != null && audioSource != null) {
      localVideoTrack = mFactory?.createVideoTrack("video_track", videoSource)
      localAudioTrack = mFactory?.createAudioTrack("audio_track", audioSource)
      val mediaStream = mFactory?.createLocalMediaStream("media_stream")
      mediaStream?.addTrack(localVideoTrack)
      mediaStream?.addTrack(localAudioTrack)
      mPeerConnection?.addStream(mediaStream)
      if (localSink != null) {
        localVideoTrack?.addSink(localSink)
      }
    }
  }

  private fun createPeerConnectionInternal() {
    if (mFactory == null) {
      Log.e(TAG, "Peerconnection factory is not created")
      return
    }
    isError = false
    Log.d(TAG, "Create peer connection.")
    val rtcConfig = RTCConfiguration(peerConnectionParameters.iceServers)
    // TCP candidates are only useful when connecting to a server that supports
    // ICE-TCP.
    rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
    rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
    rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
    rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
    // Use ECDSA encryption.
    rtcConfig.keyType = PeerConnection.KeyType.ECDSA
    // Enable DTLS for normal calls and disable for loopback calls.
    rtcConfig.enableDtlsSrtp = true
    rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B
    mPeerConnection = mFactory?.createPeerConnection(rtcConfig, pcObserver)
    if (dataChannelEnabled && mPeerConnection != null) {
      val init = DataChannel.Init()
      init.ordered = peerConnectionParameters.dataChannelParameters!!.ordered
      init.negotiated = peerConnectionParameters.dataChannelParameters.negotiated
      init.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits
      init.maxRetransmitTimeMs = peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs
      init.id = peerConnectionParameters.dataChannelParameters.id
      init.protocol = peerConnectionParameters.dataChannelParameters.protocol
      dataChannel = mPeerConnection?.createDataChannel("ApprtcDemo data", init)
    }
    isInitiator = false

    // Set INFO libjingle logging.
    // NOTE: this _must_ happen while |factory| is alive!
    Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO)
    if (peerConnectionParameters.aecDump) {
      try {
        val aecDumpFileDescriptor = ParcelFileDescriptor.open(
          File(Environment.getExternalStorageDirectory().path + File.separator + "Download/audio.aecdump"),
          ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                  or ParcelFileDescriptor.MODE_TRUNCATE
        )
        mFactory?.startAecDump(aecDumpFileDescriptor.detachFd(), -1)
      } catch (e: IOException) {
        Log.e(TAG, "Can not open aecdump file", e)
      }
    }
    Log.d(TAG, "Peer connection created.")
  }

  private fun closeInternal() {
    Log.d(TAG, "Closing peer connection.")
    statsTimer.cancel()
    dataChannel?.dispose()
    dataChannel = null
    mPeerConnection?.dispose()
    mPeerConnection = null
    Log.d(TAG, "Closing peer connection done.")
    events.onPeerConnectionClosed()
  }

  private fun createMediaConstraintsInternal() {
    // Create SDP constraints.
    sdpMediaConstraints = MediaConstraints()
    sdpMediaConstraints?.mandatory?.add(
      MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO, peerConnectionParameters.offerToReceive.toString())
    )
    sdpMediaConstraints?.mandatory?.add(
      MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO, peerConnectionParameters.offerToReceive.toString())
    )
  }

  fun close() {
    closeInternal()
  }

  private val stats: Unit
    get() {
      if (mPeerConnection != null && !isError) {
        mPeerConnection?.getStats { reports: RTCStatsReport? -> events.onPeerConnectionStatsReady(reports) }
      }
    }

  fun enableStatsEvents(enable: Boolean, periodMs: Int) {
    if (enable) {
      try {
        statsTimer.schedule(object : TimerTask() {
          override fun run() {
            stats
          }
        }, 0, periodMs.toLong())
      } catch (e: Exception) {
        Log.e(TAG, "Can not schedule statistics timer", e)
      }
    } else {
      statsTimer.cancel()
    }
  }

  fun setLocalAudioEnabled(enable: Boolean) {
    localAudioTrack?.setEnabled(enable)
  }

  fun setRemoteAudioEnabled(enable: Boolean) {
    remoteAudioTrack?.setEnabled(enable)
  }

  fun setLocalVideoEnabled(enable: Boolean) {
    localVideoTrack?.setEnabled(enable)
  }

  fun setRemoteVideoEnabled(enable: Boolean) {
    remoteVideoTrack?.setEnabled(enable)
  }

  fun createOffer() {
    if (mPeerConnection != null && !isError) {
      Log.d(TAG, "PC Create OFFER")
      isInitiator = true
      mPeerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
    }
  }

  fun createAnswer() {
    if (mPeerConnection != null && !isError) {
      Log.d(TAG, "PC create ANSWER")
      isInitiator = false
      mPeerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
    }
  }

  fun addRemoteIceCandidate(candidate: IceCandidate?) {
    if (mPeerConnection != null && !isError) {
      mPeerConnection?.addIceCandidate(candidate)
    }
  }

  fun removeRemoteIceCandidates(candidates: Array<IceCandidate?>?) {
    if (mPeerConnection != null && !isError) {
      mPeerConnection?.removeIceCandidates(candidates)
    }
  }

  fun setRemoteDescription(sdp: SessionDescription) {
    if (mPeerConnection != null && !isError) {
      val sdpDescription = sdp.description
      Log.d(TAG, "Set remote SDP.")
      val sdpRemote = SessionDescription(sdp.type, sdpDescription)
      mPeerConnection?.setRemoteDescription(sdpObserver, sdpRemote)
    }
  }

  class PeerConnectionParameters(
    val offerToReceive: Boolean, val aecDump: Boolean,
    val iceServers: List<IceServer?>?,
    val dataChannelParameters: DataChannelParameters?
  ) {
    companion object {
      fun default(iceServers: List<IceServer?>?, offerToReceive: Boolean): PeerConnectionParameters {
        return PeerConnectionParameters(
          offerToReceive, false,
          iceServers, DataChannelParameters.default()
        )
      }
    }
  }

  class DataChannelParameters(
    val ordered: Boolean, val maxRetransmitTimeMs: Int, val maxRetransmits: Int,
    val protocol: String, val negotiated: Boolean, val id: Int
  ) {
    companion object {
      fun default(): DataChannelParameters {
        return DataChannelParameters(true, -1, -1, "false", false, -1)
      }
    }
  }

  interface PeerConnectionEvents {
    /**
     * Callback fired once local SDP is created and set.
     */
    fun onLocalDescription(sdp: SessionDescription?)

    /**
     * Callback fired once local Ice candidate is generated.
     */
    fun onIceCandidate(candidate: IceCandidate?)

    /**
     * Callback fired once local ICE candidates are removed.
     */
    fun onIceCandidatesRemoved(candidates: Array<IceCandidate>?)

    /**
     * Callback fired once connection is established (IceConnectionState is
     * CONNECTED).
     */
    fun onIceConnected()

    /**
     * Callback fired once connection is established (On Track added).
     */
    fun onAddTrack(mediaStreams: Array<MediaStream>?)

    /**
     * Callback fired once connection is disconnected (IceConnectionState is
     * DISCONNECTED).
     */
    fun onIceDisconnected()

    /**
     * Callback fired once peer connection is closed.
     */
    fun onPeerConnectionClosed()

    /**
     * Callback fired once peer connection statistics is ready.
     */
    fun onPeerConnectionStatsReady(reports: RTCStatsReport?)

    /**
     * Callback fired once peer connection error happened.
     */
    fun onPeerConnectionError(description: String?)
  }

  private fun reportError(errorMessage: String) {
    Log.e(TAG, "Peerconnection error: $errorMessage")
    if (!isError) {
      events.onPeerConnectionError(errorMessage)
      isError = true
    }
  }

  // Implementation detail: observe ICE & stream changes and react accordingly.
  private inner class PCObserver : PeerConnection.Observer {
    override fun onIceCandidate(candidate: IceCandidate) {
      events.onIceCandidate(candidate)
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
      events.onIceCandidatesRemoved(candidates)
    }

    override fun onSignalingChange(newState: SignalingState) {
      Log.d(TAG, "SignalingState: $newState")
    }

    override fun onIceConnectionChange(newState: IceConnectionState) {
      Log.d(TAG, "IceConnectionState: $newState")
      when (newState) {
        IceConnectionState.CONNECTED -> {
          events.onIceConnected()
        }
        IceConnectionState.DISCONNECTED -> {
          events.onIceDisconnected()
        }
        IceConnectionState.FAILED -> {
          reportError("ICE connection failed.")
        }
        else -> {
        }
      }
    }

    override fun onIceGatheringChange(newState: IceGatheringState) {
      Log.d(TAG, "IceGatheringState: $newState")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
      Log.d(TAG, "IceConnectionReceiving changed to $receiving")
    }

    override fun onAddStream(stream: MediaStream) {}
    override fun onRemoveStream(stream: MediaStream) {}

    override fun onDataChannel(dc: DataChannel) {
      Log.d(TAG, "New Data channel " + dc.label())
      if (!dataChannelEnabled) {
        return
      }
      dc.registerObserver(object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) {
          Log.d(TAG, "Data channel buffered amount changed: " + dc.label() + ": " + dc.state())
        }

        override fun onStateChange() {
          Log.d(TAG, "Data channel state changed: " + dc.label() + ": " + dc.state())
        }

        override fun onMessage(buffer: DataChannel.Buffer) {
          if (buffer.binary) {
            Log.d(TAG, "Received binary msg over $dc")
            return
          }
          val data = buffer.data
          val bytes = ByteArray(data.capacity())
          data[bytes]
          val strData = String(bytes, StandardCharsets.UTF_8)
          Log.d(TAG, "Got msg: $strData over $dc")
        }
      })
    }

    override fun onRenegotiationNeeded() {
      // No need to do anything; AppRTC follows a pre-agreed-upon
      // signaling/negotiation protocol.
    }

    override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
      if (mediaStreams[0].audioTracks.size == 1) {
        Log.d(TAG, "onAddAudioTrack: " + mediaStreams[0].audioTracks[0])
        remoteAudioTrack = mediaStreams[0].audioTracks[0]
      }
      if (mediaStreams[0].videoTracks.size == 1) {
        Log.d(TAG, "onAddVideoTrack: " + mediaStreams[0].videoTracks[0])
        remoteVideoTrack = mediaStreams[0].videoTracks[0]
      }
      events.onAddTrack(mediaStreams)
    }
  }

  // Implementation detail: handle offer creation/signaling and answer setting,
  // as well as adding remote ICE candidates once the answer SDP is set.
  private inner class SDPObserver : SdpObserver {
    override fun onCreateSuccess(origSdp: SessionDescription) {
      if (localSdp != null) {
        reportError("Multiple SDP create.")
        return
      }
      val sdpDescription = origSdp.description
      val sdp = SessionDescription(origSdp.type, sdpDescription)
      localSdp = sdp
      if (mPeerConnection != null && !isError) {
        Log.d(TAG, "Set local SDP from " + sdp.type)
        mPeerConnection!!.setLocalDescription(sdpObserver, sdp)
      }
    }

    override fun onSetSuccess() {
      if (mPeerConnection == null || isError) {
        return
      }
      if (isInitiator) {
        // For offering peer connection we first create offer and set
        // local SDP, then after receiving answer set remote SDP.
        if (mPeerConnection!!.remoteDescription == null) {
          // We've just set our local SDP so time to send it.
          Log.d(TAG, "Local SDP set succesfully")
          events.onLocalDescription(localSdp)
        } else {
          // We've just set remote description, so drain remote
          // and send local ICE candidates.
          Log.d(TAG, "Remote SDP set succesfully")
        }
      } else {
        // For answering peer connection we set remote SDP and then
        // create answer and set local SDP.
        if (mPeerConnection!!.localDescription != null) {
          // We've just set our local SDP so time to send it, drain
          // remote and send local ICE candidates.
          Log.d(TAG, "Local SDP set succesfully")
          events.onLocalDescription(localSdp)
        } else {
          // We've just set remote SDP - do nothing for now -
          // answer will be created soon.
          Log.d(TAG, "Remote SDP set succesfully")
        }
      }
    }

    override fun onCreateFailure(error: String) {
      reportError("createSDP error: $error")
    }

    override fun onSetFailure(error: String) {
      reportError("setSDP error: $error")
    }
  }


}