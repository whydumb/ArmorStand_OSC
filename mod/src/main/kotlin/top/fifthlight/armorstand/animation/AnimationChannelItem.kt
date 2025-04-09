package top.fifthlight.armorstand.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.armorstand.model.ModelInstance

sealed class AnimationChannelItem<T: Any>(val channel: Channel<T>) {
    abstract fun apply(instance: ModelInstance, time: Float)

    class TranslationItem(
        private val index: Int,
        channel: Channel<Vector3f>,
    ): AnimationChannelItem<Vector3f>(channel) {
        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index) {
                channel.getKeyFrameData(time, translation)
            }
        }
    }

    class ScaleItem(
        private val index: Int,
        channel: Channel<Vector3f>,
    ): AnimationChannelItem<Vector3f>(channel) {
        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index) {
                channel.getKeyFrameData(time, scale)
            }
        }
    }

    class RotationItem(
        private val index: Int,
        channel: Channel<Quaternionf>,
    ): AnimationChannelItem<Quaternionf>(channel) {
        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index) {
                channel.getKeyFrameData(time, rotation)
                rotation.normalize()
            }
        }
    }
}
