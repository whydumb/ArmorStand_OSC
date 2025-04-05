package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.BufferManager;
import net.minecraft.client.gl.GlGpuBuffer;
import net.minecraft.client.gl.GlResourceManager;
import net.minecraft.client.gl.RenderPassImpl;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer;
import top.fifthlight.armorstand.render.gl.GlVertexBuffer;

@Mixin(GlResourceManager.class)
public class GlResourceManagerMixin {
    @WrapOperation(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormatMode()Lcom/mojang/blaze3d/vertex/VertexFormat$DrawMode;"))
    private VertexFormat.DrawMode onSetupRenderPassGetDrawMode(RenderPipeline instance, Operation<VertexFormat.DrawMode> original, RenderPassImpl pass) {
        var vertexBuffer = ((RenderPassWithVertexBuffer) pass).armorStand$getVertexBuffer();
        if (vertexBuffer != null) {
            return vertexBuffer.getMode();
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormatMode()Lcom/mojang/blaze3d/vertex/VertexFormat$DrawMode;"))
    private VertexFormat.DrawMode onDrawObjectWithRenderPassGetVertexMode(RenderPipeline instance, Operation<VertexFormat.DrawMode> original, RenderPassImpl pass) {
        var vertexBuffer = ((RenderPassWithVertexBuffer) pass).armorStand$getVertexBuffer();
        if (vertexBuffer != null) {
            return vertexBuffer.getMode();
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/BufferManager;setupBuffer(Lcom/mojang/blaze3d/vertex/VertexFormat;Lnet/minecraft/client/gl/GlGpuBuffer;)V"))
    private void onDrawObjectWithRenderPassSetupBuffer(BufferManager instance, VertexFormat vertexFormat, GlGpuBuffer glGpuBuffer, Operation<Void> original, RenderPassImpl pass) {
        var vertexBuffer = ((RenderPassWithVertexBuffer) pass).armorStand$getVertexBuffer();
        if (vertexBuffer != null) {
            var buffer = (GlVertexBuffer) vertexBuffer;
            GlStateManager._glBindVertexArray(buffer.getVaoId());
        } else {
            original.call(instance, vertexFormat, glGpuBuffer);
        }
    }

    @Unique
    private static boolean isPowerOfTwo(int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    @ModifyArg(method = "writeToTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Lnet/minecraft/client/texture/NativeImage;IIIIIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_pixelStore(II)V", ordinal = 3), index = 1)
    private int onSetTextureUnpackAlignmentNativeImage(int param) {
        if (!isPowerOfTwo(param)) {
            return 1;
        }
        return param;
    }

    @ModifyArg(method = "writeToTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Ljava/nio/IntBuffer;Lnet/minecraft/client/texture/NativeImage$Format;IIIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_pixelStore(II)V", ordinal = 3), index = 1)
    private int onSetTextureUnpackAlignmentIntBuffer(int param) {
        if (!isPowerOfTwo(param)) {
            return 1;
        }
        return param;
    }
}
