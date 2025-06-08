package top.fifthlight.blazerod.animation

import top.fifthlight.blazerod.model.ModelInstance
import kotlin.math.min

data class AnimationItem(
    val name: String? = null,
    val channels: List<AnimationChannelItem<*>>,
) {
    val duration: Float = channels.takeIf { it.isNotEmpty() }?.maxOf { it.channel.duration } ?: 0f

    fun apply(instance: ModelInstance, time: Float) = channels.forEach {
        it.apply(instance, min(time, duration))
    }
}