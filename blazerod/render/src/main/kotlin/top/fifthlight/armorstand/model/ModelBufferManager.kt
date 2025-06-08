package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.GpuBuffer
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.model.data.RenderTargetBuffer
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.SlottedGpuBuffer
import top.fifthlight.armorstand.util.mapToArray

object ModelBufferManager {
    private val bufferEntries = mutableMapOf<RenderScene, BufferEntry>()

    fun getEntry(scene: RenderScene) = bufferEntries.getOrPut(scene) { BufferEntry(scene) }

    class BufferEntry(private val scene: RenderScene, initialCapacity: Int = 1) : AbstractRefCount() {
        companion object {
            private val TYPE_ID = Identifier.of("armorstand", "buffer_entry")
            private const val BUFFER_USAGE_READ_WRITE = GpuBuffer.USAGE_COPY_SRC or GpuBuffer.USAGE_COPY_DST or GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_MAP_WRITE
        }

        override val typeId: Identifier
            get() = TYPE_ID

        val modelMatricesBuffers = SlottedGpuBuffer(
            initialCapacity = initialCapacity,
            itemSize = scene.primitiveNodes.size * 64,
            label = "Model matrices buffer",
            usage = GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER or BUFFER_USAGE_READ_WRITE,
        )

        val skinBuffers = scene.skins.mapToArray { skin ->
            SlottedGpuBuffer(
                initialCapacity = initialCapacity,
                itemSize = skin.jointSize * 64,
                label = "Skin buffer",
                usage = GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER or BUFFER_USAGE_READ_WRITE,
            )
        }

        val morphWeightBuffers = scene.morphedPrimitiveNodeIndices.mapToArray { morphedPrimitiveNodeIndex ->
            val node = scene.nodes[morphedPrimitiveNodeIndex] as RenderNode.Primitive
            val primitive = node.primitive
            val targets = primitive.targets!!
            val totalTargets =
                targets.position.targetsCount + targets.color.targetsCount + targets.texCoord.targetsCount
            SlottedGpuBuffer(
                initialCapacity = initialCapacity,
                itemSize = totalTargets * 4,
                label = "Morph target weight buffer",
                usage = GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER or BUFFER_USAGE_READ_WRITE,
            )
        }

        val morphIndicesBuffers = Array(scene.morphedPrimitiveNodeIndices.size) {
            SlottedGpuBuffer(
                initialCapacity = initialCapacity,
                itemSize = RenderTargetBuffer.INDICES_ENTRY_SIZE,
                label = "Morph target indices buffer",
                usage = GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER or BUFFER_USAGE_READ_WRITE,
            )
        }

        override fun onClosed() {
            modelMatricesBuffers.close()
            skinBuffers.forEach { it.close() }
            morphWeightBuffers.forEach { it.close() }
            morphIndicesBuffers.forEach { it.close() }
            require(bufferEntries.remove(scene, this)) { "Buffer entry not in manager!?" }
        }
    }
}