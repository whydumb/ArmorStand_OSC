package top.fifthlight.blazerod.model.vmd

import org.joml.*
import top.fifthlight.blazerod.model.animation.AnimationInterpolation
import top.fifthlight.blazerod.model.animation.AnimationInterpolator
import top.fifthlight.blazerod.model.util.MutableFloat

object VmdBezierInterpolation: AnimationInterpolation(1) {
    override fun interpolateVector3f(
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        startValue: List<Vector3fc>,
        endValue: List<Vector3fc>,
        result: Vector3f,
    ) = throw UnsupportedOperationException("VMD doesn't have interpolator")

    override fun interpolateQuaternionf(
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        startValue: List<Quaternionfc>,
        endValue: List<Quaternionfc>,
        result: Quaternionf,
    ) = throw UnsupportedOperationException("VMD doesn't have interpolator")

    override fun interpolateFloat(
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        startValue: List<MutableFloat>,
        endValue: List<MutableFloat>,
        result: MutableFloat,
    ) = throw UnsupportedOperationException("VMD doesn't have interpolator")
}

abstract class VmdBezierInterpolator<T> : AnimationInterpolator<T> {
    protected var _component: VmdBezierChannelComponent? = null
    protected val component: VmdBezierChannelComponent
        get() = _component ?: throw IllegalStateException("VmdBezierChannelComponent not attached")
    protected abstract val expectedChannels: Int

    fun attachComponent(component: VmdBezierChannelComponent) {
        require(component.channels == expectedChannels) {
            "VmdBezierInterpolator must be attached with $expectedChannels channels for this type."
        }
        _component = component
    }
}

class VmdBezierVector3fInterpolator : VmdBezierInterpolator<Vector3f>() {
    override val expectedChannels
        get() = 3

    override fun set(value: List<Vector3f>, result: Vector3f) {
        result.set(value[0])
    }

    override fun interpolate(
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        type: AnimationInterpolation,
        startValue: List<Vector3f>,
        endValue: List<Vector3f>,
        result: Vector3f,
    ) {
        val xDelta = component.getDelta(startFrame, 0, delta)
        val yDelta = component.getDelta(startFrame, 1, delta)
        val zDelta = component.getDelta(startFrame, 2, delta)
        val start = startValue[0]
        val end = endValue[0]
        result.set(
            Math.lerp(start.x, end.x, xDelta),
            Math.lerp(start.y, end.y, yDelta),
            Math.lerp(start.z, end.z, zDelta),
        )
    }
}

class VmdBezierSimpleVector3fInterpolator : VmdBezierInterpolator<Vector3f>() {
    override val expectedChannels
        get() = 1

    override fun set(value: List<Vector3f>, result: Vector3f) {
        result.set(value[0])
    }

    override fun interpolate(
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        type: AnimationInterpolation,
        startValue: List<Vector3f>,
        endValue: List<Vector3f>,
        result: Vector3f,
    ) {
        val delta = component.getDelta(startFrame, 0, delta)
        val start = startValue[0]
        val end = endValue[0]
        start.lerp(end, delta, result)
    }
}

class VmdBezierQuaternionfInterpolator : VmdBezierInterpolator<Quaternionf>() {
    override val expectedChannels
        get() = 1

    override fun set(value: List<Quaternionf>, result: Quaternionf) {
        result.set(value[0])
    }

    override fun interpolate(
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        type: AnimationInterpolation,
        startValue: List<Quaternionf>,
        endValue: List<Quaternionf>,
        result: Quaternionf,
    ) {
        val bezierDelta = component.getDelta(startFrame, 0, delta)
        result.set(startValue[0]).slerp(endValue[0], bezierDelta)
    }
}

class VmdBezierFloatInterpolator : VmdBezierInterpolator<MutableFloat>() {
    override val expectedChannels
        get() = 1

    override fun set(value: List<MutableFloat>, result: MutableFloat) {
        result.value = value[0].value
    }

    override fun interpolate(
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        type: AnimationInterpolation,
        startValue: List<MutableFloat>,
        endValue: List<MutableFloat>,
        result: MutableFloat,
    ) {
        val bezierDelta = component.getDelta(startFrame, 0, delta)
        result.value = Math.lerp(startValue[0].value, endValue[0].value, bezierDelta)
    }
}