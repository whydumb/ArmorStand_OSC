package top.fifthlight.blazerod.model.animation

import org.joml.Quaternionf
import org.joml.Quaternionfc
import org.joml.Vector3f
import org.joml.Vector3fc
import top.fifthlight.blazerod.model.NodeTransform

abstract class AnimationInterpolation(val elements: Int) {
    init {
        require(elements in 1 .. MAX_ELEMENTS) { "Bad elements count: should be in [1, $MAX_ELEMENTS]" }
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

object NodeTransformAnimationInterpolator : AnimationInterpolator<NodeTransform.Decomposed> {
    override fun set(
        value: List<NodeTransform.Decomposed>,
        result: NodeTransform.Decomposed,
    ) {
        result.set(value[0])
    }

    private val vec3ElementsStart = List(AnimationInterpolation.MAX_ELEMENTS) { Vector3f() }
    private val vec3ElementsEnd = List(AnimationInterpolation.MAX_ELEMENTS) { Vector3f() }
    private val quatElementsStart = List(AnimationInterpolation.MAX_ELEMENTS) { Quaternionf() }
    private val quatElementsEnd = List(AnimationInterpolation.MAX_ELEMENTS) { Quaternionf() }

    override fun interpolate(
        delta: Float,
        type: AnimationInterpolation,
        startValue: List<NodeTransform.Decomposed>,
        endValue: List<NodeTransform.Decomposed>,
        result: NodeTransform.Decomposed,
    ) = with(result) {
        startValue.forEachIndexed { index, value ->
            vec3ElementsStart[index].set(value.translation)
        }
        endValue.forEachIndexed { index, value ->
            vec3ElementsEnd[index].set(value.translation)
        }
        type.interpolateVector3f(
            delta = delta,
            startValue = vec3ElementsStart,
            endValue = vec3ElementsEnd,
            result = translation,
        )

        startValue.forEachIndexed { index, value ->
            vec3ElementsStart[index].set(value.scale)
        }
        endValue.forEachIndexed { index, value ->
            vec3ElementsEnd[index].set(value.scale)
        }
        type.interpolateVector3f(
            delta = delta,
            startValue = vec3ElementsStart,
            endValue = vec3ElementsEnd,
            result = scale,
        )

        startValue.forEachIndexed { index, value ->
            quatElementsStart[index].set(value.rotation)
        }
        endValue.forEachIndexed { index, value ->
            quatElementsEnd[index].set(value.rotation)
        }
        type.interpolateQuaternionf(
            delta,
            startValue = quatElementsStart,
            endValue = quatElementsEnd,
            result = rotation,
        )
    }
}
