package top.fifthlight.blazerod.extension.internal;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.blazerod.extension.RenderPassExt;
import top.fifthlight.blazerod.render.VertexBuffer;

import java.util.Map;

public interface RenderPassExtInternal extends RenderPassExt {
    @Nullable
    VertexBuffer blazerod$getVertexBuffer();

    @NotNull
    Map<String, GpuBufferSlice> blazerod$getStorageBuffers();
}
