package com.elyedali.englishsteps.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import com.elyedali.englishsteps.data.Progress
import java.util.Locale

/**
 * النطق بالإنجليزية بمحرك النطق الموجود في الهاتف (يعمل دون إنترنت إذا كان الصوت الإنجليزي مثبتًا)،
 * وأصوات قصيرة للإجابة الصحيحة والخاطئة.
 */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var tones: ToneGenerator? = null

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.US)
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ready) {
                    val r2 = tts?.setLanguage(Locale.UK)
                    ready = r2 != TextToSpeech.LANG_MISSING_DATA && r2 != TextToSpeech.LANG_NOT_SUPPORTED
                }
                tts?.setSpeechRate(0.85f)
            }
        }
        tones = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull()
    }

    val isReady: Boolean get() = ready

    fun speak(text: String, slow: Boolean = false) {
        val t = tts ?: return
        if (!ready) return
        t.setSpeechRate(if (slow) 0.55f else 0.85f)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u" + text.hashCode())
    }

    /** ينطق عدة جمل متتالية (مثل الحوار كاملًا) */
    fun speakAll(lines: List<String>) {
        val t = tts ?: return
        if (!ready) return
        t.setSpeechRate(0.85f)
        lines.forEachIndexed { i, s ->
            t.speak(s, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "l$i")
            t.playSilentUtterance(450, TextToSpeech.QUEUE_ADD, "s$i")
        }
    }

    fun stop() { tts?.stop() }

    fun correct() {
        if (!Progress.soundOn) return
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_ACK, 150) }
    }

    fun wrong() {
        if (!Progress.soundOn) return
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_NACK, 220) }
    }

    fun success() {
        if (!Progress.soundOn) return
        runCatching { tones?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300) }
    }
}
