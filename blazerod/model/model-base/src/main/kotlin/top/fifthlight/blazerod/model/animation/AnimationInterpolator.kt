package top.fifthlight.blazerod.model.animation

import org.joml.Quaternionf
import org.joml.Vector3f

enum class AnimationInterpolation(val elements: Int) {
    LINEAR(1),
    STEP(1),
    CUBIC_SPLINE(3),
}

interface AnimationInterpolator<T> {
    fun set(value: List<T>, result: T)

    fun interpolate(
        delta: Float,
        type: AnimationInterpolation,
        startValue: List<T>,
        endValue: List<T>,
        result: T,
    )
}

object Vector3AnimationInterpolator : AnimationInterpolator<Vector3f> {
    override fun set(value: List<Vector3f>, result: Vector3f) {
        result.set(value[0])
    }

    override fun interpolate(
        delta: Float,
        type: AnimationInterpolation,
        startValue: List<Vector3f>,
        endValue: List<Vector3f>,
        result: Vector3f,
    ) {
        when (type) {
            AnimationInterpolation.LINEAR -> result.set(startValue[0]).lerp(endValue[0], delta)
            AnimationInterpolation.STEP -> result.set(startValue[0])
            AnimationInterpolation.CUBIC_SPLINE -> {
                val t = delta
                val t2 = t * t
                val t3 = t2 * t

                // Hermite spline公式
                val h1 = 2f * t3 - 3f * t2 + 1f
                val h2 = t3 - 2f * t2 + t
                val h3 = -2f * t3 + 3f * t2
                val h4 = t3 - t2

                result.set(
                    startValue[1].mul(h1)
                        .add(startValue[2].mul(h2))
                        .add(endValue[1].mul(h3))
                        .add(endValue[0].mul(h4))
                )
            }
        }
    }
}

object QuaternionAnimationInterpolator : AnimationInterpolator<Quaternionf> {
    override fun set(value: List<Quaternionf>, result: Quaternionf) {
        result.set(value[0])
    }

    override fun interpolate(
        delta: Float,
        type: AnimationInterpolation,
        startValue: List<Quaternionf>,
        endValue: List<Quaternionf>,
        result: Quaternionf,
    ) {
        when (type) {
            AnimationInterpolation.LINEAR -> result.set(startValue[0]).slerp(endValue[0], delta)
            AnimationInterpolation.STEP -> result.set(startValue[0])
            AnimationInterpolation.CUBIC_SPLINE -> {
                val t = delta
                val t2 = t * t
                val t3 = t2 * t

                val h1 = 2f * t3 - 3f * t2 + 1f
                val h2 = t3 - 2f * t2 + t
                val h3 = -2f * t3 + 3f * t2
                val h4 = t3 - t2

                result.set(
                    startValue[1].mul(h1)
                        .add(startValue[2].mul(h2))
                        .add(endValue[1].mul(h3))
                        .add(endValue[0].mul(h4))
                )
            }
        }
    }
}
