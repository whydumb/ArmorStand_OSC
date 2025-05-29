package top.fifthlight.armorstand.model.data

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.model.RenderPrimitive
import top.fifthlight.armorstand.util.SlottedGpuBuffer

class RenderTargetBuffer private constructor(
    positionTargets: Int,
    colorTargets: Int,
    texCoordTargets: Int,
    // MorphWeights
    val weightsSlot: SlottedGpuBuffer.Slot,
    // MorphTargetIndices
    val indicesSlot: SlottedGpuBuffer.Slot,
) : AutoCloseable {
    constructor(
        targets: RenderPrimitive.Targets,
        weightsSlot: SlottedGpuBuffer.Slot,
        indicesSlot: SlottedGpuBuffer.Slot,
    ) : this(
        positionTargets = targets.position.targetsCount,
        colorTargets = targets.color.targetsCount,
        texCoordTargets = targets.texCoord.targetsCount,
        weightsSlot = weightsSlot,
        indicesSlot = indicesSlot,
    )

    companion object {
        const val INDICES_ENTRY_SIZE = 12 + 12 * ArmorStandClient.MAX_ENABLED_MORPH_TARGETS
    }

    init {
        val totalTargets = positionTargets + colorTargets + texCoordTargets
        require(weightsSlot.size == 4 * totalTargets) { "Weights slot size mismatch: want ${4 * totalTargets}, but got ${weightsSlot.size}" }
        require(indicesSlot.size == INDICES_ENTRY_SIZE) { "Indices slot size not match: want $INDICES_ENTRY_SIZE, but got ${indicesSlot.size}" }
    }

    interface WeightChannel {
        fun uploadIndices()
        operator fun set(index: Int, weight: Float)
    }

    private inner class WeightChannelImpl private constructor(
        private val channelByteOffset: Int,
        private val weightByteOffset: Int,
        private val enabledIndices: IntSet,
        private val targetsSize: Int,
    ) : WeightChannel {
        constructor(
            channelIndex: Int,
            weightOffset: Int,
            targetsSize: Int,
        ) : this(
            channelByteOffset = channelIndex * 4,
            weightByteOffset = weightOffset * 4,
            enabledIndices = IntOpenHashSet(targetsSize),
            targetsSize = targetsSize,
        )

        private var indicesDirty: Boolean = false

        override fun uploadIndices() {
            if (!indicesDirty) {
                return
            }
            indicesDirty = false
            indicesSlot.edit {
                val effectiveEnabledTargets =
                    enabledIndices.size.coerceAtMost(ArmorStandClient.MAX_ENABLED_MORPH_TARGETS)
                putInt(channelByteOffset, effectiveEnabledTargets)
                val iterator = enabledIndices.intIterator()
                var index = 0
                while (iterator.hasNext() && index < effectiveEnabledTargets) {
                    putInt(12 + index * 12 + channelByteOffset, iterator.nextInt())
                    index++
                }
            }
        }

        override operator fun set(index: Int, weight: Float) {
            check(index in 0 until targetsSize) { "Invalid target index: $index, should be in [0, $targetsSize)" }
            weightsSlot.edit {
                putFloat(weightByteOffset + index * 4, weight)
            }
            if (weight == 0f) {
                if (enabledIndices.remove(index)) {
                    indicesDirty = true
                }
            } else {
                if (enabledIndices.add(index)) {
                    indicesDirty = true
                }
            }
        }
    }

    val positionChannel: WeightChannel = WeightChannelImpl(
        channelIndex = 0,
        weightOffset = 0,
        targetsSize = positionTargets,
    )

    val colorChannel: WeightChannel = WeightChannelImpl(
        channelIndex = 1,
        weightOffset = positionTargets,
        targetsSize = colorTargets,
    )

    val texCoordChannel: WeightChannel = WeightChannelImpl(
        channelIndex = 2,
        weightOffset = positionTargets + colorTargets,
        targetsSize = texCoordTargets,
    )

    fun uploadIndices() {
        positionChannel.uploadIndices()
        colorChannel.uploadIndices()
        texCoordChannel.uploadIndices()
    }

    override fun close() {
        weightsSlot.close()
        indicesSlot.close()
    }
}