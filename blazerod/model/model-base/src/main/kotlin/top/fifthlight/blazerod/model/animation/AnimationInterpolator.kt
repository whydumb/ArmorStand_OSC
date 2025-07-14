package top.fifthlight.blazerod.model.animation

import org.joml.*
import top.fifthlight.blazerod.model.util.MutableFloat

abstract class AnimationInterpolation(val elements: Int) {
    init {
        require(elements in 1..MAX_ELEMENTS) { "Bad elements count: should be in [1, $MAX_ELEMENTS]" }
    }

    abstract fun interpolateVector3f(
        delta: Float,
        startValue: List<Vector3fc>,
        endValue: List<Vector3fc>,
        result: Vector3f,
    )

    abstract fun interpolateQuaternionf(
        delta: Float,
        startValue: List<Quaternionfc>,
        endValue: List<Quaternionfc>,
        result: Quaternionf,
    )

    abstract fun interpolateFloat(
        delta: Float,
        startValue: List<MutableFloat>,
        endValue: List<MutableFloat>,
        result: MutableFloat,
    )

    companion object {
        const val MAX_ELEMENTS = 4

        val linear = object : AnimationInterpolation(1) {
            override fun interpolateVector3f(
                delta: Float,
                startValue: List<Vector3fc>,
                endValue: List<Vector3fc>,
                result: Vector3f,
            ) {
                result.set(startValue[0]).lerp(endValue[0], delta)
            }

            override fun interpolateQuaternionf(
                delta: Float,
                startValue: List<Quaternionfc>,
                endValue: List<Quaternionfc>,
                result: Quaternionf,
            ) {
                result.set(startValue[0]).slerp(endValue[0], delta)
            }

            override fun interpolateFloat(
                delta: Float,
                startValue: List<MutableFloat>,
                endValue: List<MutableFloat>,
                result: MutableFloat,
            ) {
                result.value = Math.lerp(startValue[0].value, endValue[0].value, delta)
            }
        }

        val step = object : AnimationInterpolation(1) {
            override fun interpolateVector3f(
                delta: Float,
                startValue: List<Vector3fc>,
                endValue: List<Vector3fc>,
                result: Vector3f,
            ) {
                result.set(startValue[0])
            }

            override fun interpolateQuaternionf(
                delta: Float,
                startValue: List<Quaternionfc>,
                endValue: List<Quaternionfc>,
                result: Quaternionf,
            ) {
                result.set(startValue[0])
            }

            override fun interpolateFloat(
                delta: Float,
                startValue: List<MutableFloat>,
                endValue: List<MutableFloat>,
                result: MutableFloat,
            ) {
                result.value = startValue[0].value
            }
        }

        val cubicSpline = object : AnimationInterpolation(3) {
            override fun interpolateVector3f(
                delta: Float,
                startValue: List<Vector3fc>,
                endValue: List<Vector3fc>,
                result: Vector3f,
            ) {
                val t = delta
                val t2 = t * t
                val t3 = t2 * t

                // Hermite spline formula
                val h1 = 2f * t3 - 3f * t2 + 1f
                val h2 = t3 - 2f * t2 + t
                val h3 = -2f * t3 + 3f * t2
                val h4 = t3 - t2

                startValue[1].mul(h1, result)
                    .fma(h2, startValue[2], result)
                    .fma(h3, endValue[1], result)
                    .fma(h4, endValue[0], result)
            }

            private val tempQuaternion = Quaternionf()
            override fun interpolateQuaternionf(
                delta: Float,
                startValue: List<Quaternionfc>,
                endValue: List<Quaternionfc>,
                result: Quaternionf,
            ) {
                val t = delta
                val t2 = t * t
                val t3 = t2 * t

                val h1 = 2f * t3 - 3f * t2 + 1f
                val h2 = t3 - 2f * t2 + t
                val h3 = -2f * t3 + 3f * t2
                val h4 = t3 - t2

                startValue[1].mul(h1, result)
                startValue[2].mul(h2, tempQuaternion)
                result.add(tempQuaternion)
                endValue[1].mul(h3, tempQuaternion)
                result.add(tempQuaternion)
                endValue[0].mul(h4, tempQuaternion)
                result.add(tempQuaternion)
                result.normalize()
            }

            override fun interpolateFloat(
                delta: Float,
                startValue: List<MutableFloat>,
                endValue: List<MutableFloat>,
                result: MutableFloat,
            ) {

                val t = delta
                val t2 = t * t
                val t3 = t2 * t

                // Hermite spline formula
                val h1 = 2f * t3 - 3f * t2 + 1f
                val h2 = t3 - 2f * t2 + t
                val h3 = -2f * t3 + 3f * t2
                val h4 = t3 - t2

                result.value = Math.fma(
                    h1,
                    startValue[1].value,
                    Math.fma(h2, startValue[2].value, Math.fma(h3, endValue[1].value, h4 * endValue[0].value))
                )
            }
        }
    }
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
    ) = type.interpolateVector3f(
        delta = delta,
        startValue = startValue,
        endValue = endValue,
        result = result,
    )
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
    ) = type.interpolateQuaternionf(
        delta = delta,
        startValue = startValue,
        endValue = endValue,
        result = result,
    )
}

object FloatAnimationInterpolator : AnimationInterpolator<MutableFloat> {
    override fun set(
        value: List<MutableFloat>,
        result: MutableFloat,
    ) {
        result.value = value[0].value
    }

    override fun interpolate(
        delta: Float,
        type: AnimationInterpolation,
        startValue: List<MutableFloat>,
        endValue: List<MutableFloat>,
        result: MutableFloat,
    ) {
        type.interpolateFloat(
            delta = delta,
            startValue = startValue,
            endValue = endValue,
            result = result,
        )
    }
}