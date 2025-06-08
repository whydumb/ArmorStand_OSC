package top.fifthlight.blazerod.mixin;

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
import top.fifthlight.blazerod.extension.internal.RenderPipelineBuilderExtInternal;
import top.fifthlight.blazerod.extension.internal.RenderPipelineExtInternal;
import top.fifthlight.blazerod.extension.internal.RenderPipelineSnippetExtInternal;
import top.fifthlight.blazerod.model.VertexType;

import java.util.Optional;

@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin implements RenderPipelineBuilderExtInternal {
    @Unique
    Optional<VertexType> vertexType;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        vertexType = Optional.empty();
    }

    @Override
    @Nullable
    public Optional<VertexType> blazerod$getVertexType() {
        return vertexType;
    }

    @Override
    public void blazerod$withVertexType(@NotNull VertexType type) {
        this.vertexType = Optional.of(type);
    }

    @Inject(method = "withSnippet", at = @At("HEAD"))
    void withSnippet(@NotNull RenderPipeline.Snippet snippet, CallbackInfo ci) {
        var snippetInternal = ((RenderPipelineSnippetExtInternal) (Object) snippet);

        var vertexType = snippetInternal.blazerod$getVertexType();
        if (vertexType.isPresent()) {
            this.vertexType = vertexType;
        }
    }

    @ModifyReturnValue(method = "buildSnippet", at = @At("RETURN"))
    public RenderPipeline.Snippet buildSnippet(@NotNull RenderPipeline.Snippet original) {
        var snippetExt = ((RenderPipelineSnippetExtInternal) (Object) original);
        snippetExt.blazerod$setVertexType(vertexType);
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
        return vertexType.isEmpty() && original;
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
        ((RenderPipelineExtInternal) original).blazerod$setVertexType(vertexType);
        return original;
    }
}
