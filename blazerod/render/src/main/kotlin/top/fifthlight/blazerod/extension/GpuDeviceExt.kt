package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.GpuDevice
import java.nio.ByteBuffer
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

fun GpuDevice.createBuffer(
    labelGetter: Supplier<String>?,
    usage: Int,
    extraUsage: Int,
    size: Int,
): GpuBuffer = (this as GpuDeviceExt).`blazerod$createBuffer`(labelGetter, usage, extraUsage, size)

fun GpuDevice.createBuffer(
    labelGetter: Supplier<String>?,
    usage: Int,
    extraUsage: Int,
    data: ByteBuffer,
): GpuBuffer = (this as GpuDeviceExt).`blazerod$createBuffer`(labelGetter, usage, extraUsage, data)

val GpuDevice.shaderDataPool
    get() = (this as GpuDeviceExt).`blazerod$getShaderDataPool`()

val GpuDevice.supportSsbo
    get() = (this as GpuDeviceExt).`blazerod$supportSsbo`()