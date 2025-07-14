package top.fifthlight.blazerod.model.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.HumanoidTag
import top.fifthlight.blazerod.model.Node
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.model.util.MutableFloat

interface AnimationChannel<T: Any, D> {
    sealed class Type<T: Any, D> {
        data class NodeData(
            val targetNode: Node?,
            val targetNodeName: String?,
            val targetHumanoidTag: HumanoidTag?,
        )

        data class TransformData(
            val node: NodeData,
            val transformId: TransformId,
        )

        data object Expression : Type<MutableFloat, Expression.ExpressionData>() {
            data class ExpressionData(
                val name: String? = null,
                val tag: top.fifthlight.blazerod.model.Expression.Tag? = null,
            )
        }

        data object Translation : Type<Vector3f, TransformData>()
        data object Scale : Type<Vector3f, TransformData>()
        data object Rotation : Type<Quaternionf, TransformData>()

        data object Morph: Type<MutableFloat, Morph.MorphData>() {
            data class MorphData(
                val nodeData: NodeData,
                val targetMorphGroupIndex: Int,
            )
        }
    }

    val type: Type<T, D>
    val data: D
    val duration: Float
    fun getKeyFrameData(time: Float, result: T)
}

data class SimpleAnimationChannel<T : Any, D>(
    override val type: AnimationChannel.Type<T, D>,
    override val data: D,
    val indexer: AnimationKeyFrameIndexer,
    val interpolator: AnimationInterpolator<T>,
    val keyframeData: AnimationKeyFrameData<T>,
    val interpolation: AnimationInterpolation,
    val defaultValue: () -> T,
): AnimationChannel<T, D> {
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
fun <D> SimpleAnimationChannel(
    type: AnimationChannel.Type<Vector3f, D>,
    data: D,
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<Vector3f>,
    interpolation: AnimationInterpolation,
): SimpleAnimationChannel<Vector3f, D> = SimpleAnimationChannel(
    type = type,
    data = data,
    indexer = indexer,
    interpolator = Vector3AnimationInterpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = ::Vector3f,
)

@JvmName("QuaternionfSimpleAnimationChannel")
fun <D> SimpleAnimationChannel(
    type: AnimationChannel.Type<Quaternionf, D>,
    data: D,
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<Quaternionf>,
    interpolation: AnimationInterpolation,
): SimpleAnimationChannel<Quaternionf, D> = SimpleAnimationChannel(
    type = type,
    data = data,
    indexer = indexer,
    interpolator = QuaternionAnimationInterpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = ::Quaternionf,
)

@JvmName("FloatSimpleAnimationChannel")
fun <D> SimpleAnimationChannel(
    type: AnimationChannel.Type<MutableFloat, D>,
    data: D,
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<MutableFloat>,
    interpolation: AnimationInterpolation,
): SimpleAnimationChannel<MutableFloat, D> = SimpleAnimationChannel(
    type = type,
    data = data,
    indexer = indexer,
    interpolator = FloatAnimationInterpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = ::MutableFloat,
)
