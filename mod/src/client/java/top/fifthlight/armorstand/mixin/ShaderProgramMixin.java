package top.fifthlight.armorstand.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.UniformType;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.extension.internal.ShaderProgramExtInternal;

import java.util.*;

@Mixin(ShaderProgram.class)
public abstract class ShaderProgramMixin implements ShaderProgramExtInternal {
    @Shadow
    @Nullable
    private static UniformType getType(int id) {
        return null;
    }

    @Unique
    private List<String> uniformBlocks;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        uniformBlocks = new ArrayList<>();
    }

    @Override
    public List<String> armorstand$getUniformBlocks() {
        return uniformBlocks;
    }

    @Shadow
    @Final
    private Map<String, GlUniform> uniformsByName;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private List<GlUniform> uniforms;

    @Shadow
    @Final
    private String debugLabel;

    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;glGetProgrami(II)I"))
    private int checkActiveUniforms(
            int program,
            int pname,
            List<RenderPipeline.UniformDescription> uniforms,
            List<String> samplers
    ) {
        IntSet blockUniformIndices = new IntOpenHashSet();
        var activeBlocks = GlStateManager.glGetProgrami(program, GL31.GL_ACTIVE_UNIFORM_BLOCKS);

        try (var stack = MemoryStack.stackPush()) {
            var paramBuffer = stack.mallocInt(1);
            for (var blockIndex = 0; blockIndex < activeBlocks; blockIndex++) {
                var name = GL31.glGetActiveUniformBlockName(program, blockIndex);
                uniformBlocks.add(name);

                GL31.glUniformBlockBinding(program, blockIndex, blockIndex);

                GL31.glGetActiveUniformBlockiv(program, blockIndex,
                        GL31.GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS, paramBuffer);
                var uniformsInBlock = paramBuffer.get(0);

                var uniformIndices = stack.mallocInt(uniformsInBlock);
                GL31.glGetActiveUniformBlockiv(program, blockIndex,
                        GL31.GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES, uniformIndices);

                for (var i = 0; i < uniformsInBlock; i++) {
                    blockUniformIndices.add(uniformIndices.get(i));
                }
            }

            var uniformsSize = GlStateManager.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);

            var sizeBuffer = stack.mallocInt(1);
            var uniformTypeBuf = stack.mallocInt(1);

            for (var uniformIndex = 0; uniformIndex < uniformsSize; uniformIndex++) {
                if (blockUniformIndices.contains(uniformIndex)) {
                    continue;
                }
                var uniformName = GL20.glGetActiveUniform(program, uniformIndex, sizeBuffer, uniformTypeBuf);
                var uniformType = getType(uniformTypeBuf.get(0));
                if (!this.uniformsByName.containsKey(uniformName) && !samplers.contains(uniformName)) {
                    if (uniformType != null) {
                        LOGGER.info("Found unknown but potentially supported uniform {} in {}", uniformName, this.debugLabel);
                        var glUniform2 = new GlUniform(uniformName, uniformType);
                        glUniform2.setLocation(uniformIndex);
                        this.uniforms.add(glUniform2);
                        this.uniformsByName.put(uniformName, glUniform2);
                    } else {
                        LOGGER.warn("Found unknown and unsupported uniform {} in {}", uniformName, this.debugLabel);
                    }
                }
            }
        }

        return 0;
    }
}
