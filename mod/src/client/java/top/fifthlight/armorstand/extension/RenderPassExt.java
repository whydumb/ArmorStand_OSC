package top.fifthlight.armorstand.extension;

import com.mojang.blaze3d.buffers.GpuBuffer;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.armorstand.render.GpuTextureBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;

public interface RenderPassExt {
    void armorStand$bindSampler(String name, GpuTextureBuffer textureBuffer);
    void armorStand$setUniform(String name, GpuBuffer buffer);
    void armorStand$setVertexBuffer(@NotNull VertexBuffer vertexBuffer);
    void armorStand$drawIndexedInstanced(int instances, int offset, int count);
    void armorStand$drawInstanced(int instances, int offset, int count);
}