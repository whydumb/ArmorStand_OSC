package top.fifthlight.blazerod.extension;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

public interface CommandEncoderExt {
    enum ClearType {
        ZERO_FILLED,
        BYTE_ONE_FILLED,
        FLOAT_ONE_FILLED,
    }

    void blazerod$clearBuffer(GpuBufferSlice slice, ClearType clearType);
}
