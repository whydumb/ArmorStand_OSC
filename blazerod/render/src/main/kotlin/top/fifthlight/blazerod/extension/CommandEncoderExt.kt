package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.CommandEncoder
import top.fifthlight.blazerod.systems.ComputePass
import java.util.function.Supplier

fun CommandEncoder.clearBuffer(slice: GpuBufferSlice, clearType: CommandEncoderExt.ClearType) =
    (this as CommandEncoderExt).`blazerod$clearBuffer`(slice, clearType)

fun CommandEncoder.createComputePass(label: Supplier<String>): ComputePass =
    (this as CommandEncoderExt).`blazerod$createComputePass`(label)

fun CommandEncoder.memoryBarrier(barriers: Int) =
    (this as CommandEncoderExt).`blazerod$memoryBarrier`(barriers)
