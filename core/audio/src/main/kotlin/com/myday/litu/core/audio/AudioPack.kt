package com.myday.litu.core.audio

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus as PlayStatus
import java.io.File

/** Locates the fast-follow Play Asset Delivery pack "audio_pack" (spec section 11). */
internal class AudioPack(context: Context, private val onStatus: (AudioPackStatus) -> Unit) {
    private val manager = runCatching { AssetPackManagerFactory.getInstance(context) }.getOrNull()

    private val listener = AssetPackStateUpdateListener { state ->
        if (state.name() != PACK) return@AssetPackStateUpdateListener
        onStatus(
            when (state.status()) {
                PlayStatus.COMPLETED -> AudioPackStatus.READY
                PlayStatus.DOWNLOADING, PlayStatus.TRANSFERRING, PlayStatus.PENDING -> AudioPackStatus.DOWNLOADING
                else -> AudioPackStatus.NOT_DOWNLOADED
            },
        )
    }

    init {
        manager?.registerListener(listener)
        onStatus(if (root() != null) AudioPackStatus.READY else AudioPackStatus.NOT_DOWNLOADED)
    }

    /** Asks Play to download the pack if the fast-follow download has not happened yet. */
    fun requestDownload() {
        if (root() == null) runCatching { manager?.fetch(listOf(PACK)) }
    }

    fun root(): File? = runCatching {
        manager?.getPackLocation(PACK)?.assetsPath()?.let { File(it, "audio") }?.takeIf { it.isDirectory }
    }.getOrNull()

    fun clip(key: String): File? = root()?.let { File(it, key) }?.takeIf { it.isFile }

    private companion object {
        const val PACK = "audio_pack"
    }
}
