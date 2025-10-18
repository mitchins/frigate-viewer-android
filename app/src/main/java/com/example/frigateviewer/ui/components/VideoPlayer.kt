package com.example.frigateviewer.ui.components

import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import org.videolan.libvlc.util.VLCVideoLayout
import org.videolan.libvlc.MediaPlayer.Event as VlcEvent

@Composable
fun VideoPlayer(
    libVLC: LibVLC,
    streamUrl: String,
    cameraName: String,
    enableAudio: Boolean = false,
    targetAspect: Float? = null, // when set, enforce aspect ratio (e.g., for wall fill)
    onAspectRatio: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mediaPlayer = remember {
        MediaPlayer(libVLC).apply {
            setEventListener { event ->
                when (event.type) {
                    VlcEvent.Playing -> Log.d("VideoPlayer", "VLC state: Playing")
                    VlcEvent.Buffering -> Log.d("VideoPlayer", "VLC state: Buffering ${event.buffering}")
                    VlcEvent.Paused -> Log.d("VideoPlayer", "VLC state: Paused")
                    VlcEvent.Stopped -> Log.d("VideoPlayer", "VLC state: Stopped")
                    VlcEvent.EndReached -> Log.d("VideoPlayer", "VLC state: EndReached")
                    VlcEvent.EncounteredError -> Log.e("VideoPlayer", "VLC state: Error encountered")
                    else -> {}
                }
            }
        }
    }

    // Keep latest listener across recompositions
    val latestAspectListener by rememberUpdatedState(onAspectRatio)

    // Note: For this libVLC version, per-frame layout callbacks must be handled via
    // DisplayManager. As a safe fallback, we keep the hook but do not attach it here.

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply { keepScreenOn = true }
            },
            update = { videoLayout ->
                // Attach VLC output to the provided layout
                val vlcVout = mediaPlayer.vlcVout
                if (!vlcVout.areViewsAttached()) {
                    mediaPlayer.attachViews(videoLayout, null, false, false)
                    if (!mediaPlayer.isPlaying) mediaPlayer.play()
                }
                // Apply target aspect ratio if requested
                try {
                    if (targetAspect != null && targetAspect > 0f) {
                        val num = (targetAspect * 1000f).toInt().coerceAtLeast(1)
                        mediaPlayer.setAspectRatio("${num}:1000")
                    } else {
                        mediaPlayer.setAspectRatio(null)
                    }
                } catch (_: Throwable) { }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camera name overlay
        Text(
            text = cameraName,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    DisposableEffect(streamUrl, enableAudio) {
        Log.d("VideoPlayer", "Preparing media for $cameraName -> $streamUrl")
        // Stop any previous playback before switching media to avoid codec deadlocks
        try { if (mediaPlayer.isPlaying) mediaPlayer.stop() } catch (_: Throwable) { }

        val media = Media(libVLC, Uri.parse(streamUrl)).apply {
            // Prefer hardware decoding when available
            setHWDecoderEnabled(true, false)
            // Media-level options to ensure RTSP TCP and resiliency
            addOption(":rtsp-tcp")
            addOption(":rtsp-reconnect")
            addOption(":network-caching=300")
            // Avoid Live555 frame truncation on high-bitrate IDR frames
            addOption(":rtsp-frame-buffer-size=2000000")
            // Reduce backlog/stalls when switching selections quickly
            addOption(":drop-late-frames")
            addOption(":skip-frames")
            if (!enableAudio) addOption(":no-audio")
        }

        mediaPlayer.media = media
        media.release()

        // Start will actually occur on onSurfacesCreated; if views already attached, ensure play
        if (mediaPlayer.vlcVout.areViewsAttached() && !mediaPlayer.isPlaying) {
            mediaPlayer.play()
        }

        onDispose {
            try {
                mediaPlayer.stop()
            } catch (_: Throwable) { }
            try {
                // Detach also clears layout listener
                mediaPlayer.detachViews()
            } catch (_: Throwable) { }
        }
    }

    DisposableEffect(Unit) {
        onDispose { try { mediaPlayer.release() } catch (_: Throwable) { } }
    }
}
