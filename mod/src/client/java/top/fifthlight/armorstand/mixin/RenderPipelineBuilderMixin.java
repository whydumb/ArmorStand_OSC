package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.helper.RenderPipelineWithVertexType;
import top.fifthlight.armorstand.model.VertexType;

import java.util.Optional;

@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin implements RenderPipelineWithVertexType {
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

    @Inject(method = "withSnippet", at = @At("HEAD"))
    void withSnippet(RenderPipeline.Snippet snippet, CallbackInfo ci) {
        var vertexType = ((RenderPipelineWithVertexType) (Object) snippet).armorStand$getVertexType();
        if (vertexType != null) {
            this.vertexType = vertexType;
        }
    }

    @ModifyReturnValue(method = "buildSnippet", at = @At("RETURN"))
    public RenderPipeline.Snippet buildSnippet(RenderPipeline.Snippet original) {
        ((RenderPipelineWithVertexType) (Object) original).armorStand$setVertexType(vertexType);
        return original;
    }

    @ModifyExpressionValue(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Optional;isEmpty()Z",
                    slice = "builder"
            ),
            slice = {
                    @Slice(
                            id = "builder",
                            from = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;vertexFormat:Ljava/util/Optional;"),
                            to = @At(value = "NEW", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;")
                    )
            }
    )
    public boolean isVertexFormatOrModeEmpty(boolean original) {
        return vertexType == null && original;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Optional;get()Ljava/lang/Object;"
            ),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;vertexFormat:Ljava/util/Optional;", ordinal = 1),
                    to = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;depthBiasScaleFactor:F")
            )
    )
    public <T> T getVertexFormat(Optional<T> instance) {
        return instance.orElse(null);
    }

    @ModifyReturnValue(method = "build", at = @At("RETURN"))
    public RenderPipeline afterBuilt(RenderPipeline original) {
        ((RenderPipelineWithVertexType) original).armorStand$setVertexType(vertexType);
        return original;
    }
}
