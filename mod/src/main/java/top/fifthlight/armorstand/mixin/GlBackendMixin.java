package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.helper.GlStateManagerHelper;
import top.fifthlight.armorstand.helper.GpuDeviceExt;
import top.fifthlight.armorstand.helper.RenderPipelineWithVertexType;
import top.fifthlight.armorstand.render.GpuTextureBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;
import top.fifthlight.armorstand.render.gl.FilledVertexElementProvider;
import top.fifthlight.armorstand.render.gl.GlTextureBuffer;
import top.fifthlight.armorstand.render.gl.GlVertexBuffer;
import top.fifthlight.armorstand.util.ShaderProgramExt;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Mixin(GlBackend.class)
public abstract class GlBackendMixin implements GpuDeviceExt {
    @Shadow
    protected abstract CompiledShader compileShader(Identifier id, ShaderType type, Defines defines, BiFunction<Identifier, ShaderType, String> sourceRetriever);

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private DebugLabelManager debugLabelManager;

    @Shadow
    @Final
    private Set<String> usedGlCapabilities;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(long contextId, int debugVerbosity, boolean sync, BiFunction<Identifier, ShaderType, String> shaderSourceGetter, boolean renderDebugLabels, CallbackInfo ci, @Local GLCapabilities capabilities) {
        FilledVertexElementProvider.INSTANCE.init(capabilities, this.debugLabelManager, this.usedGlCapabilities);
    }

    @WrapMethod(method = "compileRenderPipeline")
    private CompiledShaderPipeline compileRenderPipeline(RenderPipeline pipeline, BiFunction<Identifier, ShaderType, String> sourceRetriever, Operation<CompiledShaderPipeline> original) {
        var vertexType = ((RenderPipelineWithVertexType) pipeline).armorStand$getVertexType();
        if (vertexType != null) {
            CompiledShader compiledShader = this.compileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), sourceRetriever);
            CompiledShader compiledShader2 = this.compileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), sourceRetriever);
            if (compiledShader == CompiledShader.INVALID_SHADER) {
                LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", pipeline.getLocation(), pipeline.getVertexShader());
                return new CompiledShaderPipeline(pipeline, ShaderProgram.INVALID);
            } else if (compiledShader2 == CompiledShader.INVALID_SHADER) {
                LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", pipeline.getLocation(), pipeline.getFragmentShader());
                return new CompiledShaderPipeline(pipeline, ShaderProgram.INVALID);
            } else {
                ShaderProgram shaderProgram;
                try {
                    shaderProgram = ShaderProgramExt.create(compiledShader, compiledShader2, vertexType, pipeline.getLocation().toString());
                } catch (ShaderLoader.LoadException ex) {
                    LOGGER.error("Couldn't compile program for pipeline {}: {}", pipeline.getLocation(), ex);
                    return new CompiledShaderPipeline(pipeline, ShaderProgram.INVALID);
                }

                shaderProgram.set(pipeline.getUniforms(), pipeline.getSamplers());
                this.debugLabelManager.labelShaderProgram(shaderProgram);
                return new CompiledShaderPipeline(pipeline, shaderProgram);
            }
        } else {
            return original.call(pipeline, sourceRetriever);
        }
    }

    @Override
    public @NotNull GpuBuffer armorStand$createBuffer(@Nullable Supplier<String> labelGetter, BufferType type, BufferUsage usage, int size, FillType fillType) {
        return FilledVertexElementProvider.INSTANCE.generate(labelGetter, type, usage, size, fillType);
    }

    @Override
    @NotNull
    public VertexBuffer armorStand$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount) {
        return new GlVertexBuffer(mode, elements, verticesCount);
    }

    @Override
    @NotNull
    public GpuTextureBuffer armorStand$createTextureBuffer(@Nullable String label, TextureFormat format, GpuBuffer buffer) {
        RenderSystem.assertOnRenderThread();
        if (buffer.isClosed()) {
            throw new IllegalStateException("Source buffer is closed");
        }
        if (buffer.size % format.pixelSize() != 0) {
            throw new IllegalArgumentException("Bad byte size " + buffer.size + " for format " + format + " (" + buffer.size + " % " + format.pixelSize() + " != 0)");
        }
        int textureId = GlStateManager._genTexture();
        if (label == null) {
            label = String.valueOf(textureId);
        }
        GlStateManagerHelper._bindTexture(GL32C.GL_TEXTURE_BUFFER, textureId);
        GlStateManagerHelper._glTexBuffer(GL32C.GL_TEXTURE_BUFFER, GlConst.toGlInternalId(format), ((GlGpuBuffer) buffer).id);
        return new GlTextureBuffer(textureId, label, format);
    }
}
