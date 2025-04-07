package top.fifthlight.armorstand.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.gl.RenderPassImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.helper.RenderPassWithTextureBuffer;
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer;
import top.fifthlight.armorstand.render.GpuTextureBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;

import java.util.HashMap;
import java.util.Set;

@Mixin(RenderPassImpl.class)
public abstract class RenderPassImplMixin implements RenderPassWithVertexBuffer, RenderPassWithTextureBuffer {
    @Shadow
    @Final
    public GpuBuffer[] vertexBuffers;

    @Shadow @Final protected Set<String> setSamplers;
    @Unique
    VertexBuffer vertexBuffer;

    @Unique
    HashMap<String, GpuTextureBuffer> bufferSamplerUniforms = new HashMap<>();

    @Override
    @Nullable
    public VertexBuffer armorStand$getVertexBuffer() {
        return vertexBuffer;
    }

    @Override
    public void armorStand$setVertexBuffer(@NotNull VertexBuffer vertexBuffer) {
        if (this.vertexBuffers[0] != null) {
            throw new IllegalStateException("Can't set ArmorStand VertexBuffer to a RenderPass already having a Blaze3D vertex buffer");
        }
        this.vertexBuffer = vertexBuffer;
    }

    @Inject(method = "setVertexBuffer", at = @At("HEAD"))
    public void onSetVertexBuffer(int i, GpuBuffer gpuBuffer, CallbackInfo ci) {
        if (this.vertexBuffer != null) {
            throw new IllegalStateException("Can't set Blaze3d VertexBuffer to a RenderPass already having an ArmorStand vertex buffer");
        }
    }

    @Override
    public HashMap<String, GpuTextureBuffer> armorStand$getBufferSamplerUniforms() {
        return bufferSamplerUniforms;
    }

    @Override
    public void armorStand$bindSampler(String name, GpuTextureBuffer textureBuffer) {
        this.bufferSamplerUniforms.put(name, textureBuffer);
        this.setSamplers.add(name);
    }
}
