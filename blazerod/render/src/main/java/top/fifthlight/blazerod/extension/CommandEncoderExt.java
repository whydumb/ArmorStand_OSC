package top.fifthlight.blazerod.extension;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import top.fifthlight.blazerod.systems.ComputePass;

import java.util.function.Supplier;

public interface CommandEncoderExt {
    enum ClearType {
        ZERO_FILLED,
        BYTE_ONE_FILLED,
        FLOAT_ONE_FILLED,
    }

    void blazerod$clearBuffer(GpuBufferSlice slice, ClearType clearType);

    ComputePass blazerod$createComputePass(Supplier<String> supplier);
}
