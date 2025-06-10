package top.fifthlight.blazerod.model.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.HumanoidTag
import top.fifthlight.blazerod.model.Node
import top.fifthlight.blazerod.model.NodeTransform

interface AnimationChannel<T: Any> {
    sealed class Type<T: Any> {
        // For VMD
        data object RelativeNodeTransformItem: Type<NodeTransform.Decomposed>()
        data object Translation: Type<Vector3f>()
        data object Scale: Type<Vector3f>()
        data object Rotation: Type<Quaternionf>()
    }

    val targetNode: Node?
    val targetNodeName: String?
    val targetHumanoidTag: HumanoidTag?
    val type: Type<T>
    val duration: Float
    fun getKeyFrameData(time: Float, result: T)
}

data class SimpleAnimationChannel<T : Any>(
    override val type: AnimationChannel.Type<T>,
    override val targetNode: Node?,
    override val targetNodeName: String?,
    override val targetHumanoidTag: HumanoidTag?,
    val indexer: AnimationKeyFrameIndexer,
    val interpolator: AnimationInterpolator<T>,
    val keyframeData: AnimationKeyFrameData<T>,
    val interpolation: AnimationInterpolation,
    val defaultValue: () -> T,
): AnimationChannel<T> {
    init {
        require(interpolation.elements == keyframeData.elements) { "Bad elements of keyframe data: ${keyframeData.elements}" }
    }

    override val duration: Float
        get() = indexer.lastTime

    private val indexResult = AnimationKeyFrameIndexer.FindResult()
    private val startValues = List(interpolation.elements) { defaultValue() }
    private val endValues = List(interpolation.elements) { defaultValue() }

    override fun getKeyFrameData(time: Float, result: T) {
        indexer.findKeyFrames(time, indexResult)
        if (indexResult.startFrame == indexResult.endFrame || indexResult.startTime > time || indexResult.endTime < time) {
            keyframeData.get(indexResult.startFrame, startValues)
            interpolator.set(startValues, result)
            return
        }
        val delta = (time - indexResult.startTime) / (indexResult.endTime - indexResult.startTime)
        keyframeData.get(indexResult.startFrame, startValues)
        keyframeData.get(indexResult.endFrame, endValues)
        interpolator.interpolate(delta, interpolation, startValues, endValues, result)
    }
}

@JvmName("Vector3fSimpleAnimationChannel")
fun SimpleAnimationChannel(
    type: AnimationChannel.Type<Vector3f>,
    targetNode: Node?,
    targetNodeName: String?,
    targetHumanoidTag: HumanoidTag?,
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<Vector3f>,
    interpolation: AnimationInterpolation,
): SimpleAnimationChannel<Vector3f> = SimpleAnimationChannel(
    type = type,
    targetNode = targetNode,
    targetNodeName = targetNodeName,
    targetHumanoidTag = targetHumanoidTag,
    indexer = indexer,
    interpolator = Vector3AnimationInterpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = ::Vector3f,
)

@JvmName("QuaternionfSimpleAnimationChannel")
fun SimpleAnimationChannel(
    type: AnimationChannel.Type<Quaternionf>,
    targetNode: Node?,
    targetNodeName: String?,
    targetHumanoidTag: HumanoidTag?,
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<Quaternionf>,
    interpolation: AnimationInterpolation,
): SimpleAnimationChannel<Quaternionf> = SimpleAnimationChannel(
    type = type,
    targetNode = targetNode,
    targetNodeName = targetNodeName,
    targetHumanoidTag = targetHumanoidTag,
    indexer = indexer,
    interpolator = QuaternionAnimationInterpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = ::Quaternionf,
)

@JvmName("NodeTransformSimpleAnimationChannel")
fun SimpleAnimationChannel(
    type: AnimationChannel.Type<NodeTransform.Decomposed>,
    targetNode: Node?,
    targetNodeName: String?,
    targetHumanoidTag: HumanoidTag?,
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<NodeTransform.Decomposed>,
    interpolation: AnimationInterpolation,
): SimpleAnimationChannel<NodeTransform.Decomposed> = SimpleAnimationChannel(
    type = type,
    targetNode = targetNode,
    targetNodeName = targetNodeName,
    targetHumanoidTag = targetHumanoidTag,
    indexer = indexer,
    interpolator = NodeTransformAnimationInterpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = NodeTransform::Decomposed,
)
