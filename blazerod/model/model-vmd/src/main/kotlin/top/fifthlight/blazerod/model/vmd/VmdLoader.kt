package top.fifthlight.blazerod.model.vmd

import it.unimi.dsi.fastutil.floats.FloatArrayList
import top.fifthlight.blazerod.model.HumanoidTag
import top.fifthlight.blazerod.model.ModelFileLoader
import top.fifthlight.blazerod.model.animation.*
import top.fifthlight.blazerod.model.util.readAll

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class VmdLoadException(message: String) : Exception(message)

object VmdLoader : ModelFileLoader {
    private val OLD_VMD_SIGNATURE = "Vocaloid Motion Data file".toByteArray()
    private val NEW_VMD_SIGNATURE = "Vocaloid Motion Data 0002".toByteArray()
    private val VMD_SIGNATURES = listOf(OLD_VMD_SIGNATURE, NEW_VMD_SIGNATURE)

    override val extensions = mapOf(
        "vmd" to setOf(ModelFileLoader.Ability.EXTERNAL_ANIMATION),
    )
    override val probeLength = VMD_SIGNATURES.maxOf { it.size }

    override fun probe(buffer: ByteBuffer) = VMD_SIGNATURES.any { signature ->
        val lastPosition = buffer.position()
        if (buffer.remaining() < signature.size) return false
        val signatureBytes = ByteArray(signature.size)
        buffer.get(signatureBytes, 0, signature.size)
        buffer.position(lastPosition)
        signatureBytes.contentEquals(signature)
    }

    private val SHIFT_JIS = Charset.forName("Shift-JIS")
    private val decoder = SHIFT_JIS.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    private fun loadString(buffer: ByteBuffer, maxLength: Int): String {
        val bytes = ByteBuffer.allocate(maxLength)
        bytes.put(buffer.slice(buffer.position(), maxLength))
        buffer.position(buffer.position() + maxLength)
        val nullIndex = (0 until maxLength).indexOfFirst { bytes.get(it) == 0.toByte() }
        val stringBytes = bytes.slice(0, nullIndex).order(ByteOrder.LITTLE_ENDIAN)
        return decoder.decode(stringBytes).toString()
    }

    private fun loadHeader(buffer: ByteBuffer): String {
        val signature = ByteArray(30)
        buffer.get(signature)
        fun compareSignature(target: ByteArray, source: ByteArray): Boolean {
            if (target.size < source.size) {
                return false
            }
            return (0 until source.size).all {
                source[it] == target[it]
            }
        }

        val isNewFormat = when {
            compareSignature(signature, OLD_VMD_SIGNATURE) -> false
            compareSignature(signature, NEW_VMD_SIGNATURE) -> true
            else -> throw VmdLoadException("Bad VMD file signature")
        }

        return if (isNewFormat) {
            loadString(buffer, 20)
        } else {
            loadString(buffer, 10)
        }
    }

    private class BoneChannel {
        val indexList = FloatArrayList()
        val translationList = FloatArrayList()
        val rotationList = FloatArrayList()
    }

    private const val FRAME_TIME_SEC = 1f / 24f

    private fun loadBone(buffer: ByteBuffer): List<AnimationChannel<*>> {
        val channels = mutableMapOf<String, BoneChannel>()
        val boneKeyframeCount = buffer.getInt()
        repeat(boneKeyframeCount) {
            val boneName = loadString(buffer, 15)
            val channel = channels.getOrPut(boneName, ::BoneChannel)

            val frameNumber = buffer.getInt()
            val frameTime = frameNumber * FRAME_TIME_SEC
            channel.indexList.add(frameTime)
            repeat(3) {
                channel.translationList.add(buffer.getFloat())
            }
            repeat(4) {
                channel.rotationList.add(buffer.getFloat())
            }

            // Skip curve data (not supported for now)
            buffer.position(buffer.position() + 64)
        }

        return channels.flatMap { (name, channel) ->
            val indexer = ListAnimationKeyFrameIndexer(channel.indexList)
            listOf(
                SimpleAnimationChannel(
                    type = AnimationChannel.Type.Translation,
                    targetNode = null,
                    targetNodeName = name,
                    targetHumanoidTag = HumanoidTag.fromPmxJapanese(name),
                    indexer = indexer,
                    keyframeData = AnimationKeyFrameData.ofVector3f(
                        values = channel.translationList,
                        elements = 1,
                    ),
                    interpolation = AnimationInterpolation.LINEAR,
                ),
                SimpleAnimationChannel(
                    type = AnimationChannel.Type.Rotation,
                    targetNode = null,
                    targetNodeName = name,
                    targetHumanoidTag = HumanoidTag.fromPmxJapanese(name),
                    indexer = indexer,
                    keyframeData = AnimationKeyFrameData.ofQuaternionf(
                        values = channel.rotationList,
                        elements = 1,
                    ),
                    interpolation = AnimationInterpolation.LINEAR,
                ),
            )
        }
    }

    private fun load(buffer: ByteBuffer): ModelFileLoader.LoadResult {
        val modelName = loadHeader(buffer)
        val boneChannels = loadBone(buffer)

        return ModelFileLoader.LoadResult(
            metadata = null,
            model = null,
            animations = listOf(Animation(channels = boneChannels)),
        )
    }

    override fun load(
        path: Path,
        basePath: Path
    ) = FileChannel.open(path, StandardOpenOption.READ).use { channel ->
        val fileSize = channel.size()
        val buffer = runCatching {
            channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize)
        }.getOrNull() ?: run {
            if (fileSize > 32 * 1024 * 1024) {
                throw VmdLoadException("VMD animation size too large: maximum allowed is 32M, current is $fileSize")
            }
            val fileSize = fileSize.toInt()
            val buffer = ByteBuffer.allocate(fileSize)
            channel.readAll(buffer)
            buffer.flip()
            buffer
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        load(buffer)
    }
}