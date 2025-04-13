package top.fifthlight.armorstand.helper;

import top.fifthlight.armorstand.render.GpuTextureBuffer;

import java.util.HashMap;

public interface RenderPassWithTextureBuffer {
    HashMap<String, GpuTextureBuffer> armorStand$getBufferSamplerUniforms();

    void armorStand$bindSampler(String name, GpuTextureBuffer textureBuffer);
}
