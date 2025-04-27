package top.fifthlight.armorstand.extension.internal.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.armorstand.extension.RenderPassExt;
import top.fifthlight.armorstand.render.GpuTextureBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;

import java.util.HashMap;

public interface GlRenderPassImplExtInternal extends RenderPassExt {
    HashMap<String, GpuTextureBuffer> armorStand$getBufferSamplerUniforms();

    @Nullable
    VertexBuffer armorStand$getVertexBuffer();

    @NotNull
    HashMap<String, GpuBuffer> armorStand$getUniformBuffers();
}
