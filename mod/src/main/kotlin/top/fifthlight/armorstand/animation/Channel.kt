package top.fifthlight.armorstand.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.renderer.model.AnimationInterpolation

data class Channel<T: Any>(
    val indexer: KeyFrameIndexer,
    val interpolator: Interpolator<T>,
    val keyframeData: KeyFrameData<T>,
    val interpolation: AnimationInterpolation,
    val defaultValue: () -> T,
) {
    val duration: Float
        get() = indexer.lastTime

    private val indexResult = KeyFrameIndexer.FindResult()
    private val startValues = List(interpolation.outputMultiplier) { defaultValue() }
    private val endValues = List(interpolation.outputMultiplier) { defaultValue() }

    fun getKeyFrameData(time: Float, result: T) {
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

@JvmName("Vector3fChannel")
fun Channel(
    indexer: KeyFrameIndexer,
    keyframeData: KeyFrameData<Vector3f>,
    interpolation: AnimationInterpolation,
): Channel<Vector3f> = Channel(
    indexer = indexer,
    interpolator = Vector3Interpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = ::Vector3f,
)

@JvmName("QuaternionfChannel")
fun Channel(
    indexer: KeyFrameIndexer,
    keyframeData: KeyFrameData<Quaternionf>,
    interpolation: AnimationInterpolation,
): Channel<Quaternionf> = Channel(
    indexer = indexer,
    interpolator = QuaternionInterpolator,
    keyframeData = keyframeData,
    interpolation = interpolation,
    defaultValue = ::Quaternionf,
)