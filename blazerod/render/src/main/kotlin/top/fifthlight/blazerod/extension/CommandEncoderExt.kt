package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.CommandEncoder

fun CommandEncoder.clearBuffer(slice: GpuBufferSlice, clearType: CommandEncoderExt.ClearType) =
    (this as CommandEncoderExt).`blazerod$clearBuffer`(slice, clearType)
