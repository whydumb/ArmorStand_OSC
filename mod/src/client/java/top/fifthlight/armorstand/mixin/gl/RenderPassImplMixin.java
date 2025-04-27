package top.fifthlight.armorstand.mixin.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.GlResourceManager;
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
import top.fifthlight.armorstand.extension.internal.gl.GlRenderPassImplExtInternal;
import top.fifthlight.armorstand.extension.internal.gl.GlResourceManagerExtInternal;
import top.fifthlight.armorstand.render.GpuTextureBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;

import java.util.HashMap;
import java.util.Set;

@Mixin(RenderPassImpl.class)
public abstract class RenderPassImplMixin implements GlRenderPassImplExtInternal {
    @Shadow
    @Final
    public GpuBuffer[] vertexBuffers;

    @Shadow
    @Final
    public Set<String> setSamplers;

    @Shadow
    private boolean closed;

    @Shadow
    @Final
    private GlResourceManager resourceManager;

    @Shadow
    protected VertexFormat.IndexType indexType;

    @Unique
    VertexBuffer vertexBuffer;

    @Unique
    private final HashMap<String, GpuTextureBuffer> bufferSamplerUniforms = new HashMap<>();

    @Unique
    private final HashMap<String, GpuBuffer> uniformBlocks = new HashMap<>();

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

    @Override
    public HashMap<String, GpuTextureBuffer> armorStand$getBufferSamplerUniforms() {
        return bufferSamplerUniforms;
    }

    @Override
    public void armorStand$bindSampler(String name, GpuTextureBuffer textureBuffer) {
        this.bufferSamplerUniforms.put(name, textureBuffer);
        this.setSamplers.add(name);
    }

    @Override
    @NotNull
    public HashMap<String, GpuBuffer> armorStand$getUniformBuffers() {
        return this.uniformBlocks;
    }

    @Override
    public void armorStand$setUniform(String name, GpuBuffer buffer) {
        this.uniformBlocks.put(name, buffer);
    }

    @Override
    public void armorStand$drawInstanced(int instances, int offset, int count) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }
        ((GlResourceManagerExtInternal) this.resourceManager).armorStand$drawInstancedBoundObjectWithRenderPass((RenderPassImpl) (Object) this, instances, offset, count, null);
    }

    @Override
    public void armorStand$drawIndexedInstanced(int instances, int offset, int count) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }
        ((GlResourceManagerExtInternal) this.resourceManager).armorStand$drawInstancedBoundObjectWithRenderPass((RenderPassImpl) (Object) this, instances, offset, count, this.indexType);
    }

    @Inject(method = "setVertexBuffer", at = @At("HEAD"))
    public void onSetVertexBuffer(int i, GpuBuffer gpuBuffer, CallbackInfo ci) {
        if (this.vertexBuffer != null) {
            throw new IllegalStateException("Can't set Blaze3d VertexBuffer to a RenderPass already having an ArmorStand vertex buffer");
        }
    }
}
