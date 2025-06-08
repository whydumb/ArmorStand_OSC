package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.CommandEncoder
import top.fifthlight.blazerod.extension.CommandEncoderExt

fun CommandEncoder.clearBuffer(slice: GpuBufferSlice, clearType: CommandEncoderExt.ClearType) =
    (this as CommandEncoderExt).`blazerod$clearBuffer`(slice, clearType)

fun CommandEncoder.copyBuffer(target: GpuBufferSlice, source: GpuBufferSlice) =
    (this as CommandEncoderExt).`blazerod$copyBuffer`(target, source)
