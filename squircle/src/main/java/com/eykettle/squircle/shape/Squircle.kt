package com.eykettle.squircle.shape

import androidx.collection.LruCache
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eykettle.squircle.CornerSmoothing
import com.eykettle.squircle.addSquircle

fun Squircle(
    cornerRadius: Dp, cornerSmoothing: CornerSmoothing = CornerSmoothing.Default
): Squircle = Squircle(
    topStart = CornerSize(cornerRadius),
    topEnd = CornerSize(cornerRadius),
    bottomEnd = CornerSize(cornerRadius),
    bottomStart = CornerSize(cornerRadius),
    cornerSmoothing = cornerSmoothing
)

fun Squircle(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
    cornerSmoothing: CornerSmoothing = CornerSmoothing.Default
): Squircle = Squircle(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart),
    cornerSmoothing = cornerSmoothing
)

/**
 * A Shape that draws a squircle (smooth rounded rectangle).
 *
 * @param cornerSmoothing A value between 0.0 and 1.0 that controls the smoothness of the curve.
 */
@Immutable
class Squircle(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    val cornerSmoothing: CornerSmoothing = CornerSmoothing.Default,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    companion object {
        val Default = Squircle(cornerRadius = 8.dp)
        val Max = Squircle(cornerSmoothing = CornerSmoothing.Max)
        val Pretty = Squircle(cornerSmoothing = CornerSmoothing.Pretty)
        val iOS = Squircle(cornerSmoothing = CornerSmoothing.iOS)

        private data class CacheKey(
            val size: Size,
            val topLeftRadius: Float,
            val topRightRadius: Float,
            val bottomRightRadius: Float,
            val bottomLeftRadius: Float,
            val cornerSmoothing: Float
        )

        private val cache = LruCache<CacheKey, Path>(100)
    }

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val topLeft = if (isLtr) topStart else topEnd
        val topRight = if (isLtr) topEnd else topStart
        val bottomLeft = if (isLtr) bottomStart else bottomEnd
        val bottomRight = if (isLtr) bottomEnd else bottomStart

        if (topLeft == 0f && topRight == 0f && bottomRight == 0f && bottomLeft == 0f) {
            return Outline.Rectangle(size.toRect())
        }

        if (size.isEmpty()) {
            return Outline.Rectangle(Rect.Zero)
        }

        val key = CacheKey(
            size, topLeft, topRight, bottomRight, bottomLeft, cornerSmoothing.value
        )

        cache[key]?.let { return Outline.Generic(it) }

        val path = Path().apply {
            addSquircle(
                rect = size.toRect(),
                topLeftRadius = topLeft,
                topRightRadius = topRight,
                bottomRightRadius = bottomRight,
                bottomLeftRadius = bottomLeft,
                cornerSmoothing = cornerSmoothing
            )
        }

        cache.put(key, path)
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize
    ): CornerBasedShape = Squircle(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        cornerSmoothing = cornerSmoothing
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Squircle) return false

        if (topStart != other.topStart) return false
        if (topEnd != other.topEnd) return false
        if (bottomEnd != other.bottomEnd) return false
        if (bottomStart != other.bottomStart) return false
        if (cornerSmoothing != other.cornerSmoothing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + cornerSmoothing.hashCode()
        return result
    }

    override fun toString(): String {
        return "Squircle(topStart = $topStart, topEnd = $topEnd, bottomEnd = $bottomEnd, bottomStart = $bottomStart, cornerSmoothing = $cornerSmoothing)"
    }
}