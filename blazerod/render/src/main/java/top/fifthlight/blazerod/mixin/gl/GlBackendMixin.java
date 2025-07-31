package top.fifthlight.blazerod.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.blazerod.extension.GpuDeviceExt;
import top.fifthlight.blazerod.extension.RenderPipelineExt;
import top.fifthlight.blazerod.extension.internal.GpuBufferExtInternal;
import top.fifthlight.blazerod.extension.internal.RenderPipelineExtInternal;
import top.fifthlight.blazerod.extension.internal.gl.ShaderProgramExt;
import top.fifthlight.blazerod.helper.ShaderProgramHelper;
import top.fifthlight.blazerod.render.VertexBuffer;
import top.fifthlight.blazerod.render.gl.GlVertexBuffer;
import top.fifthlight.blazerod.util.GlslExtensionProcessor;
import top.fifthlight.blazerod.util.GpuShaderDataPool;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Mixin(GlBackend.class)
public abstract class GlBackendMixin implements GpuDeviceExt {
    @Unique
    private static final boolean allowGlTextureBufferRange = true;

    @Unique
    private static final boolean allowGlShaderStorageBufferObject = true;

    @Shadow
    @Final
    private Set<String> usedGlCapabilities;

    @Shadow
    @Final
    private int uniformOffsetAlignment;
    @Unique
    private int glMajorVersion;
    @Unique
    private int glMinorVersion;

    @Unique
    private GpuShaderDataPool gpuShaderDataPool;

    @Unique
    private boolean supportSsbo;

    @Shadow
    public abstract GpuBuffer createBuffer(@Nullable Supplier<String> labelSupplier, int usage, int size);

    @Shadow
    public abstract GpuBuffer createBuffer(@Nullable Supplier<String> labelSupplier, int usage, ByteBuffer data);

    @Unique
    private int gcd(int a, int b) {
        var max = Math.max(a, b);
        var min = Math.min(a, b);
        while (min != 0) {
            var temp = max % min;
            max = min;
            min = temp;
        }
        return max;
    }

    @Unique
    private int lcm(int a, int b) {
        return a * b / gcd(a, b);
    }

    @Unique
    private static boolean canUseSsbo(boolean useVertexSsbo) {
        // OpenGL spec says GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS must be at least 8, but we check it
        // just in case some drivers don't follow the spec
        if (GL11.glGetInteger(GL43C.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) < 8) {
            return true;
        }
        if (useVertexSsbo && GL11.glGetInteger(GL43C.GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS) < 8) {
            return false;
        }
        return true;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(long contextId, int debugVerbosity, boolean sync, BiFunction<Identifier, ShaderType, String> shaderSourceGetter, boolean renderDebugLabels, CallbackInfo ci, @Local(ordinal = 0) GLCapabilities glCapabilities) {
        if (allowGlShaderStorageBufferObject && glCapabilities.GL_ARB_shader_storage_buffer_object && glCapabilities.GL_ARB_program_interface_query && canUseSsbo(true)) {
            usedGlCapabilities.add("ARB_shader_storage_buffer_object");
            usedGlCapabilities.add("ARB_program_interface_query");
            supportSsbo = true;
        } else {
            supportSsbo = false;
        }

        glMajorVersion = GL11.glGetInteger(GL30C.GL_MAJOR_VERSION);
        glMinorVersion = GL11.glGetInteger(GL30C.GL_MINOR_VERSION);

        boolean supportTextureBufferSlice;
        if (glCapabilities.GL_ARB_texture_buffer_range && allowGlTextureBufferRange) {
            usedGlCapabilities.add("ARB_texture_buffer_range");
            supportTextureBufferSlice = true;
        } else {
            supportTextureBufferSlice = false;
        }
        var vanillaAlignment = uniformOffsetAlignment;
        int shaderDataAlignment;
        if (supportSsbo) {
            shaderDataAlignment = GL11.glGetInteger(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT);
        } else if (supportTextureBufferSlice) {
            shaderDataAlignment = GL11.glGetInteger(ARBTextureBufferRange.GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT);
        } else {
            shaderDataAlignment = 0;
        }
        if (shaderDataAlignment != 0 && vanillaAlignment != 0) {
            shaderDataAlignment = lcm(vanillaAlignment, shaderDataAlignment);
        } else {
            shaderDataAlignment = Math.max(vanillaAlignment, shaderDataAlignment);
        }
        gpuShaderDataPool = GpuShaderDataPool.create(supportSsbo, shaderDataAlignment, supportSsbo || supportTextureBufferSlice);
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void onClose(CallbackInfo ci) throws Exception {
        gpuShaderDataPool.close();
    }

    @WrapOperation(method = "compileRenderPipeline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;create(Lnet/minecraft/client/gl/CompiledShader;Lnet/minecraft/client/gl/CompiledShader;Lcom/mojang/blaze3d/vertex/VertexFormat;Ljava/lang/String;)Lnet/minecraft/client/gl/ShaderProgram;"))
    private ShaderProgram onCompileShaderProgram(CompiledShader vertexShader, CompiledShader fragmentShader, VertexFormat format, String name, Operation<ShaderProgram> original, RenderPipeline pipeline) throws ShaderLoader.LoadException {
        var vertexType = ((RenderPipelineExt) pipeline).blazerod$getVertexType();
        if (vertexType.isPresent()) {
            return ShaderProgramHelper.create(vertexShader, fragmentShader, vertexType.get(), pipeline.getLocation().toString());
        } else {
            return original.call(vertexShader, fragmentShader, format, name);
        }
    }

    @Inject(method = "compileRenderPipeline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/ShaderProgram;set(Ljava/util/List;Ljava/util/List;)V"))
    private void onSetShaderProgram(RenderPipeline pipeline, BiFunction<Identifier, ShaderType, String> sourceRetriever, CallbackInfoReturnable<CompiledShaderPipeline> cir, @Local ShaderProgram shaderProgram) {
        var shaderProgramExt = (ShaderProgramExt) shaderProgram;
        var pipelineExt = (RenderPipelineExtInternal) pipeline;
        shaderProgramExt.blazerod$setStorageBuffers(pipelineExt.blazerod$getStorageBuffers());
    }

    @WrapOperation(method = "compileShader(Lnet/minecraft/client/gl/GlBackend$ShaderKey;Ljava/util/function/BiFunction;)Lnet/minecraft/client/gl/CompiledShader;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlImportProcessor;addDefines(Ljava/lang/String;Lnet/minecraft/client/gl/Defines;)Ljava/lang/String;"))
    public String modifyShaderSource(String source, Defines defines, Operation<String> original) {
        var context = new GlslExtensionProcessor.Context(glMajorVersion, glMinorVersion, defines);
        var processedShader = GlslExtensionProcessor.process(context, source);
        return original.call(processedShader, defines);
    }

    @Override
    @NotNull
    public VertexBuffer blazerod$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount) {
        return new GlVertexBuffer(mode, elements, verticesCount);
    }

    @NotNull
    @Override
    public GpuBuffer blazerod$createBuffer(@Nullable Supplier<String> labelSupplier, int usage, int extraUsage, int size) {
        var buffer = createBuffer(labelSupplier, usage, size);
        ((GpuBufferExtInternal) buffer).blazerod$setExtraUsage(extraUsage);
        return buffer;
    }

    @NotNull
    @Override
    public GpuBuffer blazerod$createBuffer(@Nullable Supplier<String> labelSupplier, int usage, int extraUsage, ByteBuffer data) {
        var buffer = createBuffer(labelSupplier, usage, data);
        ((GpuBufferExtInternal) buffer).blazerod$setExtraUsage(extraUsage);
        return buffer;
    }

    @NotNull
    @Override
    public GpuShaderDataPool blazerod$getShaderDataPool() {
        return gpuShaderDataPool;
    }

    @Override
    public boolean blazerod$supportSsbo() {
        return supportSsbo;
    }
}
