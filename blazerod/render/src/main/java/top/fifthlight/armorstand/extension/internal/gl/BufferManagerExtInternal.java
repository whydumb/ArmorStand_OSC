package top.fifthlight.armorstand.extension.internal.gl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import top.fifthlight.armorstand.extension.CommandEncoderExt.ClearType;

public interface BufferManagerExtInternal {
    boolean armorStand$isGlClearBufferObjectEnabled();
    void armorStand$clearBufferData(int buffer, int offset, int size, ClearType clearType);
    void armorStand$copyBuffer(int target, int source, int targetOffset, int sourceOffset, int size);
}