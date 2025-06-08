package top.fifthlight.blazerod.model.uniform

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.RenderSystem
import top.fifthlight.blazerod.std140.Std140Layout
import top.fifthlight.blazerod.util.Pool

abstract class UniformBuffer<T: UniformBuffer<T, L>, L: Std140Layout<L>>(
    private val buffer: GpuBuffer,
): AutoCloseable {
    abstract val pool: Pool<T>
    protected var released = false

    constructor(layout: L) : this(
        buffer = RenderSystem.getDevice().createBuffer({"Uniform Buffer"}, GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_MAP_WRITE, layout.totalSize)
    )

    val slice: GpuBufferSlice = buffer.slice()

    abstract val layout: L

    init {
        require(layout.totalSize <= 65536) { "Uniform size too big: ${layout.totalSize}, maximum is 65536" }
    }

    fun write(block: L.() -> Unit) {
        require(!released) { "Attempt to write a closed uniform buffer" }
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()
        commandEncoder.mapBuffer(buffer, false, true).use { mappedView ->
            layout.withBuffer(mappedView.data(), block)
        }
    }

    override fun close() {
        if (released) {
            return
        }
        released = true
        @Suppress("UNCHECKED_CAST")
        pool.release(this as T)
    }
}