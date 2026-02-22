package com.eykettle.squircle.shape

import androidx.collection.LruCache
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private val SQRT2 = sqrt(2.0f)
private fun toRadians(degrees: Float): Float = (degrees * Math.PI / 180.0).toFloat()

internal data class AllCornerRadius(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
)

internal data class AllCornerPathArgs(
    val topLeft: CornerPathArgs,
    val topRight: CornerPathArgs,
    val bottomRight: CornerPathArgs,
    val bottomLeft: CornerPathArgs,
)

/**
 * Convenience factory for creating a squircle shape using unified radius.
 */
fun Squircle(
    cornerRadius: Dp,
    cornerSmoothing: CornerSmoothing = CornerSmoothing.Default
): Squircle = Squircle(
    topStart = CornerSize(cornerRadius),
    topEnd = CornerSize(cornerRadius),
    bottomEnd = CornerSize(cornerRadius),
    bottomStart = CornerSize(cornerRadius),
    cornerSmoothing = cornerSmoothing
)

/**
 * Convenience factory for creating a squircle shape specifying corners independently.
 */
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
 * Create a squircle shape.
 *
 * @param cornerSmoothing A value between 0.0 and 1.0 that controls the smoothness of the transition curve.
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

        internal data class CacheKey(
            val size: Size,
            val topLeftRadius: Float,
            val topRightRadius: Float,
            val bottomRightRadius: Float,
            val bottomLeftRadius: Float,
            val cornerSmoothing: Float
        )

        internal val cache = LruCache<CacheKey, Path>(100)
    }

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        // Handle RTL layouts correctly like standard Compose Shapes
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val topLeft = if (isLtr) topStart else topEnd
        val topRight = if (isLtr) topEnd else topStart
        val bottomLeft = if (isLtr) bottomStart else bottomEnd
        val bottomRight = if (isLtr) bottomEnd else bottomStart

        val allRadiusNone = topLeft == 0f && topRight == 0f &&
                bottomRight == 0f && bottomLeft == 0f
        if (allRadiusNone || size.width == 0f || size.height == 0f)
            return Outline.Rectangle(size.toRect())

        val initialRadius = AllCornerRadius(topLeft, topRight, bottomRight, bottomLeft)

        if (cornerSmoothing == CornerSmoothing.None) {
            return Outline.Rounded(
                RoundRect(
                    rect = size.toRect(),
                    topLeft = CornerRadius(initialRadius.topLeft),
                    topRight = CornerRadius(initialRadius.topRight),
                    bottomRight = CornerRadius(initialRadius.bottomRight),
                    bottomLeft = CornerRadius(initialRadius.bottomLeft)
                )
            )
        }

        val key = CacheKey(
            size,
            topLeft,
            topRight,
            bottomRight,
            bottomLeft,
            cornerSmoothing.value
        )
        cache[key]?.let { return Outline.Generic(it) }

        val finalRadius = normalizeRadius(initialRadius, size)
        val path = createSquirclePath(size, finalRadius, cornerSmoothing.value)
            .also { cache.put(key, it) }
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
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

    private fun normalizeRadius(initial: AllCornerRadius, size: Size): AllCornerRadius {
        val width = size.width
        val height = size.height

        // Object creation optimization
        if (initial.topLeft + initial.bottomLeft <= height &&
            initial.topRight + initial.bottomRight <= height &&
            initial.topLeft + initial.topRight <= width &&
            initial.bottomLeft + initial.bottomRight <= width
        ) {
            return initial
        }

        var topLeft = initial.topLeft
        var topRight = initial.topRight
        var bottomRight = initial.bottomRight
        var bottomLeft = initial.bottomLeft

        val heightCalc = {
            val leftDiameter = topLeft + bottomLeft
            if (leftDiameter > height && leftDiameter > 0) {
                topLeft = (topLeft / leftDiameter) * height
                bottomLeft = (bottomLeft / leftDiameter) * height
            }
            val rightDiameter = topRight + bottomRight
            if (rightDiameter > height && rightDiameter > 0) {
                topRight = (topRight / rightDiameter) * height
                bottomRight = (bottomRight / rightDiameter) * height
            }
        }
        val widthCalc = {
            val topDiameter = topLeft + topRight
            if (topDiameter > width && topDiameter > 0) {
                topLeft = (topLeft / topDiameter) * width
                topRight = (topRight / topDiameter) * width
            }
            val bottomDiameter = bottomLeft + bottomRight
            if (bottomDiameter > width && bottomDiameter > 0) {
                bottomLeft = (bottomLeft / bottomDiameter) * width
                bottomRight = (bottomRight / bottomDiameter) * width
            }
        }
        if (width > height) {
            heightCalc()
            widthCalc()
        } else {
            widthCalc()
            heightCalc()
        }
        return AllCornerRadius(topLeft, topRight, bottomRight, bottomLeft)
    }

    private fun createSquirclePath(
        size: Size,
        radius: AllCornerRadius,
        smoothing: Float
    ): Path {
        val width = size.width
        val height = size.height

        val topSpace = width - radius.topLeft - radius.topRight
        val rightSpace = height - radius.topRight - radius.bottomRight
        val bottomSpace = width - radius.bottomLeft - radius.bottomRight
        val leftSpace = height - radius.topLeft - radius.bottomLeft

        val initialArgs = if (
            radius.topLeft == radius.topRight &&
            radius.topRight == radius.bottomRight &&
            radius.bottomRight == radius.bottomLeft
        ) {
            val args = getPathArgsForCorner(
                radius = radius.topLeft,
                smoothing,
                verticalSpace = leftSpace / 2,
                horizontalSpace = topSpace / 2
            )
            AllCornerPathArgs(args, args, args, args)
        } else {
            AllCornerPathArgs(
                topLeft = getPathArgsForCorner(
                    radius.topLeft, smoothing, leftSpace / 2, topSpace / 2,
                ),
                topRight = getPathArgsForCorner(
                    radius.topRight, smoothing, rightSpace / 2, topSpace / 2,
                ),
                bottomRight = getPathArgsForCorner(
                    radius.bottomRight, smoothing, rightSpace / 2, bottomSpace / 2,
                ),
                bottomLeft = getPathArgsForCorner(
                    radius.bottomLeft, smoothing, leftSpace / 2, bottomSpace / 2,
                )
            )
        }

        val finalArgs =
            adjustTransition(
                initialArgs, size,
                topSpace, rightSpace, bottomSpace, leftSpace
            )

        val topMerged = topSpace <= 0
        val rightMerged = rightSpace <= 0
        val bottomMerged = bottomSpace <= 0
        val leftMerged = leftSpace <= 0

        return drawPath(width, height, finalArgs, topMerged, rightMerged, bottomMerged, leftMerged)
    }

    internal fun drawPath(
        width: Float, height: Float, args: AllCornerPathArgs,
        topMerged: Boolean, rightMerged: Boolean, bottomMerged: Boolean, leftMerged: Boolean
    ): Path {
        return Path().apply {
            moveTo(width - args.topRight.horizontalTransitionLength, 0f)

            drawTopRightCorner(
                width, halfStandardArcAngle, args.topRight, topMerged, rightMerged
            )
            lineTo(width, height - args.bottomRight.verticalTransitionLength)
            drawBottomRightCorner(
                width,
                height,
                halfStandardArcAngle,
                args.bottomRight,
                rightMerged,
                bottomMerged
            )
            lineTo(args.bottomLeft.horizontalTransitionLength, height)
            drawBottomLeftCorner(
                height, halfStandardArcAngle, args.bottomLeft, bottomMerged, leftMerged
            )
            lineTo(0f, args.topLeft.verticalTransitionLength)
            drawTopLeftCorner(halfStandardArcAngle, args.topLeft, leftMerged, topMerged)

            close()
        }
    }

    private fun adjustTransition(
        args: AllCornerPathArgs,
        size: Size,
        topSpace: Float,
        rightSpace: Float,
        bottomSpace: Float,
        leftSpace: Float
    ): AllCornerPathArgs {
        var tl = args.topLeft
        var tr = args.topRight
        var br = args.bottomRight
        var bl = args.bottomLeft

        // Logically, it should execute after beyond bound
        // Actually, it will never beyond bound
        if (tl.horizontalTransitionLength + tr.horizontalTransitionLength >= size.width) {
            val fullTransitionLength =
                tl.maxTransitionLength + tr.maxTransitionLength - tl.radius - tr.radius
            if (fullTransitionLength > 0) {
                val delta = topSpace / fullTransitionLength
                tl = adjustTransition(delta, true, tl)
                tr = adjustTransition(delta, true, tr)
            }
        }
        if (tr.verticalTransitionLength + br.verticalTransitionLength >= size.height) {
            val fullTransitionLength =
                tr.maxTransitionLength + br.maxTransitionLength - tr.radius - br.radius
            if (fullTransitionLength > 0) {
                val delta = rightSpace / fullTransitionLength
                tr = adjustTransition(delta, false, tr)
                br = adjustTransition(delta, false, br)
            }
        }
        if (br.horizontalTransitionLength + bl.horizontalTransitionLength >= size.width) {
            val fullTransitionLength =
                br.maxTransitionLength + bl.maxTransitionLength - br.radius - bl.radius
            if (fullTransitionLength > 0) {
                val delta = bottomSpace / fullTransitionLength
                br = adjustTransition(delta, true, br)
                bl = adjustTransition(delta, true, bl)
            }
        }
        if (bl.verticalTransitionLength + tl.verticalTransitionLength >= size.height) {
            val fullTransitionLength =
                bl.maxTransitionLength + tl.maxTransitionLength - bl.radius - tl.radius
            if (fullTransitionLength > 0) {
                val delta = leftSpace / fullTransitionLength
                bl = adjustTransition(delta, false, bl)
                tl = adjustTransition(delta, false, tl)
            }
        }
        return AllCornerPathArgs(tl, tr, br, bl)
    }

    private val halfStandardArcAngle: Float = 45 * (1 - cornerSmoothing.value)

    private fun calcLengthAB(
        transitionLength: Float, baseLengthCalcVal: Float, noSpace: Boolean
    ): Pair<Float, Float> {
        if (noSpace) return Pair(0f, 0f)
        val lengthB = (transitionLength - baseLengthCalcVal) / 3
        return Pair(2 * lengthB, lengthB)
    }

    private fun getPathArgsForCorner(
        radius: Float, smoothing: Float, verticalSpace: Float, horizontalSpace: Float
    ): CornerPathArgs {
        val maxTransitionLength = (1 + smoothing) * radius
        val verticalTransitionLength = min(maxTransitionLength, radius + verticalSpace)
        val horizontalTransitionLength = min(maxTransitionLength, radius + horizontalSpace)

        val arcMovementLength = sin(toRadians(halfStandardArcAngle)) * radius * SQRT2

        val halfComAngle = (45 - halfStandardArcAngle) / 2
        val distance34 = radius * tan(toRadians(halfComAngle))
        val transitionRadians = toRadians(45 * smoothing)
        val lengthD = distance34 * sin(transitionRadians)
        val lengthC = distance34 * cos(transitionRadians)

        val baseLengthCalcVal = arcMovementLength + lengthC + lengthD

        val (verticalLengthA, verticalLengthB) = calcLengthAB(
            transitionLength = verticalTransitionLength,
            baseLengthCalcVal = baseLengthCalcVal,
            noSpace = verticalSpace < 0
        )
        val (horizontalLengthA, horizontalLengthB) = calcLengthAB(
            transitionLength = horizontalTransitionLength,
            baseLengthCalcVal = baseLengthCalcVal,
            noSpace = horizontalSpace < 0
        )

        return CornerPathArgs(
            radius,
            arcMovementLength,
            lengthC,
            lengthD,
            verticalTransitionLength,
            horizontalTransitionLength,
            verticalLengthA,
            verticalLengthB,
            horizontalLengthA,
            horizontalLengthB,
            maxTransitionLength
        )
    }

    private fun adjustTransition(
        delta: Float, horizontal: Boolean, args: CornerPathArgs
    ): CornerPathArgs {
        val deltaLen = (if (horizontal) {
            args.horizontalLengthA - args.horizontalLengthB / 1.9f
        } else {
            args.verticalLengthA - args.verticalLengthB / 1.9f
        }) * (1 - delta).pow(3)

        return if (horizontal) {
            args.copy(
                horizontalLengthA = args.horizontalLengthA - deltaLen,
                horizontalLengthB = args.horizontalLengthB + deltaLen
            )
        } else {
            args.copy(
                verticalLengthA = args.verticalLengthA - deltaLen,
                verticalLengthB = args.verticalLengthB + deltaLen
            )
        }
    }
}

@JvmInline
value class CornerSmoothing(val value: Float) {
    init {
        require(value in 0f..1f) { "CornerSmoothing must be between 0.0 and 1.0" }
    }

    companion object {
        val Pretty = CornerSmoothing(0.8f)
        val Default = Pretty
        val Max = CornerSmoothing(1.0f)
        val iOS = CornerSmoothing(0.6f)
        val None = CornerSmoothing(0.0f)
    }
}

internal data class CornerPathArgs(
    val radius: Float,
    val arcMovementLength: Float,
    val lengthC: Float,
    val lengthD: Float,
    val verticalTransitionLength: Float,
    val horizontalTransitionLength: Float,
    val verticalLengthA: Float,
    val verticalLengthB: Float,
    val horizontalLengthA: Float,
    val horizontalLengthB: Float,
    val maxTransitionLength: Float
)

// Extension functions for drawing the corners

private fun Path.drawTopRightCorner(
    width: Float,
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    topMerged: Boolean,
    rightMerged: Boolean,
) {
    var startAngleDegrees = -45 - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2
    if (!topMerged) relativeCubicTo(
        args.horizontalLengthA,
        0f,
        args.horizontalLengthA + args.horizontalLengthB,
        0f,
        args.horizontalLengthA + args.horizontalLengthB + args.lengthC,
        args.lengthD
    )
    else {
        val topMovement = 45 - halfStandardArcAngle
        startAngleDegrees -= topMovement
        sweepAngleDegrees += topMovement
    }
    if (rightMerged) sweepAngleDegrees += 45 - halfStandardArcAngle
    arcTo(
        Rect(width - args.radius * 2, 0f, width, args.radius * 2),
        startAngleDegrees,
        sweepAngleDegrees,
        false
    )
    if (!rightMerged) relativeCubicTo(
        args.lengthD,
        args.lengthC,
        args.lengthD,
        args.lengthC + args.verticalLengthB,
        args.lengthD,
        args.lengthC + args.verticalLengthB + args.verticalLengthA
    )
}

private fun Path.drawBottomRightCorner(
    width: Float,
    height: Float,
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    rightMerged: Boolean,
    bottomMerged: Boolean,
) {
    var startAngleDegrees = 45 - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2
    if (!rightMerged) relativeCubicTo(
        0f,
        args.verticalLengthA,
        0f,
        args.verticalLengthA + args.verticalLengthB,
        -args.lengthD,
        args.verticalLengthA + args.verticalLengthB + args.lengthC
    )
    else {
        val rightMovement = 45 - halfStandardArcAngle
        startAngleDegrees -= rightMovement
        sweepAngleDegrees += rightMovement
    }
    if (bottomMerged) sweepAngleDegrees += 45 - halfStandardArcAngle
    arcTo(
        Rect(width - args.radius * 2, height - args.radius * 2, width, height),
        startAngleDegrees,
        sweepAngleDegrees,
        false
    )
    if (!bottomMerged) relativeCubicTo(
        -args.lengthC,
        args.lengthD,
        -args.lengthC - args.horizontalLengthB,
        args.lengthD,
        -args.lengthC - args.horizontalLengthB - args.horizontalLengthA,
        args.lengthD
    )
}

private fun Path.drawBottomLeftCorner(
    height: Float,
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    bottomMerged: Boolean,
    leftMerged: Boolean,
) {
    var startAngleDegrees = 135 - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2
    if (!bottomMerged) relativeCubicTo(
        -args.horizontalLengthA,
        0f,
        -args.horizontalLengthA - args.horizontalLengthB,
        0f,
        -args.horizontalLengthA - args.horizontalLengthB - args.lengthC,
        -args.lengthD
    )
    else {
        val bottomMovement = 45 - halfStandardArcAngle
        startAngleDegrees -= bottomMovement
        sweepAngleDegrees += bottomMovement
    }
    if (leftMerged) sweepAngleDegrees += 45 - halfStandardArcAngle
    arcTo(
        Rect(0f, height - args.radius * 2, args.radius * 2, height),
        startAngleDegrees,
        sweepAngleDegrees,
        false
    )
    if (!leftMerged) relativeCubicTo(
        -args.lengthD,
        -args.lengthC,
        -args.lengthD,
        -args.lengthC - args.verticalLengthB,
        -args.lengthD,
        -args.lengthC - args.verticalLengthB - args.verticalLengthA
    )
}

private fun Path.drawTopLeftCorner(
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    leftMerged: Boolean,
    topMerged: Boolean,
) {
    var startAngleDegrees = -135 - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2
    if (!leftMerged) relativeCubicTo(
        0f,
        -args.verticalLengthA,
        0f,
        -args.verticalLengthA - args.verticalLengthB,
        args.lengthD,
        -args.verticalLengthA - args.verticalLengthB - args.lengthC
    )
    else {
        val leftMovement = 45 - halfStandardArcAngle
        startAngleDegrees -= leftMovement
        sweepAngleDegrees += leftMovement
    }
    if (topMerged) sweepAngleDegrees += 45 - halfStandardArcAngle
    arcTo(
        Rect(0f, 0f, args.radius * 2, args.radius * 2), startAngleDegrees, sweepAngleDegrees, false
    )
    if (!topMerged) relativeCubicTo(
        args.lengthC,
        -args.lengthD,
        args.lengthC + args.horizontalLengthB,
        -args.lengthD,
        args.lengthC + args.horizontalLengthB + args.horizontalLengthA,
        -args.lengthD
    )
}