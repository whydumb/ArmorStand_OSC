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
import top.fifthlight.armorstand.extension.internal.RenderPipelineBuilderExtInternal;
import top.fifthlight.armorstand.extension.internal.RenderPipelineExtInternal;
import top.fifthlight.armorstand.extension.internal.RenderPipelineSnippetExtInternal;
import top.fifthlight.armorstand.model.VertexType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin implements RenderPipelineBuilderExtInternal {
    @Unique
    Optional<VertexType> vertexType;
    @Unique
    Optional<List<String>> uniformBuffers;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        vertexType = Optional.empty();
        uniformBuffers = Optional.empty();
    }

    @Override
    @Nullable
    public Optional<VertexType> armorStand$getVertexType() {
        return vertexType;
    }

    @Override
    public void armorStand$withVertexType(@NotNull VertexType type) {
        this.vertexType = Optional.of(type);
    }

    @Override
    public void armorStand$withUniformBuffer(@NotNull String name) {
        if (uniformBuffers.isEmpty()) {
            uniformBuffers = Optional.of(new ArrayList<>());
        }
        uniformBuffers.get().add(name);
    }

    @Inject(method = "withSnippet", at = @At("HEAD"))
    void withSnippet(@NotNull RenderPipeline.Snippet snippet, CallbackInfo ci) {
        var snippetInternal = ((RenderPipelineSnippetExtInternal) (Object) snippet);

        var vertexType = snippetInternal.armorstand$getVertexType();
        if (vertexType.isPresent()) {
            this.vertexType = vertexType;
        }
        snippetInternal.armorstand$getUniformBuffers()
                .ifPresent(uniformBuffers -> {
                    if (this.uniformBuffers.isPresent()) {
                        this.uniformBuffers.get().addAll(uniformBuffers);
                    } else {
                        this.uniformBuffers = Optional.of(new ArrayList<>(uniformBuffers));
                    }
                });
    }

    @ModifyReturnValue(method = "buildSnippet", at = @At("RETURN"))
    public RenderPipeline.Snippet buildSnippet(@NotNull RenderPipeline.Snippet original) {
        var snippetExt = ((RenderPipelineSnippetExtInternal) (Object) original);
        snippetExt.armorStand$setVertexType(vertexType);
        snippetExt.armorStand$setUniformBuffers(uniformBuffers.map(Collections::unmodifiableList));
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
        ((RenderPipelineExtInternal) original).armorStand$setVertexType(vertexType);
        ((RenderPipelineExtInternal) original).armorStand$setUniformBuffers(uniformBuffers.orElse(Collections.emptyList()));
        return original;
    }
}
