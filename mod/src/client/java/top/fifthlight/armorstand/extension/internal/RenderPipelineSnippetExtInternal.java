package top.fifthlight.armorstand.extension.internal;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.armorstand.extension.RenderPipelineSnippetExt;
import top.fifthlight.armorstand.model.VertexType;

import java.util.List;
import java.util.Optional;

public interface RenderPipelineSnippetExtInternal extends RenderPipelineSnippetExt {
    void armorStand$setUniformBuffers(@NotNull Optional<List<String>> uniformBuffers);

    void armorStand$setVertexType(@NotNull Optional<VertexType> type);
}
