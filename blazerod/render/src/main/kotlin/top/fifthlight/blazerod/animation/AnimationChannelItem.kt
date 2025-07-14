package top.fifthlight.blazerod.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.model.animation.AnimationChannel
import top.fifthlight.blazerod.model.resource.RenderExpression
import top.fifthlight.blazerod.model.resource.RenderExpressionGroup
import top.fifthlight.blazerod.model.util.MutableFloat

sealed class AnimationChannelItem<T : Any, D>(
    val channel: AnimationChannel<T, D>
) {
    abstract fun apply(instance: ModelInstance, time: Float)

    class TranslationItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Vector3f, Unit>,
    ) : AnimationChannelItem<Vector3f, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Translation) { "Unmatched animation channel: want translation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index, transformId) {
                channel.getKeyFrameData(time, translation)
            }
        }
    }

    class ScaleItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Vector3f, Unit>,
    ) : AnimationChannelItem<Vector3f, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Scale) { "Unmatched animation channel: want scale, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index, transformId) {
                channel.getKeyFrameData(time, scale)
            }
        }
    }

    class RotationItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Quaternionf, Unit>,
    ) : AnimationChannelItem<Quaternionf, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Rotation) { "Unmatched animation channel: want rotation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index, transformId) {
                channel.getKeyFrameData(time, rotation)
                rotation.normalize()
            }
        }
    }

    class MorphItem(
        private val primitiveIndex: Int,
        private val targetGroupIndex: Int,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.Morph.MorphData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.Morph.MorphData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, time: Float) {
            channel.getKeyFrameData(time, data)
            instance.setGroupWeight(primitiveIndex, targetGroupIndex, data.value)
        }
    }

    protected fun RenderExpression.apply(instance: ModelInstance, weight: Float) = bindings.forEach { binding ->
        when (binding) {
            is RenderExpression.Binding.MorphTarget -> {
                instance.setGroupWeight(binding.morphedPrimitiveIndex, binding.groupIndex, weight)
            }
        }
    }

    class ExpressionItem(
        val expression: RenderExpression,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.Expression.ExpressionData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.Expression.ExpressionData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, time: Float) {
            channel.getKeyFrameData(time, data)
            expression.apply(instance, data.value)
        }
    }

    class ExpressionGroupItem(
        val group: RenderExpressionGroup,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.Expression.ExpressionData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.Expression.ExpressionData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, time: Float) {
            channel.getKeyFrameData(time, data)
            for (item in group.items) {
                val expression = instance.scene.expressions[item.expressionIndex]
                expression.apply(instance, data.value * item.influence)
            }
        }
    }
}
