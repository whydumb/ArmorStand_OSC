package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.BufferManager;
import net.minecraft.client.gl.GlGpuBuffer;
import net.minecraft.client.gl.GlResourceManager;
import net.minecraft.client.gl.RenderPassImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer;
import top.fifthlight.armorstand.render.gl.GlVertexBuffer;

import java.util.Collection;

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

    @Unique
    private void checkRenderPassBuffers(RenderPassImpl pass, boolean checkIndexBuffer) {
        if (checkIndexBuffer) {
            if (pass.indexBuffer == null) {
                throw new IllegalStateException("Missing index buffer");
            }

            if (pass.indexBuffer.isClosed()) {
                throw new IllegalStateException("Index buffer has been closed!");
            }
        }

        var blaze3DVertexBuffer = pass.vertexBuffers[0];
        var armorStandVertexBuffer = ((RenderPassWithVertexBuffer) pass).armorStand$getVertexBuffer();
        if (blaze3DVertexBuffer == null && armorStandVertexBuffer == null) {
            throw new IllegalStateException("Missing vertex buffer");
        }

        if ((blaze3DVertexBuffer != null && blaze3DVertexBuffer.isClosed()) || (armorStandVertexBuffer != null && armorStandVertexBuffer.getClosed())) {
            throw new IllegalStateException("Vertex buffer has been closed!");
        }
    }

    @ModifyExpressionValue(method = "drawBoundObjectWithRenderPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/RenderPassImpl;IS_DEVELOPMENT:Z"))
    public boolean onCheckSingleRenderPass(boolean original, RenderPassImpl pass, int first, int count, @Nullable VertexFormat.IndexType indexType) {
        if (original) {
            checkRenderPassBuffers(pass, indexType != null);
        }
        return false;
    }

    @ModifyExpressionValue(method = "drawObjectsWithRenderPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/RenderPassImpl;IS_DEVELOPMENT:Z"))
    public boolean onCheckMultipleRenderPass(boolean original, RenderPassImpl pass, Collection<RenderPass.RenderObject> objects, @Nullable GpuBuffer indexBuffer, @Nullable VertexFormat.IndexType indexType) {
        if (original) {
            checkRenderPassBuffers(pass, true);
        }
        return false;
    }
}
