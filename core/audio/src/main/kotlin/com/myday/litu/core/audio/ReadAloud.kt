package com.myday.litu.core.audio

import com.myday.litu.core.model.AnswerOption
import com.myday.litu.core.model.Note
import com.myday.litu.core.model.Question
import kotlinx.coroutines.flow.StateFlow

sealed interface ReadAloudItem {
    val key: String

    /** Stem, then each option in the order shown on screen (options are shuffled at display time). */
    data class QuestionItem(val question: Question, val displayedOptions: List<AnswerOption>) : ReadAloudItem {
        override val key get() = "q:${question.id}"
    }

    data class ExplanationItem(val question: Question) : ReadAloudItem {
        override val key get() = "e:${question.id}"
    }

    data class NoteItem(val note: Note, val title: String) : ReadAloudItem {
        override val key get() = "n:${note.sectionId}"
    }
}

enum class AudioPackStatus { READY, DOWNLOADING, NOT_DOWNLOADED }

/** Read-aloud for every question and note (spec section 11). */
interface ReadAloud {
    /** Key of the item playing now, or null. */
    val playing: StateFlow<String?>
    val packStatus: StateFlow<AudioPackStatus>

    fun play(item: ReadAloudItem, speed: Float)
    fun toggle(item: ReadAloudItem, speed: Float) = if (playing.value == item.key) stop() else play(item, speed)
    fun stop()
}
