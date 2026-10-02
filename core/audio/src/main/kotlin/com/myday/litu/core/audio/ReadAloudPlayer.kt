package com.myday.litu.core.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays pre-generated Opus clips from the audio pack with ExoPlayer. If the pack is not ready or any
 * clip is missing, falls back to Android TextToSpeech with an en-GB voice. Call from the main thread.
 */
@Singleton
class ReadAloudPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReadAloud {
    private val mutablePlaying = MutableStateFlow<String?>(null)
    override val playing: StateFlow<String?> = mutablePlaying.asStateFlow()
    private val mutableStatus = MutableStateFlow(AudioPackStatus.NOT_DOWNLOADED)
    override val packStatus: StateFlow<AudioPackStatus> = mutableStatus.asStateFlow()

    private val pack = AudioPack(context) { mutableStatus.value = it }
    private val main = Handler(Looper.getMainLooper())

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) mutablePlaying.value = null
                }
            })
        }
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: (() -> Unit)? = null

    private data class Segment(val clip: String?, val text: String)

    private fun segments(item: ReadAloudItem): List<Segment> = when (item) {
        is ReadAloudItem.QuestionItem -> buildList {
            add(Segment("q/${item.question.id}.opus", item.question.stem))
            item.displayedOptions.forEachIndexed { i, o ->
                val letter = ('A' + i).toString()
                add(Segment("l/$letter.opus", "Option $letter."))
                add(Segment("o/${o.id}.opus", o.label))
            }
        }
        is ReadAloudItem.ExplanationItem -> listOf(Segment("e/${item.question.id}.opus", item.question.explanation))
        is ReadAloudItem.NoteItem -> listOf(
            Segment(
                "n/${item.note.sectionId}.opus",
                item.title + ". " + item.note.bodyMarkdown.replace(Regex("[#*_>`]"), "") +
                    " Key facts. " + item.note.keyFacts.joinToString(". "),
            ),
        )
    }

    override fun play(item: ReadAloudItem, speed: Float) {
        stop()
        val parts = segments(item)
        val files = parts.map { p -> p.clip?.let(pack::clip) }
        mutablePlaying.value = item.key
        if (files.all { it != null }) {
            player.setMediaItems(files.map { MediaItem.fromUri(android.net.Uri.fromFile(it!!)) })
            player.playbackParameters = PlaybackParameters(speed)
            player.prepare()
            player.play()
        } else {
            pack.requestDownload()
            speak(parts.map { it.text }, speed, item.key)
        }
    }

    override fun stop() {
        if (mutablePlaying.value == null) return
        mutablePlaying.value = null
        player.stop()
        tts?.stop()
        pendingSpeech = null
    }

    private fun speak(texts: List<String>, speed: Float, key: String) {
        val run = {
            tts?.let { engine ->
                engine.setSpeechRate(speed)
                texts.forEachIndexed { i, t ->
                    engine.speak(t, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, if (i == texts.lastIndex) "$key#end" else "$key#$i")
                }
            }
            Unit
        }
        if (ttsReady) run() else {
            pendingSpeech = run
            initTts()
        }
    }

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            main.post {
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@post
                    if (engine.setLanguage(Locale.UK) < TextToSpeech.LANG_AVAILABLE) engine.setLanguage(Locale.ENGLISH)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            if (utteranceId?.endsWith("#end") == true) main.post { mutablePlaying.value = null }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            main.post { mutablePlaying.value = null }
                        }
                    })
                    ttsReady = true
                    pendingSpeech?.invoke()
                    pendingSpeech = null
                } else {
                    Log.w(TAG, "TextToSpeech unavailable ($status)")
                    mutablePlaying.value = null
                }
            }
        }
    }

    private companion object {
        const val TAG = "ReadAloud"
    }
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class AudioModule {
    @dagger.Binds
    abstract fun bindReadAloud(impl: ReadAloudPlayer): ReadAloud
}
