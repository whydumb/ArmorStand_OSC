package top.fifthlight.blazerod.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.animation.AnimationChannel

sealed class AnimationChannelItem<T : Any>(
    val channel: AnimationChannel<T>
) {
    abstract fun apply(instance: ModelInstance, time: Float)

    class TranslationItem(
        private val index: Int,
        channel: AnimationChannel<Vector3f>,
    ) : AnimationChannelItem<Vector3f>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Translation) { "Unmatched animation channel: want translation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index) {
                channel.getKeyFrameData(time, translation)
            }
        }
    }

    class ScaleItem(
        private val index: Int,
        channel: AnimationChannel<Vector3f>,
    ) : AnimationChannelItem<Vector3f>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Scale) { "Unmatched animation channel: want scale, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index) {
                channel.getKeyFrameData(time, scale)
            }
        }
    }

    class RotationItem(
        private val index: Int,
        channel: AnimationChannel<Quaternionf>,
    ) : AnimationChannelItem<Quaternionf>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Rotation) { "Unmatched animation channel: want rotation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, time: Float) {
            instance.setTransformDecomposed(index) {
                channel.getKeyFrameData(time, rotation)
                rotation.normalize()
            }
        }
    }
}
