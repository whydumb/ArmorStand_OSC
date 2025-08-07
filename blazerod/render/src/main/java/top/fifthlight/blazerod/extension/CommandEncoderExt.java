package top.fifthlight.blazerod.extension;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import top.fifthlight.blazerod.systems.ComputePass;

import java.util.function.Supplier;

@SuppressWarnings("PointlessBitwiseExpression")
public interface CommandEncoderExt {
    int BARRIER_STORAGE_BUFFER_BIT = 1 << 0;
    int BARRIER_VERTEX_BUFFER_BIT = 1 << 1;
    int BARRIER_INDEX_BUFFER_BIT = 1 << 2;
    int BARRIER_TEXTURE_FETCH_BIT = 1 << 3;
    int BARRIER_UNIFORM_BIT = 1 << 4;

    enum ClearType {
        ZERO_FILLED,
        BYTE_ONE_FILLED,
        FLOAT_ONE_FILLED,
    }

    void blazerod$clearBuffer(GpuBufferSlice slice, ClearType clearType);

    ComputePass blazerod$createComputePass(Supplier<String> label);

    void blazerod$memoryBarrier(int barriers);
}
