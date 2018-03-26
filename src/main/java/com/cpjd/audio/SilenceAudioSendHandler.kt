package com.cpjd.audio

import net.dv8tion.jda.core.audio.AudioSendHandler
import java.util.*

class SilenceAudioSendHandler: AudioSendHandler {
    var canProvide = true
    var startTime = Date().time

    override fun provide20MsAudio(): ByteArray {

        // send the silence only for 5 seconds
        if (((Date().time - startTime) / 1000) > 5) {
            canProvide = false
        }

        val silence = arrayOf(0xF8.toByte(), 0xFF.toByte(), 0xFE.toByte())
        return silence.toByteArray()
    }

    override fun canProvide(): Boolean {
        return canProvide
    }

    override fun isOpus(): Boolean {
        return true
    }
}