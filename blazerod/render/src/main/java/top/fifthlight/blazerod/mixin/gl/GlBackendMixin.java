package top.fifthlight.blazerod.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBTextureBufferRange;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.blazerod.extension.GpuDeviceExt;
import top.fifthlight.blazerod.extension.RenderPipelineExt;
import top.fifthlight.blazerod.helper.ShaderProgramHelper;
import top.fifthlight.blazerod.util.GpuShaderDataPool;
import top.fifthlight.blazerod.render.VertexBuffer;
import top.fifthlight.blazerod.render.gl.GlVertexBuffer;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

@Mixin(GlBackend.class)
public abstract class GlBackendMixin implements GpuDeviceExt {
    @Unique
    private static final boolean allowGlTextureBufferRange = true;

    // Not support for now
    @Unique
    private static final boolean allowGlShaderStorageBufferObject = false;

    @Shadow
    @Final
    private Set<String> usedGlCapabilities;

    @Shadow
    @Final
    private int uniformOffsetAlignment;

    @Unique
    private GpuShaderDataPool gpuShaderDataPool;

    @Unique
    private boolean supportSsbo;

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

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(long contextId, int debugVerbosity, boolean sync, BiFunction<Identifier, ShaderType, String> shaderSourceGetter, boolean renderDebugLabels, CallbackInfo ci, @Local(ordinal = 0) GLCapabilities glCapabilities) {
        if (glCapabilities.GL_ARB_shader_storage_buffer_object && allowGlShaderStorageBufferObject) {
            usedGlCapabilities.add("ARB_shader_storage_buffer_object");
            supportSsbo = true;
        } else {
            supportSsbo = false;
        }

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
        gpuShaderDataPool = GpuShaderDataPool.create(shaderDataAlignment, supportSsbo || supportTextureBufferSlice);
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

    @Override
    @NotNull
    public VertexBuffer blazerod$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount) {
        return new GlVertexBuffer(mode, elements, verticesCount);
    }

    @NotNull
    @Override
    public GpuShaderDataPool blazerod$getShaderDataPool() {
        return gpuShaderDataPool;
    }
}
