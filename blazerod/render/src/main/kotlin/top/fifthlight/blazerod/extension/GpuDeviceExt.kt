package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode
import top.fifthlight.blazerod.render.VertexBuffer
import top.fifthlight.blazerod.render.VertexBuffer.VertexElement
import java.util.function.Supplier

fun GpuDevice.createBuffer(
    labelGetter: Supplier<String>?,
    usage: Int,
    size: Int,
    clearType: CommandEncoderExt.ClearType
): GpuBuffer = createBuffer(labelGetter, usage, size).also { buffer ->
    val commandEncoder = createCommandEncoder()
    commandEncoder.clearBuffer(buffer.slice(), clearType)
}

fun GpuDevice.createVertexBuffer(
    mode: DrawMode?,
    elements: List<VertexElement>,
    verticesCount: Int
): VertexBuffer =
    (this as GpuDeviceExt).`blazerod$createVertexBuffer`(mode, elements, verticesCount)
