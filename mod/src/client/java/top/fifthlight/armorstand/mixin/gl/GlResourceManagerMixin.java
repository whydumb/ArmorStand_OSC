package top.fifthlight.armorstand.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gl.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32C;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.armorstand.extension.RenderObjectExt;
import top.fifthlight.armorstand.extension.internal.gl.GlResourceManagerExtInternal;
import top.fifthlight.armorstand.extension.internal.RenderObjectExtInternal;
import top.fifthlight.armorstand.extension.internal.ShaderProgramExtInternal;
import top.fifthlight.armorstand.extension.internal.gl.GlRenderPassImplExtInternal;
import top.fifthlight.armorstand.helper.gl.GlStateManagerHelper;
import top.fifthlight.armorstand.render.gl.GlTextureBuffer;
import top.fifthlight.armorstand.render.gl.GlVertexBuffer;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mixin(GlResourceManager.class)
public abstract class GlResourceManagerMixin implements GlResourceManagerExtInternal {
    @Shadow
    protected abstract boolean setupRenderPass(RenderPassImpl pass);

    @Shadow
    @Final
    private GlBackend backend;

    @WrapOperation(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormatMode()Lcom/mojang/blaze3d/vertex/VertexFormat$DrawMode;"))
    private VertexFormat.DrawMode onSetupRenderPassGetDrawMode(RenderPipeline instance, Operation<VertexFormat.DrawMode> original, RenderPassImpl pass) {
        var vertexBuffer = ((GlRenderPassImplExtInternal) pass).armorStand$getVertexBuffer();
        if (vertexBuffer != null) {
            return vertexBuffer.getMode();
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormatMode()Lcom/mojang/blaze3d/vertex/VertexFormat$DrawMode;"))
    private VertexFormat.DrawMode onDrawObjectWithRenderPassGetVertexMode(RenderPipeline instance, Operation<VertexFormat.DrawMode> original, RenderPassImpl pass) {
        var vertexBuffer = ((GlRenderPassImplExtInternal) pass).armorStand$getVertexBuffer();
        if (vertexBuffer != null) {
            return vertexBuffer.getMode();
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/BufferManager;setupBuffer(Lcom/mojang/blaze3d/vertex/VertexFormat;Lnet/minecraft/client/gl/GlGpuBuffer;)V"))
    private void onDrawObjectWithRenderPassSetupBuffer(BufferManager instance, VertexFormat vertexFormat, GlGpuBuffer glGpuBuffer, Operation<Void> original, RenderPassImpl pass) {
        var vertexBuffer = ((GlRenderPassImplExtInternal) pass).armorStand$getVertexBuffer();
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
        var armorStandVertexBuffer = ((GlRenderPassImplExtInternal) pass).armorStand$getVertexBuffer();
        if (blaze3DVertexBuffer == null && armorStandVertexBuffer == null) {
            throw new IllegalStateException("Missing vertex buffer");
        }

        if ((blaze3DVertexBuffer != null && blaze3DVertexBuffer.isClosed()) || (armorStandVertexBuffer != null && armorStandVertexBuffer.getClosed())) {
            throw new IllegalStateException("Vertex buffer has been closed!");
        }
    }

    @ModifyExpressionValue(method = "drawBoundObjectWithRenderPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/RenderPassImpl;IS_DEVELOPMENT:Z"))
    private boolean onCheckSingleRenderPass(boolean original, RenderPassImpl pass, int first, int count, @Nullable VertexFormat.IndexType indexType) {
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

    @WrapWithCondition(method = "drawObjectsWithRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/RenderPassImpl;setVertexBuffer(ILcom/mojang/blaze3d/buffers/GpuBuffer;)V"))
    private boolean onVertexBufferSetWhenDrawingMultipleObjects(RenderPassImpl pass, int i, GpuBuffer gpuBuffer, @Local(ordinal = 0) RenderPass.RenderObject renderObject) {
        var vertexBuffer = ((RenderObjectExt) (Object) renderObject).armorStand$getVertexBuffer();
        if (vertexBuffer == null) {
            return true;
        }
        ((RenderObjectExtInternal) pass).armorStand$setVertexBuffer(vertexBuffer);
        return false;
    }

    @WrapWithCondition(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private boolean shouldReportDepthTextureProblem(Logger instance, String message, Object locationObj) {
        var location = (Identifier) locationObj;
        // Render pipeline owo:pipeline/gui_blur wants a depth texture but none was provided - this is probably a bug
        return !location.getNamespace().equals("owo");
    }

    @Redirect(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;getSamplers()Ljava/util/List;", ordinal = 0))
    private List<String> onCheckSamplers(ShaderProgram instance, RenderPassImpl pass) {
        var bufferSamplerUniforms = ((GlRenderPassImplExtInternal) pass).armorStand$getBufferSamplerUniforms();

        //noinspection DataFlowIssue,resource
        for (var samplerName : pass.pipeline.program().getSamplers()) {
            var sampler = pass.samplerUniforms.get(samplerName);
            var bufferSampler = bufferSamplerUniforms.get(samplerName);
            if (sampler == null && bufferSampler == null) {
                throw new IllegalStateException("Missing sampler " + samplerName);
            }

            if ((sampler != null && sampler.isClosed()) || (bufferSampler != null && bufferSampler.getClosed())) {
                throw new IllegalStateException("Sampler " + samplerName + " has been closed!");
            }
        }
        return List.of();
    }

    @WrapOperation(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;getSamplerLocations()Lit/unimi/dsi/fastutil/ints/IntList;"))
    private IntList onSetupRenderPassGetSamplerUniform(ShaderProgram instance, Operation<IntList> original, RenderPassImpl pass, @Local boolean switchedProgram, @Local ShaderProgram shaderProgram) {
        var samplerLocations = original.call(instance);
        var bufferSamplerUniforms = ((GlRenderPassImplExtInternal) pass).armorStand$getBufferSamplerUniforms();
        for (var i = 0; i < shaderProgram.getSamplers().size(); i++) {
            var name = shaderProgram.getSamplers().get(i);
            var bufferSampler = (GlTextureBuffer) bufferSamplerUniforms.get(name);
            if (bufferSampler != null) {
                if (switchedProgram || pass.setSamplers.contains(name)) {
                    var samplerLocation = samplerLocations.getInt(i);
                    GlUniform.setUniform(samplerLocation, i);
                    GlStateManager._activeTexture(GlConst.GL_TEXTURE0 + i);
                }

                GlStateManagerHelper._bindTexture(GL32C.GL_TEXTURE_BUFFER, bufferSampler.getGlId());
            }
        }
        return samplerLocations;
    }

    @Inject(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;initializeUniforms(Lcom/mojang/blaze3d/vertex/VertexFormat$DrawMode;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FF)V", shift = At.Shift.AFTER))
    private void onSetupRenderPassInitializeUniform(RenderPassImpl pass, CallbackInfoReturnable<Boolean> cir) {
        var pipeline = Objects.requireNonNull(pass.pipeline);
        var shaderProgram = Objects.requireNonNull(pipeline.program());
        var uniformBuffers = ((GlRenderPassImplExtInternal) pass).armorStand$getUniformBuffers();
        var shaderUniformBlocks = ((ShaderProgramExtInternal) shaderProgram).armorstand$getUniformBlocks();
        var uniformCount = shaderUniformBlocks.size();
        for (var i = 0; i < uniformCount; i++) {
            var uniformName = shaderUniformBlocks.get(i);
            var buffer = uniformBuffers.get(uniformName);
            if (buffer == null) {
                throw new IllegalStateException("Missing uniform buffer object " + uniformName);
            }

            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, i, ((GlGpuBuffer) buffer).id);
        }
    }

    @Unique
    private void drawInstancedObjectWithRenderPass(@NotNull RenderPassImpl pass, int instances, int first, int count, @Nullable VertexFormat.IndexType indexType, CompiledShaderPipeline pipeline) {
        var vertexBuffer = ((GlRenderPassImplExtInternal) pass).armorStand$getVertexBuffer();
        VertexFormat.DrawMode vertexFormatMode;
        if (vertexBuffer != null) {
            vertexFormatMode = vertexBuffer.getMode();
            var buffer = (GlVertexBuffer) vertexBuffer;
            GlStateManager._glBindVertexArray(buffer.getVaoId());
        } else {
            var info = pipeline.info();
            vertexFormatMode = info.getVertexFormatMode();
            this.backend.getBufferManager().setupBuffer(info.getVertexFormat(), (GlGpuBuffer) pass.vertexBuffers[0]);
        }
        if (indexType != null) {
            //noinspection DataFlowIssue, because we check it in outer methods
            GlStateManager._glBindBuffer(GlConst.GL_ELEMENT_ARRAY_BUFFER, ((GlGpuBuffer) pass.indexBuffer).id);
            GlStateManagerHelper._drawElementsInstanced(GlConst.toGl(vertexFormatMode), count, GlConst.toGl(indexType), (long) first * indexType.size, instances);
        } else {
            GlStateManagerHelper._drawArraysInstanced(GlConst.toGl(vertexFormatMode), first, count, instances);
        }
    }

    @Override
    public void armorStand$drawInstancedBoundObjectWithRenderPass(@NotNull RenderPassImpl pass, int instances, int first, int count, VertexFormat.@Nullable IndexType indexType) {
        if (this.setupRenderPass(pass)) {
            if (RenderPassImpl.IS_DEVELOPMENT) {
                checkRenderPassBuffers(pass, indexType != null);
            }

            if (pass.pipeline == null) {
                throw new IllegalStateException("Pipeline is null");
            }
            this.drawInstancedObjectWithRenderPass(pass, instances, first, count, indexType, pass.pipeline);
        }
    }
}
