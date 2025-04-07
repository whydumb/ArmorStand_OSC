package top.fifthlight.armorstand.mixin;

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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32C;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.fifthlight.armorstand.helper.GlStateManagerHelper;
import top.fifthlight.armorstand.helper.RenderPassWithTextureBuffer;
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer;
import top.fifthlight.armorstand.render.gl.GlTextureBuffer;
import top.fifthlight.armorstand.render.gl.GlVertexBuffer;

import java.util.Collection;

@Mixin(GlResourceManager.class)
public class GlResourceManagerMixin {
    @Final
    @Shadow
    private static Logger LOGGER;

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
        var vertexBuffer = ((RenderPassWithVertexBuffer) (Object) renderObject).armorStand$getVertexBuffer();
        if (vertexBuffer == null) {
            return true;
        }
        ((RenderPassWithVertexBuffer) pass).armorStand$setVertexBuffer(vertexBuffer);
        return false;
    }

    @Unique
    private void checkRenderPassUniforms(RenderPassImpl pass) {
        if (pass.pipeline == null) {
            throw new IllegalStateException("Can't draw without a render pipeline");
        }

        if (pass.pipeline.program() == ShaderProgram.INVALID) {
            throw new IllegalStateException("Pipeline contains invalid shader program");
        }

        for (RenderPipeline.UniformDescription uniformDescription : pass.pipeline.info().getUniforms()) {
            Object object = pass.simpleUniforms.get(uniformDescription.name());
            if (object == null && !ShaderProgram.PREDEFINED_UNIFORMS.contains(uniformDescription.name())) {
                throw new IllegalStateException("Missing uniform " + uniformDescription.name() + " (should be " + uniformDescription.type() + ")");
            }
        }

        var bufferSamplerUniforms = ((RenderPassWithTextureBuffer) pass).armorStand$getBufferSamplerUniforms();

        for (String samplerName : pass.pipeline.program().getSamplers()) {
            var sampler = pass.samplerUniforms.get(samplerName);
            var bufferSampler = bufferSamplerUniforms.get(samplerName);
            if (sampler == null && bufferSampler == null) {
                throw new IllegalStateException("Missing sampler " + samplerName);
            }

            if ((sampler != null && sampler.isClosed()) || (bufferSampler != null && bufferSampler.getClosed())) {
                throw new IllegalStateException("Sampler " + samplerName + " has been closed!");
            }
        }

        if (pass.pipeline.info().wantsDepthTexture() && !pass.hasDepth()) {
            LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", pass.pipeline.info().getLocation());
        }
    }

    @ModifyExpressionValue(method = "setupRenderPass(Lnet/minecraft/client/gl/RenderPassImpl;)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/RenderPassImpl;IS_DEVELOPMENT:Z"))
    private boolean onCheckRenderPassUniforms(boolean isDevelopment, RenderPassImpl pass) {
        if (isDevelopment) {
            checkRenderPassUniforms(pass);
        }
        return false;
    }

    @WrapOperation(method = "setupRenderPass(Lnet/minecraft/client/gl/RenderPassImpl;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;getSamplerLocations()Lit/unimi/dsi/fastutil/ints/IntList;"))
    private IntList onSetupRenderPassGetSamplerUniform(ShaderProgram instance, Operation<IntList> original, RenderPassImpl pass, @Local boolean switchedProgram, @Local ShaderProgram shaderProgram) {
        var samplerLocations = original.call(instance);
        var bufferSamplerUniforms = ((RenderPassWithTextureBuffer) pass).armorStand$getBufferSamplerUniforms();
        for (int i = 0; i < shaderProgram.getSamplers().size(); i++) {
            var name = shaderProgram.getSamplers().get(i);
            var bufferSampler = (GlTextureBuffer) bufferSamplerUniforms.get(name);
            if (bufferSampler != null) {
                if (switchedProgram || pass.setSamplers.contains(name)) {
                    int samplerLocation = samplerLocations.getInt(i);
                    GlUniform.setUniform(samplerLocation, i);
                    GlStateManager._activeTexture(GlConst.GL_TEXTURE0 + i);
                }

                GlStateManagerHelper._bindTexture(GL32C.GL_TEXTURE_BUFFER, bufferSampler.getGlId());
            }
        }
        return samplerLocations;
    }
}
