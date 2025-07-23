package top.fifthlight.blazerod.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBTextureBufferRange;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.blazerod.extension.CommandEncoderExt;
import top.fifthlight.blazerod.extension.GpuBufferExt;
import top.fifthlight.blazerod.extension.GpuDeviceExt;
import top.fifthlight.blazerod.extension.RenderObjectExt;
import top.fifthlight.blazerod.extension.internal.RenderObjectExtInternal;
import top.fifthlight.blazerod.extension.internal.RenderPassExtInternal;
import top.fifthlight.blazerod.extension.internal.gl.BufferManagerExtInternal;
import top.fifthlight.blazerod.extension.internal.gl.ShaderProgramExt;
import top.fifthlight.blazerod.render.gl.BufferClearer;
import top.fifthlight.blazerod.render.gl.GlVertexBuffer;

import java.nio.ByteBuffer;
import java.util.Collection;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderExt {

    @Shadow
    @Final
    private GlBackend backend;

    @Shadow
    public abstract void writeToBuffer(GpuBufferSlice gpuBufferSlice, ByteBuffer byteBuffer);

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormatMode()Lcom/mojang/blaze3d/vertex/VertexFormat$DrawMode;"))
    private VertexFormat.DrawMode onDrawObjectWithRenderPassGetVertexMode(RenderPipeline instance, Operation<VertexFormat.DrawMode> original, RenderPassImpl pass) {
        var vertexBuffer = ((RenderPassExtInternal) pass).blazerod$getVertexBuffer();
        if (vertexBuffer != null) {
            return vertexBuffer.getMode();
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/VertexBufferManager;setupBuffer(Lcom/mojang/blaze3d/vertex/VertexFormat;Lnet/minecraft/client/gl/GlGpuBuffer;)V"))
    private void onDrawObjectWithRenderPassSetupBuffer(VertexBufferManager instance, VertexFormat vertexFormat, GlGpuBuffer glGpuBuffer, Operation<Void> original, RenderPassImpl pass) {
        var vertexBuffer = ((RenderPassExtInternal) pass).blazerod$getVertexBuffer();
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

    @ModifyArg(method = "writeToTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Lnet/minecraft/client/texture/NativeImage;IIIIIIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_pixelStore(II)V", ordinal = 3), index = 1)
    private int onSetTextureUnpackAlignmentNativeImage(int param) {
        if (!isPowerOfTwo(param)) {
            return 1;
        }
        return param;
    }

    @ModifyArg(method = "writeToTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Ljava/nio/IntBuffer;Lnet/minecraft/client/texture/NativeImage$Format;IIIIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_pixelStore(II)V", ordinal = 3), index = 1)
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

            if ((pass.indexBuffer.usage() & GpuBuffer.USAGE_INDEX) == 0) {
                throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
            }
        }

        var blaze3DVertexBuffer = pass.vertexBuffers[0];
        var blazeRodVertexBuffer = ((RenderPassExtInternal) pass).blazerod$getVertexBuffer();

        if (blaze3DVertexBuffer == null && blazeRodVertexBuffer == null) {
            throw new IllegalStateException("Missing vertex buffer at slot 0");
        }

        if (blaze3DVertexBuffer != null) {
            if (blaze3DVertexBuffer.isClosed()) {
                throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
            }

            if ((blaze3DVertexBuffer.usage() & GpuBuffer.USAGE_VERTEX) == 0) {
                throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
            }
        }

        if (blazeRodVertexBuffer != null) {
            if (blazeRodVertexBuffer.getClosed()) {
                throw new IllegalStateException("Vertex buffer has been closed!");
            }
        }
    }

    @ModifyExpressionValue(method = "drawBoundObjectWithRenderPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/RenderPassImpl;IS_DEVELOPMENT:Z"))
    private boolean onCheckSingleRenderPass(boolean original, RenderPassImpl pass, int baseVertex, int firstIndex, int count, @Nullable VertexFormat.IndexType indexType) {
        if (original) {
            checkRenderPassBuffers(pass, indexType != null);
        }
        return false;
    }

    @ModifyExpressionValue(method = "drawObjectsWithRenderPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/RenderPassImpl;IS_DEVELOPMENT:Z"))
    private boolean onCheckMultipleRenderPass(boolean original, RenderPassImpl pass, Collection<RenderPass.RenderObject> objects, @Nullable GpuBuffer indexBuffer, @Nullable VertexFormat.IndexType indexType) {
        if (original) {
            checkRenderPassBuffers(pass, true);
        }
        return false;
    }

    @ModifyExpressionValue(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$UniformDescription;type()Lnet/minecraft/client/gl/UniformType;", ordinal = 2))
    private UniformType onCheckTexelBufferUniformSlice(UniformType original) {
        if (original != UniformType.TEXEL_BUFFER) {
            return original;
        }
        if (!((GpuDeviceExt) backend).blazerod$getShaderDataPool().getSupportSlicing()) {
            return original;
        }
        return null;
    }

    @Redirect(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31;glTexBuffer(III)V"))
    private void onSetTexelBufferUniformSlice(int target, int internalformat, int buffer, @Local GpuBufferSlice slice) {
        ARBTextureBufferRange.glTexBufferRange(target, internalformat, buffer, slice.offset(), slice.length());
    }

    @Inject(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V"))
    private void afterClearSimpleUniforms(RenderPassImpl pass, Collection<String> validationSkippedUniforms, CallbackInfoReturnable<Boolean> cir) {
        var renderPipeline = pass.pipeline.info();
        var shaderProgram = pass.pipeline.program();
        var passExt = (RenderPassExtInternal) pass;
        var shaderProgramExt = (ShaderProgramExt) shaderProgram;
        var shaderStorageBuffers = shaderProgramExt.blazerod$getStorageBuffers();
        var passStorageBuffers = passExt.blazerod$getStorageBuffers();

        if (RenderPassImpl.IS_DEVELOPMENT) {
            for (var name : shaderStorageBuffers.keySet()) {
                var entry = passStorageBuffers.get(name);
                if (entry == null) {
                    throw new IllegalStateException("Missing ssbo " + name);
                }
                var glBuffer = entry.buffer();
                var glBufferExt = (GpuBufferExt) glBuffer;
                if ((glBufferExt.blazerod$getExtraUsage() & GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER) == 0) {
                    throw new IllegalStateException("Storage buffer " + name + " must have GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER");
                }
            }
        }

        for (var entry : passStorageBuffers.entrySet()) {
            var name = entry.getKey();
            var info = shaderStorageBuffers.get(name);
            if (info == null) {
                throw new IllegalStateException("Missing ssbo " + name + " for pipeline" + renderPipeline.toString());
            }
            var slice = entry.getValue();
            var glBuffer = (GlGpuBuffer) slice.buffer();
            GL32.glBindBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, info.binding(), glBuffer.id, slice.offset(), slice.length());
        }
    }

    @WrapWithCondition(method = "drawObjectsWithRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/RenderPassImpl;setVertexBuffer(ILcom/mojang/blaze3d/buffers/GpuBuffer;)V"))
    private boolean onVertexBufferSetWhenDrawingMultipleObjects(RenderPassImpl pass, int i, GpuBuffer gpuBuffer, @Local(ordinal = 0) RenderPass.RenderObject<?> renderObject) {
        var vertexBuffer = ((RenderObjectExt) (Object) renderObject).blazerod$getVertexBuffer();
        if (vertexBuffer == null) {
            return true;
        }
        ((RenderObjectExtInternal) pass).blazerod$setVertexBuffer(vertexBuffer);
        return false;
    }

    @Override
    public void blazerod$clearBuffer(GpuBufferSlice slice, ClearType clearType) {
        var buffer = (GlGpuBuffer) slice.buffer();
        var bufferManager = (BufferManagerExtInternal) this.backend.getBufferManager();
        if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
            throw new IllegalArgumentException("Buffer to be cleared should have USAGE_COPY_DST bit set");
        }
        if (bufferManager.blazerod$isGlClearBufferObjectEnabled()) {
            bufferManager.blazerod$clearBufferData(buffer.id, slice.offset(), slice.length(), clearType);
        } else {
            var byteBuffer = ByteBuffer.allocateDirect(slice.length());
            BufferClearer.clear(clearType, byteBuffer);
            writeToBuffer(slice, byteBuffer);
        }
    }
}
