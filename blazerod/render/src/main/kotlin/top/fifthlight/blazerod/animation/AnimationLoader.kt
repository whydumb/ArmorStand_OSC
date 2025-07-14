package top.fifthlight.blazerod.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.animation.AnimationChannelItem.*
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.model.animation.Animation
import top.fifthlight.blazerod.model.animation.AnimationChannel
import top.fifthlight.blazerod.model.util.MutableFloat

object AnimationLoader {
    fun load(
        scene: RenderScene,
        animation: Animation,
    ): AnimationItem {
        fun AnimationChannel.Type.NodeData.findTargetTransformIndex(): Int? {
            val node = targetNode?.id.let { nodeId -> scene.nodeIdMap[nodeId] }
                ?: targetNodeName?.let { name -> scene.nodeNameMap[name] }
                ?: targetHumanoidTag?.let { humanoid ->
                    scene.humanoidTagMap[humanoid]
                }
            return node?.nodeIndex
        }

        @Suppress("UNCHECKED_CAST")
        fun mapAnimationChannel(channel: AnimationChannel<*, *>): AnimationChannelItem<*, *>? {
            return when (channel.type) {
                AnimationChannel.Type.Translation -> {
                    val data = channel.data as AnimationChannel.Type.TransformData
                    TranslationItem(
                        index = data.node.findTargetTransformIndex() ?: return null,
                        transformId = data.transformId,
                        channel = channel as AnimationChannel<Vector3f, Unit>,
                    )
                }

                AnimationChannel.Type.Scale -> {
                    val data = channel.data as AnimationChannel.Type.TransformData
                    ScaleItem(
                        index = data.node.findTargetTransformIndex() ?: return null,
                        transformId = data.transformId,
                        channel = channel as AnimationChannel<Vector3f, Unit>,
                    )
                }

                AnimationChannel.Type.Rotation -> {
                    val data = channel.data as AnimationChannel.Type.TransformData
                    RotationItem(
                        index = data.node.findTargetTransformIndex() ?: return null,
                        transformId = data.transformId,
                        channel = channel as AnimationChannel<Quaternionf, Unit>,
                    )
                }

                AnimationChannel.Type.Morph -> {
                    val data = channel.data as AnimationChannel.Type.Morph.MorphData
                    MorphItem(
                        primitiveIndex = data.nodeData.findTargetTransformIndex() ?: return null,
                        targetGroupIndex = data.targetMorphGroupIndex,
                        channel = channel as AnimationChannel<MutableFloat, AnimationChannel.Type.Morph.MorphData>,
                    )
                }

                AnimationChannel.Type.Expression -> {
                    val channel =
                        channel as AnimationChannel<MutableFloat, AnimationChannel.Type.Expression.ExpressionData>
                    val data = channel.data
                    scene.expressions.firstOrNull { (it.name != null && it.name == data.name) || (it.tag != null && it.tag == data.tag) }
                        ?.let { ExpressionItem(it, channel) }
                        ?: scene.expressionGroups.firstOrNull { (it.name != null && it.name == data.name) || (it.tag != null && it.tag == data.tag) }
                            ?.let { ExpressionGroupItem(it, channel) }
                }
            }
        }

        return AnimationItem(
            name = animation.name,
            channels = animation.channels.mapNotNull(::mapAnimationChannel)
        )
    }
}