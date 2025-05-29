package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.CommandEncoder

fun CommandEncoder.clearBuffer(slice: GpuBufferSlice, clearType: CommandEncoderExt.ClearType) =
    (this as CommandEncoderExt).`armorStand$clearBuffer`(slice, clearType)

fun CommandEncoder.copyBuffer(target: GpuBufferSlice, source: GpuBufferSlice) =
    (this as CommandEncoderExt).`armorStand$copyBuffer`(target, source)
