@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.ui.screens.player

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.nuvio.tv.data.local.PlayerControlAction
import com.nuvio.tv.ui.theme.InkTheme
import com.nuvio.tv.ui.v2.appearance.sampleArtworkAccent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The player's controls in the Ink theme (this fork only). Arc's PlayerCustomControls keeps its
 * layout and all of its focus handling; these only change what is drawn when InkTheme is active:
 * a black band feathering up from the bottom, the title in Archivo (caps), the times in Silkscreen
 * over the line, buttons as words (focused in full ink, the rest at 35%), and the progress line in
 * the backdrop's accent (drawn by Arc's ProgressBar, which reads InkTheme.progressAccent).
 */
internal object InkPlayer {
    /** Black from the bottom, feathering out over the top 40% (rule 4), never a hard edge. */
    val band = Brush.verticalGradient(
        0f to Color.Transparent,
        0.4f to Color.Black.copy(alpha = .85f),
        1f to Color.Black.copy(alpha = .85f),
    )

    fun word(action: PlayerControlAction, playing: Boolean): String = when (action) {
        PlayerControlAction.PLAY_PAUSE -> if (playing) "pause" else "play"
        PlayerControlAction.RESTART -> "restart"
        PlayerControlAction.EPISODES -> "episodes"
        PlayerControlAction.CHAPTERS -> "chapters"
        PlayerControlAction.NEXT_EPISODE -> "next"
        PlayerControlAction.STATS -> "stats"
        PlayerControlAction.AUDIO -> "audio"
        PlayerControlAction.SUBTITLES -> "subs"
        PlayerControlAction.SOURCES -> "sources"
        PlayerControlAction.MORE -> "more"
        PlayerControlAction.SPEED -> "speed"
        PlayerControlAction.ASPECT -> "aspect"
        PlayerControlAction.EXTERNAL -> "external"
        PlayerControlAction.ENGINE -> "engine"
        PlayerControlAction.REPORT -> "report"
        PlayerControlAction.WATCH_PARTY -> "party"
        PlayerControlAction.INFO -> "info"
    }
}

/** A control as a word: no pill, no icon, no zoom; selection is brightness. */
@Composable
internal fun InkControlWord(word: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, contentColor = InkTheme.dim,
            focusedContainerColor = Color.Transparent, focusedContentColor = InkTheme.ink,
            pressedContainerColor = Color.Transparent, pressedContentColor = InkTheme.mid,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Text(
            word,
            color = LocalContentColor.current,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

/** The title in Archivo Black (caps font) rather than the logo: the picture is the only colour. */
@Composable
internal fun InkPlayerTitle(title: String) {
    Text(
        title,
        color = InkTheme.ink,
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Elapsed left, remaining right, in Silkscreen at 70%; sits over the line. */
@Composable
internal fun InkPlayerTimes(viewModel: PlayerViewModel) {
    val timeline by viewModel.playbackTimeline.collectAsState()
    val shown = if (ScrubGlide.active) ScrubGlide.positionMs else timeline.currentPosition
    val remaining = (timeline.duration - shown).coerceAtLeast(0L)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(formatSeekTime(shown), color = InkTheme.mid, style = MaterialTheme.typography.labelLarge)
        Text("-" + formatSeekTime(remaining), color = InkTheme.mid, style = MaterialTheme.typography.labelLarge)
    }
}

/** Points the progress line at this title's accent, from the backdrop already in Coil's cache. */
@Composable
internal fun InkPlayerAccent(imageUrl: String?) {
    val context = LocalContext.current
    LaunchedEffect(imageUrl) {
        InkTheme.progressAccent.value = imageUrl?.takeIf(String::isNotBlank)?.let { inkAccentOf(context, it) } ?: InkTheme.ink
    }
}

/**
 * A small decode of the backdrop: from Coil's caches when it's there, else one fetch (the player's
 * own backdrop isn't always cached under this exact URL; a cache-only read came back empty).
 */
private suspend fun inkAccentOf(context: Context, url: String): Color? = withContext(Dispatchers.IO) {
    val result = try {
        context.imageLoader.execute(
            ImageRequest.Builder(context).data(url).size(48, 48).allowHardware(false)
                .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED).build()
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        Log.d("InkPlayer", "accent: load failed for $url: $e")
        null
    }
    val bitmap = ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
    if (bitmap == null) {
        Log.d("InkPlayer", "accent: no bitmap for $url (${result?.let { it::class.simpleName }})")
        return@withContext null
    }
    val width = minOf(48, bitmap.width)
    val height = minOf(48, bitmap.height)
    val pixels = IntArray(width * height) { i ->
        bitmap.getPixel((i % width) * bitmap.width / width, (i / width) * bitmap.height / height)
    }
    sampleArtworkAccent(pixels).also { Log.d("InkPlayer", "accent: $it from $url") }
}

/** The stream's badges as words in Silkscreen at 70%: same facts, no coloured logos. */
@Composable
internal fun InkPlayerBadges(badges: List<PlayerBadge>) {
    val resources = LocalContext.current.resources
    val words = badges.mapNotNull { badge ->
        when (badge) {
            is PlayerBadge.Pack -> badge.badge.name.trim().takeIf(String::isNotEmpty)
            is PlayerBadge.BuiltIn -> builtInWord(runCatching { resources.getResourceEntryName(badge.res) }.getOrNull())
        }
    }.distinct()
    if (words.isEmpty()) return
    Text(
        words.joinToString("   ").lowercase(),
        color = InkTheme.mid,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

private fun builtInWord(name: String?): String? = when (name?.removePrefix("badge_")) {
    null -> null
    "res_2160" -> "4k"
    "res_1080" -> "1080p"
    "res_720" -> "720p"
    "res_480" -> "480p"
    "hdr" -> "hdr"
    "hdr10" -> "hdr10"
    "dv" -> "dolby vision"
    "truehd" -> "truehd"
    "atmos" -> "atmos"
    "dts" -> "dts"
    "dts_hd" -> "dts-hd"
    "dts_hd_ma" -> "dts-hd ma"
    "dd" -> "dd"
    "ddp" -> "dd+"
    "ch_5_1" -> "5.1"
    "ch_7_1" -> "7.1"
    else -> name.removePrefix("badge_").replace('_', ' ')
}
