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
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;

@Mixin(RenderPassImpl.class)
public abstract class RenderPassImplMixin implements RenderPassWithVertexBuffer {
    @Shadow
    @Final
    protected GpuBuffer[] vertexBuffers;

    @Unique
    VertexBuffer vertexBuffer;

    @Override
    public @Nullable VertexBuffer armorStand$getVertexBuffer() {
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
}
