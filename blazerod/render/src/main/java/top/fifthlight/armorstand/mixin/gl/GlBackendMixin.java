package top.fifthlight.armorstand.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.fifthlight.armorstand.extension.GpuDeviceExt;
import top.fifthlight.armorstand.extension.RenderPipelineExt;
import top.fifthlight.armorstand.helper.ShaderProgramHelper;
import top.fifthlight.armorstand.render.VertexBuffer;
import top.fifthlight.armorstand.render.gl.GlVertexBuffer;

import java.util.List;

@Mixin(GlBackend.class)
public abstract class GlBackendMixin implements GpuDeviceExt {
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
    @NotNull
    public VertexBuffer armorStand$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount) {
        return new GlVertexBuffer(mode, elements, verticesCount);
    }
}
