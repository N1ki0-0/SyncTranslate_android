package com.example.synctranslate.data.remote.webrtc

import com.example.synctranslate.util.AppLogger
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class SdpObserverImpl(private val tag: String) : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) { AppLogger.i("SdpObserver", "$tag: onCreateSuccess") }
    override fun onSetSuccess() { AppLogger.i("SdpObserver", "$tag: onSetSuccess") }
    override fun onCreateFailure(error: String?) { AppLogger.e("SdpObserver", "$tag: onCreateFailure: $error") }
    override fun onSetFailure(error: String?) { AppLogger.e("SdpObserver", "$tag: onSetFailure: $error") }
}