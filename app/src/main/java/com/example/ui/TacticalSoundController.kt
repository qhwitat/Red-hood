package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object TacticalSoundController {
    private var toneGen: ToneGenerator? = null
    var isSoundEnabled: Boolean = true

    init {
        try {
            // Use STREAM_MUSIC so volume respects media volume controls
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (e: Exception) {
            Log.e("TacticalSound", "Could not initialize ToneGenerator", e)
        }
    }

    fun playClick() {
        if (!isSoundEnabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 25) // Clean, high-speed clicking beep
        } catch (e: Exception) {
            Log.e("TacticalSound", "Tone playback failed", e)
        }
    }

    fun playTick() {
        if (!isSoundEnabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_PIP, 15) // Microsecond ticking sound
        } catch (e: Exception) {
            Log.e("TacticalSound", "Tone playback failed", e)
        }
    }

    fun playSuccess() {
        if (!isSoundEnabled) return
        try {
            // Ascending double frequency synth chime
            Thread {
                try {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                    Thread.sleep(100)
                    toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                } catch (ex: Exception) {
                    Log.e("TacticalSound", "Failed to compile success chimes", ex)
                }
            }.start()
        } catch (e: Exception) {
            Log.e("TacticalSound", "Tone playback failed", e)
        }
    }

    fun playError() {
        if (!isSoundEnabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 240) // Warning beep tone
        } catch (e: Exception) {
            Log.e("TacticalSound", "Tone playback failed", e)
        }
    }

    fun playUploadAck() {
        if (!isSoundEnabled) return
        try {
            // Science sweep double pip
            Thread {
                try {
                    toneGen?.startTone(ToneGenerator.TONE_CDMA_PIP, 40)
                    Thread.sleep(60)
                    toneGen?.startTone(ToneGenerator.TONE_CDMA_PIP, 40)
                } catch (ex: Exception) {}
            }.start()
        } catch (e: Exception) {}
    }
}
