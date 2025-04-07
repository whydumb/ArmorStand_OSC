package top.fifthlight.armorstand.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.armorstand.helper.RenderPipelineWithVertexType;
import top.fifthlight.armorstand.model.VertexType;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineMixin implements RenderPipelineWithVertexType {
    @Unique
    VertexType vertexType;

    @Override
    public void armorStand$setVertexType(@NotNull VertexType type) {
        vertexType = type;
    }

    @Override
    @Nullable
    public VertexType armorStand$getVertexType() {
        return vertexType;
    }
}
