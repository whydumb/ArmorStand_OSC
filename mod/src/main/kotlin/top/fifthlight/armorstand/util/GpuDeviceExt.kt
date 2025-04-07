package top.fifthlight.armorstand.util

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode
import top.fifthlight.armorstand.helper.GpuDeviceExt
import top.fifthlight.armorstand.helper.GpuDeviceExt.FillType
import top.fifthlight.armorstand.render.GpuTextureBuffer
import top.fifthlight.armorstand.render.TextureBufferFormat
import top.fifthlight.armorstand.render.VertexBuffer
import top.fifthlight.armorstand.render.VertexBuffer.VertexElement
import java.util.function.Supplier

fun GpuDevice.createBuffer(
    labelGetter: Supplier<String?>?,
    type: BufferType?,
    usage: BufferUsage?,
    size: Int,
    fillType: FillType?
): GpuBuffer =
    (this as GpuDeviceExt).`armorStand$createBuffer`(labelGetter, type, usage, size, fillType)

fun GpuDevice.createVertexBuffer(
    mode: DrawMode?,
    elements: List<VertexElement>,
    verticesCount: Int
): VertexBuffer =
    (this as GpuDeviceExt).`armorStand$createVertexBuffer`(mode, elements, verticesCount)

fun GpuDevice.createTextureBuffer(label: String?, format: TextureBufferFormat, buffer: GpuBuffer): GpuTextureBuffer =
    (this as GpuDeviceExt).`armorStand$createTextureBuffer`(label, format, buffer)