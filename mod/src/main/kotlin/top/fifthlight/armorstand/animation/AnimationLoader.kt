package top.fifthlight.armorstand.animation

import com.mojang.logging.LogUtils
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.renderer.model.Accessor
import top.fifthlight.renderer.model.Animation
import top.fifthlight.renderer.model.AnimationChannel
import kotlin.math.max

object AnimationLoader {
    private val LOGGER = LogUtils.getLogger()

    private fun loadSignedByteAccessor(accessor: Accessor) = FloatArray(accessor.count * accessor.type.components).also { array ->
        require(accessor.componentType == Accessor.ComponentType.BYTE) { "Loaded bad accessor: should be BYTE, but got ${accessor.componentType}" }
        var index = 0
        accessor.read { buffer ->
            repeat(accessor.type.components) {
                array[index++] = max(buffer.get() / 127f, -1f)
            }
        }
    }

    private fun loadUnsignedByteAccessor(accessor: Accessor) = FloatArray(accessor.count * accessor.type.components).also { array ->
        require(accessor.componentType == Accessor.ComponentType.UNSIGNED_BYTE) { "Loaded bad accessor: should be UNSIGNED_BYTE, but got ${accessor.componentType}" }
        var index = 0
        accessor.read { buffer ->
            repeat(accessor.type.components) {
                array[index++] = buffer.get() / 255f
            }
        }
    }

    private fun loadSignedShortAccessor(accessor: Accessor) = FloatArray(accessor.count * accessor.type.components).also { array ->
        require(accessor.componentType == Accessor.ComponentType.SHORT) { "Loaded bad accessor: should be SHORT, but got ${accessor.componentType}" }
        var index = 0
        accessor.read { buffer ->
            repeat(accessor.type.components) {
                array[index++] = max(buffer.get() / 32767f, -1f)
            }
        }
    }

    private fun loadUnsignedShortAccessor(accessor: Accessor) = FloatArray(accessor.count * accessor.type.components).also { array ->
        require(accessor.componentType == Accessor.ComponentType.UNSIGNED_SHORT) { "Loaded bad accessor: should be UNSIGNED_SHORT, but got ${accessor.componentType}" }
        var index = 0
        accessor.read { buffer ->
            repeat(accessor.type.components) {
                array[index++] = buffer.get() / 65535f
            }
        }
    }

    private fun loadFloatAccessor(accessor: Accessor) = FloatArray(accessor.count * accessor.type.components).also { array ->
        require(accessor.componentType == Accessor.ComponentType.FLOAT) { "Loaded bad accessor: should be FLOAT, but got ${accessor.componentType}" }
        var index = 0
        accessor.read { buffer ->
            repeat(accessor.type.components) {
                array[index++] = buffer.getFloat()
            }
        }
    }

    private fun loadIndexer(accessor: Accessor): KeyFrameIndexer =
        FloatArrayKeyFrameIndexer(times = loadFloatAccessor(accessor))

    fun load(
        scene: RenderScene,
        animation: Animation,
    ): AnimationItem {
        fun findTargetTransformIndex(channel: AnimationChannel): Int? =
            channel.targetNode?.let { nodeId -> scene.nodeIdTransformMap.getInt(nodeId).takeIf { it >= 0 } }
                ?: channel.targetNodeName?.let { name -> scene.nodeNameTransformMap.getInt(name).takeIf { it >= 0 } }
                ?: channel.targetHumanoid?.let { humanoid ->
                    scene.humanoidTagTransformMap.getInt(humanoid).takeIf { it >= 0 }
                }

        fun mapAnimationChannel(channel: AnimationChannel): AnimationChannelItem<*>? {
            val index = findTargetTransformIndex(channel) ?: return null
            return when (channel.targetPath) {
                AnimationChannel.Path.TRANSLATION -> AnimationChannelItem.TranslationItem(
                    index = index,
                    channel = Channel(
                        indexer = loadIndexer(channel.sampler.input),
                        keyframeData = Vector3KeyFrameData(
                            when (channel.sampler.output.componentType) {
                                Accessor.ComponentType.BYTE -> loadSignedByteAccessor(channel.sampler.output)
                                Accessor.ComponentType.UNSIGNED_BYTE -> loadUnsignedByteAccessor(channel.sampler.output)
                                Accessor.ComponentType.SHORT -> loadSignedShortAccessor(channel.sampler.output)
                                Accessor.ComponentType.UNSIGNED_SHORT -> loadUnsignedShortAccessor(channel.sampler.output)
                                Accessor.ComponentType.FLOAT -> loadFloatAccessor(channel.sampler.output)
                                else -> error("Bad translation component: ${channel.sampler.output.componentType}")
                            }
                        ),
                        interpolation = channel.sampler.interpolation,
                    )
                )

                AnimationChannel.Path.SCALE -> AnimationChannelItem.ScaleItem(
                    index = index,
                    channel = Channel(
                        indexer = loadIndexer(channel.sampler.input),
                        keyframeData = Vector3KeyFrameData(
                            when (channel.sampler.output.componentType) {
                                Accessor.ComponentType.FLOAT -> loadFloatAccessor(channel.sampler.output)
                                else -> error("Bad scale component: should be float, but got ${channel.sampler.output.componentType}")
                            }
                        ),
                        interpolation = channel.sampler.interpolation,
                    )
                )

                AnimationChannel.Path.ROTATION -> AnimationChannelItem.RotationItem(
                    index = index,
                    channel = Channel(
                        indexer = loadIndexer(channel.sampler.input),
                        keyframeData = QuaternionKeyFrameData(
                            when (channel.sampler.output.componentType) {
                                Accessor.ComponentType.BYTE -> loadSignedByteAccessor(channel.sampler.output)
                                Accessor.ComponentType.UNSIGNED_BYTE -> loadUnsignedByteAccessor(channel.sampler.output)
                                Accessor.ComponentType.SHORT -> loadSignedShortAccessor(channel.sampler.output)
                                Accessor.ComponentType.UNSIGNED_SHORT -> loadUnsignedShortAccessor(channel.sampler.output)
                                Accessor.ComponentType.FLOAT -> loadFloatAccessor(channel.sampler.output)
                                else -> error("Bad rotation component: ${channel.sampler.output.componentType}")
                            }
                        ),
                        interpolation = channel.sampler.interpolation,
                    )
                )

                AnimationChannel.Path.WEIGHTS -> {
                    LOGGER.warn("Morph weight animation is not supported for now!")
                    null
                }
            }
        }

        return AnimationItem(
            name = animation.name,
            channels = animation.channels.mapNotNull(::mapAnimationChannel)
        )
    }
}