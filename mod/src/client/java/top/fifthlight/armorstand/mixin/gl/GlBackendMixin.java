package top.fifthlight.armorstand.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.helper.gl.GlStateManagerHelper;
import top.fifthlight.armorstand.extension.GpuDeviceExt;
import top.fifthlight.armorstand.extension.RenderPipelineExt;
import top.fifthlight.armorstand.helper.ShaderProgramHelper;
import top.fifthlight.armorstand.render.GpuTextureBuffer;
import top.fifthlight.armorstand.render.TextureBufferFormat;
import top.fifthlight.armorstand.render.VertexBuffer;
import top.fifthlight.armorstand.render.gl.FilledVertexElementProvider;
import top.fifthlight.armorstand.render.gl.GlConstKt;
import top.fifthlight.armorstand.render.gl.GlTextureBuffer;
import top.fifthlight.armorstand.render.gl.GlVertexBuffer;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Mixin(GlBackend.class)
public abstract class GlBackendMixin implements GpuDeviceExt {
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

    @WrapOperation(method = "compileRenderPipeline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;create(Lnet/minecraft/client/gl/CompiledShader;Lnet/minecraft/client/gl/CompiledShader;Lcom/mojang/blaze3d/vertex/VertexFormat;Ljava/lang/String;)Lnet/minecraft/client/gl/ShaderProgram;"))
    private ShaderProgram onCompileShaderProgram(CompiledShader vertexShader, CompiledShader fragmentShader, VertexFormat format, String name, Operation<ShaderProgram> original, RenderPipeline pipeline) throws ShaderLoader.LoadException {
        var vertexType = ((RenderPipelineExt) pipeline).armorStand$getVertexType();
        if (vertexType.isPresent()) {
            return ShaderProgramHelper.create(vertexShader, fragmentShader, vertexType.get(), pipeline.getLocation().toString());
        } else {
            return original.call(vertexShader, fragmentShader, format, name);
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
    public GpuTextureBuffer armorStand$createTextureBuffer(@Nullable String label, TextureBufferFormat format, GpuBuffer buffer) {
        RenderSystem.assertOnRenderThread();
        if (buffer.isClosed()) {
            throw new IllegalStateException("Source buffer is closed");
        }
        if (buffer.size % format.getPixelSize() != 0) {
            throw new IllegalArgumentException("Bad byte size " + buffer.size + " for format " + format + " (" + buffer.size + " % " + format.getPixelSize() + " != 0)");
        }
        var textureId = GlStateManager._genTexture();
        if (label == null) {
            label = String.valueOf(textureId);
        }
        GlStateManagerHelper._bindTexture(GL32C.GL_TEXTURE_BUFFER, textureId);
        GlStateManagerHelper._glTexBuffer(GL32C.GL_TEXTURE_BUFFER, GlConstKt.toGlInternal(format), ((GlGpuBuffer) buffer).id);
        return new GlTextureBuffer(textureId, label, format);
    }
}
