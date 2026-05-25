package com.calorietracker.presentation.theme

import android.provider.Settings
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

object MotionDurations {
    const val QUICK = 200
    const val STANDARD = 300
    const val DELIBERATE = 600
    const val SLOW = 800
    const val SHIMMER_CYCLE = 1200
}

@Composable
@ReadOnlyComposable
fun shouldReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val transitionScale = Settings.Global.getFloat(
        resolver,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        1f
    )
    val animatorScale = Settings.Global.getFloat(
        resolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    return transitionScale == 0f || animatorScale == 0f
}

@Composable
fun <T> motionTween(durationMillis: Int): DurationBasedAnimationSpec<T> {
    return if (shouldReduceMotion()) snap() else tween(durationMillis = durationMillis)
}
