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

class VmdBezierVector3fInterpolator() : AnimationInterpolator<Vector3f> {
    private var _component: VmdBezierChannelComponent? = null
    private val component
        get() = _component ?: throw IllegalStateException("VmdBezierChannelComponent not attached")

    fun attachComponent(component: VmdBezierChannelComponent) {
        require(component.channels == 3) { "VmdBezierVector3fInterpolator must be attached with 3 channels" }
        _component = component
    }

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
        val component = component
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

class VmdBezierQuaternionfInterpolator() : AnimationInterpolator<Quaternionf> {
    private var _component: VmdBezierChannelComponent? = null
    private val component
        get() = _component ?: throw IllegalStateException("VmdBezierChannelComponent not attached")

    fun attachComponent(component: VmdBezierChannelComponent) {
        require(component.channels == 1) { "VmdBezierVector3fInterpolator must be attached with 1 channels" }
        _component = component
    }

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
