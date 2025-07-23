package top.fifthlight.blazerod.extension;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.render.VertexBuffer;

public interface RenderPassExt {
    void blazerod$setVertexBuffer(@NotNull VertexBuffer vertexBuffer);

    void blazerod$setStorageBuffer(@NotNull String name, GpuBufferSlice buffer);
    void blazerod$draw(int baseVertex, int firstIndex, int count, int instanceCount);
}