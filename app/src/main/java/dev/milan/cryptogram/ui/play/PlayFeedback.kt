package dev.milan.cryptogram.ui.play

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Tap tick, wrong-letter buzz and solve chime (design doc section 6). Haptics always
 * work; sound plays only if the optional `res/raw/{tick,wrong,solve}.ogg` assets are
 * present in the build.
 */
class PlayFeedback(
    context: Context,
    private val haptic: (HapticFeedbackType) -> Unit,
) {
    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private fun soundId(context: Context, name: String): Int {
        val res = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (res != 0) pool.load(context, res, 1) else 0
    }

    private val tick = soundId(context, "tick")
    private val wrong = soundId(context, "wrong")
    private val solve = soundId(context, "solve")

    fun onTap() {
        if (hapticsEnabled) haptic(HapticFeedbackType.TextHandleMove)
        play(tick)
    }

    fun onWrong() {
        if (hapticsEnabled) haptic(HapticFeedbackType.LongPress)
        play(wrong)
    }

    fun onSolve() {
        if (hapticsEnabled) haptic(HapticFeedbackType.LongPress)
        play(solve)
    }

    private fun play(id: Int) {
        if (id != 0 && soundEnabled) pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}

@Composable
fun rememberPlayFeedback(soundEnabled: Boolean, hapticsEnabled: Boolean): PlayFeedback {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val feedback = remember {
        PlayFeedback(context.applicationContext) { haptics.performHapticFeedback(it) }
    }
    feedback.soundEnabled = soundEnabled
    feedback.hapticsEnabled = hapticsEnabled
    DisposableEffect(Unit) { onDispose { feedback.release() } }
    return feedback
}
