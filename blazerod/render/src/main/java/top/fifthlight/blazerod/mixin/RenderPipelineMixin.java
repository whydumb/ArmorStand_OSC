package top.fifthlight.blazerod.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.blazerod.extension.internal.RenderPipelineExtInternal;
import top.fifthlight.blazerod.model.resource.VertexType;

import java.util.Optional;
import java.util.Set;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineMixin implements RenderPipelineExtInternal {
    @Unique
    private Optional<VertexType> vertexType;

    @Override
    @NotNull
    public Optional<VertexType> blazerod$getVertexType() {
        return vertexType;
    }

    @Override
    public void blazerod$setVertexType(@NotNull Optional<VertexType> type) {
        vertexType = type;
    }

    @Unique
    private Set<String> storageBuffers;

    @Override
    @NotNull
    public Set<String> blazerod$getStorageBuffers() {
        return storageBuffers;
    }

    @Override
    public void blazerod$setStorageBuffers(@NotNull Set<String> storageBuffers) {
        this.storageBuffers = storageBuffers;
    }
}
