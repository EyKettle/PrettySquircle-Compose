package com.eykettle.squircle

import androidx.compose.runtime.Immutable

/**
 * Defines the smoothness of the squircle corners.
 * Represents a value between 0.0 (standard rounded rect) and 1.0 (maximum smoothing).
 */
@Immutable
@JvmInline
value class CornerSmoothing(val value: Float) {
    init {
        require(value in 0f..1f) { "CornerSmoothing must be between 0.0 and 1.0" }
    }

    companion object {
        val None = CornerSmoothing(0.0f)
        val iOS = CornerSmoothing(0.6f)
        val Pretty = CornerSmoothing(0.8f)
        val Default = Pretty
        val Max = CornerSmoothing(1.0f)
    }
}