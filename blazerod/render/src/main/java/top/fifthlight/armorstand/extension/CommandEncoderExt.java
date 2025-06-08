package top.fifthlight.armorstand.extension;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

public interface CommandEncoderExt {
    enum ClearType {
        ZERO_FILLED,
        BYTE_ONE_FILLED,
        FLOAT_ONE_FILLED,
    }

    void armorStand$clearBuffer(GpuBufferSlice slice, ClearType clearType);

    void armorStand$copyBuffer(GpuBufferSlice target, GpuBufferSlice source);
}
