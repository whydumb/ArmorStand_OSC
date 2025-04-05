package top.fifthlight.armorstand.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.armorstand.helper.RenderPipelineWithVertexType;
import top.fifthlight.armorstand.model.VertexType;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineMixin implements RenderPipelineWithVertexType {
    @Shadow @Final private VertexFormat.DrawMode vertexFormatMode;
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

    @Inject(method = "getVertexFormatMode", at = @At("HEAD"))
    public void onGetVertexFormatMode(CallbackInfoReturnable<VertexFormat.DrawMode> cir) {
        if (this.vertexFormatMode == null) {
            throw new IllegalStateException("vertexMode is null in RenderPipeline");
        }
    }
}
