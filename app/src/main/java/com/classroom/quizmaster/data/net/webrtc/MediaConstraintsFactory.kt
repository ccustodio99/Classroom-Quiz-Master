package com.classroom.quizmaster.data.net.webrtc

import org.webrtc.MediaConstraints

internal object MediaConstraintsFactory {
    fun createDefault(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }
}
