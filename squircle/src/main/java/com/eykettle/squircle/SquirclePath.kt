package com.eykettle.squircle

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Adds a squircle shape to the current path.
 *
 * @param rect The bounds in which the squircle should be drawn.
 * @param topLeftRadius The radius for the top-left corner in pixels.
 * @param topRightRadius The radius for the top-right corner in pixels.
 * @param bottomRightRadius The radius for the bottom-right corner in pixels.
 * @param bottomLeftRadius The radius for the bottom-left corner in pixels.
 * @param cornerSmoothing The smoothing factor of the corners.
 */
fun Path.addSquircle(
    rect: Rect,
    topLeftRadius: Float,
    topRightRadius: Float,
    bottomRightRadius: Float,
    bottomLeftRadius: Float,
    cornerSmoothing: CornerSmoothing
) {
    if (rect.isEmpty) return

    if (cornerSmoothing == CornerSmoothing.None) {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect,
                androidx.compose.ui.geometry.CornerRadius(topLeftRadius),
                androidx.compose.ui.geometry.CornerRadius(topRightRadius),
                androidx.compose.ui.geometry.CornerRadius(bottomRightRadius),
                androidx.compose.ui.geometry.CornerRadius(bottomLeftRadius)
            )
        )
        return
    }

    val initialRadii =
        AllCornerRadius(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius)
    val finalRadii = normalizeRadius(initialRadii, rect.width, rect.height)
    val params = calculateSquircleArgs(rect.width, rect.height, finalRadii, cornerSmoothing.value)

    drawSquirclePath(rect, params, cornerSmoothing.value)
}

private val SQRT2 = sqrt(2.0f)

private fun toRadians(degrees: Float): Float = (degrees * Math.PI / 180.0).toFloat()

private fun getHalfStandardArcAngle(smoothing: Float): Float = 45f * (1f - smoothing)

private data class AllCornerRadius(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
)

private data class CornerPathArgs(
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

private data class AllCornerPathArgs(
    val topLeft: CornerPathArgs,
    val topRight: CornerPathArgs,
    val bottomRight: CornerPathArgs,
    val bottomLeft: CornerPathArgs,
)

private data class PathDrawParams(
    val args: AllCornerPathArgs,
    val topMerged: Boolean,
    val rightMerged: Boolean,
    val bottomMerged: Boolean,
    val leftMerged: Boolean
)

private fun normalizeRadius(
    initial: AllCornerRadius, width: Float, height: Float
): AllCornerRadius {
    if (initial.topLeft + initial.bottomLeft <= height && initial.topRight + initial.bottomRight <= height && initial.topLeft + initial.topRight <= width && initial.bottomLeft + initial.bottomRight <= width) {
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

private fun calculateSquircleArgs(
    width: Float,
    height: Float,
    radius: AllCornerRadius,
    smoothing: Float,
): PathDrawParams {
    val topSpace = width - radius.topLeft - radius.topRight
    val rightSpace = height - radius.topRight - radius.bottomRight
    val bottomSpace = width - radius.bottomLeft - radius.bottomRight
    val leftSpace = height - radius.topLeft - radius.bottomLeft

    val initialArgs =
        if (radius.topLeft == radius.topRight && radius.topRight == radius.bottomRight && radius.bottomRight == radius.bottomLeft) {
            val args =
                getPathArgsForCorner(radius.topLeft, smoothing, leftSpace / 2f, topSpace / 2f)
            AllCornerPathArgs(args, args, args, args)
        } else {
            AllCornerPathArgs(
                topLeft = getPathArgsForCorner(
                    radius.topLeft, smoothing, leftSpace / 2f, topSpace / 2f
                ), topRight = getPathArgsForCorner(
                    radius.topRight, smoothing, rightSpace / 2f, topSpace / 2f
                ), bottomRight = getPathArgsForCorner(
                    radius.bottomRight, smoothing, rightSpace / 2f, bottomSpace / 2f
                ), bottomLeft = getPathArgsForCorner(
                    radius.bottomLeft, smoothing, leftSpace / 2f, bottomSpace / 2f
                )
            )
        }

    val finalArgs =
        adjustTransition(initialArgs, width, height, topSpace, rightSpace, bottomSpace, leftSpace)

    return PathDrawParams(
        args = finalArgs,
        topMerged = topSpace <= 0f,
        rightMerged = rightSpace <= 0f,
        bottomMerged = bottomSpace <= 0f,
        leftMerged = leftSpace <= 0f
    )
}

private fun calcLengthAB(
    transitionLength: Float, baseLengthCalcVal: Float, noSpace: Boolean
): Pair<Float, Float> {
    if (noSpace) return Pair(0f, 0f)
    val lengthB = (transitionLength - baseLengthCalcVal) / 3f
    return Pair(2f * lengthB, lengthB)
}

private fun getPathArgsForCorner(
    radius: Float, smoothing: Float, verticalSpace: Float, horizontalSpace: Float
): CornerPathArgs {
    val maxTransitionLength = (1f + smoothing) * radius
    val verticalTransitionLength = min(maxTransitionLength, radius + verticalSpace)
    val horizontalTransitionLength = min(maxTransitionLength, radius + horizontalSpace)

    val halfStandardArcAngle = getHalfStandardArcAngle(smoothing)
    val arcMovementLength = sin(toRadians(halfStandardArcAngle)) * radius * SQRT2

    val halfComAngle = (45f - halfStandardArcAngle) / 2f
    val distance34 = radius * tan(toRadians(halfComAngle))
    val transitionRadians = toRadians(45f * smoothing)
    val lengthD = distance34 * sin(transitionRadians)
    val lengthC = distance34 * cos(transitionRadians)

    val baseLengthCalcVal = arcMovementLength + lengthC + lengthD

    val (verticalLengthA, verticalLengthB) = calcLengthAB(
        transitionLength = verticalTransitionLength,
        baseLengthCalcVal = baseLengthCalcVal,
        noSpace = verticalSpace < 0f
    )
    val (horizontalLengthA, horizontalLengthB) = calcLengthAB(
        transitionLength = horizontalTransitionLength,
        baseLengthCalcVal = baseLengthCalcVal,
        noSpace = horizontalSpace < 0f
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
    }) * (1f - delta).pow(3)

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

private fun adjustTransition(
    args: AllCornerPathArgs,
    width: Float,
    height: Float,
    topSpace: Float,
    rightSpace: Float,
    bottomSpace: Float,
    leftSpace: Float
): AllCornerPathArgs {
    var tl = args.topLeft
    var tr = args.topRight
    var br = args.bottomRight
    var bl = args.bottomLeft

    if (tl.horizontalTransitionLength + tr.horizontalTransitionLength >= width) {
        val fullTransitionLength =
            tl.maxTransitionLength + tr.maxTransitionLength - tl.radius - tr.radius
        if (fullTransitionLength > 0f) {
            val delta = topSpace / fullTransitionLength
            tl = adjustTransition(delta, true, tl)
            tr = adjustTransition(delta, true, tr)
        }
    }
    if (tr.verticalTransitionLength + br.verticalTransitionLength >= height) {
        val fullTransitionLength =
            tr.maxTransitionLength + br.maxTransitionLength - tr.radius - br.radius
        if (fullTransitionLength > 0f) {
            val delta = rightSpace / fullTransitionLength
            tr = adjustTransition(delta, false, tr)
            br = adjustTransition(delta, false, br)
        }
    }
    if (br.horizontalTransitionLength + bl.horizontalTransitionLength >= width) {
        val fullTransitionLength =
            br.maxTransitionLength + bl.maxTransitionLength - br.radius - bl.radius
        if (fullTransitionLength > 0f) {
            val delta = bottomSpace / fullTransitionLength
            br = adjustTransition(delta, true, br)
            bl = adjustTransition(delta, true, bl)
        }
    }
    if (bl.verticalTransitionLength + tl.verticalTransitionLength >= height) {
        val fullTransitionLength =
            bl.maxTransitionLength + tl.maxTransitionLength - bl.radius - tl.radius
        if (fullTransitionLength > 0f) {
            val delta = leftSpace / fullTransitionLength
            bl = adjustTransition(delta, false, bl)
            tl = adjustTransition(delta, false, tl)
        }
    }
    return AllCornerPathArgs(tl, tr, br, bl)
}

private fun Path.drawSquirclePath(rect: Rect, params: PathDrawParams, smoothing: Float) {
    val args = params.args
    val angle = getHalfStandardArcAngle(smoothing)

    moveTo(rect.right - args.topRight.horizontalTransitionLength, rect.top)

    drawTopRightCorner(rect, angle, args.topRight, params.topMerged, params.rightMerged)
    lineTo(rect.right, rect.bottom - args.bottomRight.verticalTransitionLength)

    drawBottomRightCorner(rect, angle, args.bottomRight, params.rightMerged, params.bottomMerged)
    lineTo(rect.left + args.bottomLeft.horizontalTransitionLength, rect.bottom)

    drawBottomLeftCorner(rect, angle, args.bottomLeft, params.bottomMerged, params.leftMerged)
    lineTo(rect.left, rect.top + args.topLeft.verticalTransitionLength)

    drawTopLeftCorner(rect, angle, args.topLeft, params.leftMerged, params.topMerged)

    close()
}

private fun Path.drawTopRightCorner(
    rect: Rect,
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    topMerged: Boolean,
    rightMerged: Boolean
) {
    var startAngleDegrees = -45f - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2f

    if (!topMerged) {
        relativeCubicTo(
            args.horizontalLengthA,
            0f,
            args.horizontalLengthA + args.horizontalLengthB,
            0f,
            args.horizontalLengthA + args.horizontalLengthB + args.lengthC,
            args.lengthD
        )
    } else {
        val topMovement = 45f - halfStandardArcAngle
        startAngleDegrees -= topMovement
        sweepAngleDegrees += topMovement
    }

    if (rightMerged) sweepAngleDegrees += 45f - halfStandardArcAngle

    arcTo(
        Rect(rect.right - args.radius * 2f, rect.top, rect.right, rect.top + args.radius * 2f),
        startAngleDegrees,
        sweepAngleDegrees,
        false
    )

    if (!rightMerged) {
        relativeCubicTo(
            args.lengthD,
            args.lengthC,
            args.lengthD,
            args.lengthC + args.verticalLengthB,
            args.lengthD,
            args.lengthC + args.verticalLengthB + args.verticalLengthA
        )
    }
}

private fun Path.drawBottomRightCorner(
    rect: Rect,
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    rightMerged: Boolean,
    bottomMerged: Boolean
) {
    var startAngleDegrees = 45f - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2f

    if (!rightMerged) {
        relativeCubicTo(
            0f,
            args.verticalLengthA,
            0f,
            args.verticalLengthA + args.verticalLengthB,
            -args.lengthD,
            args.verticalLengthA + args.verticalLengthB + args.lengthC
        )
    } else {
        val rightMovement = 45f - halfStandardArcAngle
        startAngleDegrees -= rightMovement
        sweepAngleDegrees += rightMovement
    }

    if (bottomMerged) sweepAngleDegrees += 45f - halfStandardArcAngle

    arcTo(
        Rect(
            rect.right - args.radius * 2f, rect.bottom - args.radius * 2f, rect.right, rect.bottom
        ), startAngleDegrees, sweepAngleDegrees, false
    )

    if (!bottomMerged) {
        relativeCubicTo(
            -args.lengthC,
            args.lengthD,
            -args.lengthC - args.horizontalLengthB,
            args.lengthD,
            -args.lengthC - args.horizontalLengthB - args.horizontalLengthA,
            args.lengthD
        )
    }
}

private fun Path.drawBottomLeftCorner(
    rect: Rect,
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    bottomMerged: Boolean,
    leftMerged: Boolean
) {
    var startAngleDegrees = 135f - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2f

    if (!bottomMerged) {
        relativeCubicTo(
            -args.horizontalLengthA,
            0f,
            -args.horizontalLengthA - args.horizontalLengthB,
            0f,
            -args.horizontalLengthA - args.horizontalLengthB - args.lengthC,
            -args.lengthD
        )
    } else {
        val bottomMovement = 45f - halfStandardArcAngle
        startAngleDegrees -= bottomMovement
        sweepAngleDegrees += bottomMovement
    }

    if (leftMerged) sweepAngleDegrees += 45f - halfStandardArcAngle

    arcTo(
        Rect(rect.left, rect.bottom - args.radius * 2f, rect.left + args.radius * 2f, rect.bottom),
        startAngleDegrees,
        sweepAngleDegrees,
        false
    )

    if (!leftMerged) {
        relativeCubicTo(
            -args.lengthD,
            -args.lengthC,
            -args.lengthD,
            -args.lengthC - args.verticalLengthB,
            -args.lengthD,
            -args.lengthC - args.verticalLengthB - args.verticalLengthA
        )
    }
}

private fun Path.drawTopLeftCorner(
    rect: Rect,
    halfStandardArcAngle: Float,
    args: CornerPathArgs,
    leftMerged: Boolean,
    topMerged: Boolean
) {
    var startAngleDegrees = -135f - halfStandardArcAngle
    var sweepAngleDegrees = halfStandardArcAngle * 2f

    if (!leftMerged) {
        relativeCubicTo(
            0f,
            -args.verticalLengthA,
            0f,
            -args.verticalLengthA - args.verticalLengthB,
            args.lengthD,
            -args.verticalLengthA - args.verticalLengthB - args.lengthC
        )
    } else {
        val leftMovement = 45f - halfStandardArcAngle
        startAngleDegrees -= leftMovement
        sweepAngleDegrees += leftMovement
    }

    if (topMerged) sweepAngleDegrees += 45f - halfStandardArcAngle

    arcTo(
        Rect(rect.left, rect.top, rect.left + args.radius * 2f, rect.top + args.radius * 2f),
        startAngleDegrees,
        sweepAngleDegrees,
        false
    )

    if (!topMerged) {
        relativeCubicTo(
            args.lengthC,
            -args.lengthD,
            args.lengthC + args.horizontalLengthB,
            -args.lengthD,
            args.lengthC + args.horizontalLengthB + args.horizontalLengthA,
            -args.lengthD
        )
    }
}