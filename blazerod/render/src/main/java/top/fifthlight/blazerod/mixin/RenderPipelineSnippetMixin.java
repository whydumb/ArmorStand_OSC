package top.fifthlight.blazerod.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.blazerod.extension.internal.RenderPipelineSnippetExtInternal;
import top.fifthlight.blazerod.model.VertexType;

import java.util.Optional;

@Mixin(RenderPipeline.Snippet.class)
public abstract class RenderPipelineSnippetMixin implements RenderPipelineSnippetExtInternal {
    @Unique
    Optional<VertexType> vertexType = Optional.empty();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        vertexType = Optional.empty();
    }

    @Override
    public void blazerod$setVertexType(@NotNull Optional<VertexType> type) {
        vertexType = type;
    }

    @Override
    @NotNull
    public Optional<VertexType> blazerod$getVertexType() {
        return vertexType;
    }
}
