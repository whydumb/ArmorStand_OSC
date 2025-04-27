package top.fifthlight.armorstand.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.armorstand.extension.internal.RenderPipelineExtInternal;
import top.fifthlight.armorstand.model.VertexType;

import java.util.List;
import java.util.Optional;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineMixin implements RenderPipelineExtInternal {
    @Unique
    private List<String> uniformBuffers;

    @Unique
    private Optional<VertexType> vertexType;

    @Override
    @NotNull
    public Optional<VertexType> armorStand$getVertexType() {
        return vertexType;
    }

    @Override
    public void armorStand$setVertexType(@NotNull Optional<VertexType> type) {
        vertexType = type;
    }

    @Override
    @NotNull
    public List<String> armorStand$getUniformBuffers() {
        return uniformBuffers;
    }

    @Override
    public void armorStand$setUniformBuffers(List<String> uniformBuffers) {
        this.uniformBuffers = uniformBuffers;
    }
}
