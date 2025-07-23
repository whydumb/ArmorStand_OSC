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
import top.fifthlight.blazerod.model.resource.VertexType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin implements RenderPipelineBuilderExtInternal {
    @Unique
    Optional<VertexType> vertexType;

    @Override
    @Nullable
    public Optional<VertexType> blazerod$getVertexType() {
        return vertexType;
    }

    @Override
    public void blazerod$withVertexType(@NotNull VertexType type) {
        this.vertexType = Optional.of(type);
    }

    // Suppress: the field is actually initialized in the constructor by mixin
    @SuppressWarnings("NotNullFieldNotInitialized")
    @Unique
    @NotNull
    Set<String> storageBuffers;

    @Override
    @NotNull
    public Set<String> blazerod$getStorageBuffers() {
        return storageBuffers;
    }

    @Override
    public void blazerod$withStorageBuffer(@NotNull String name) {
        storageBuffers.add(name);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        vertexType = Optional.empty();
        storageBuffers = new HashSet<>();
    }

    @Inject(method = "withSnippet", at = @At("HEAD"))
    void withSnippet(@NotNull RenderPipeline.Snippet snippet, CallbackInfo ci) {
        var snippetInternal = ((RenderPipelineSnippetExtInternal) (Object) snippet);

        var vertexType = snippetInternal.blazerod$getVertexType();
        if (vertexType.isPresent()) {
            this.vertexType = vertexType;
        }
        this.storageBuffers.addAll(snippetInternal.blazerod$getStorageBuffers());
    }

    @ModifyReturnValue(method = "buildSnippet", at = @At("RETURN"))
    public RenderPipeline.Snippet buildSnippet(@NotNull RenderPipeline.Snippet original) {
        var snippetExt = ((RenderPipelineSnippetExtInternal) (Object) original);
        snippetExt.blazerod$setVertexType(vertexType);
        snippetExt.blazerod$setStorageBuffers(storageBuffers);
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
        var pipelineExt = ((RenderPipelineExtInternal) original);
        pipelineExt.blazerod$setVertexType(vertexType);
        pipelineExt.blazerod$setStorageBuffers(storageBuffers);
        return original;
    }
}
