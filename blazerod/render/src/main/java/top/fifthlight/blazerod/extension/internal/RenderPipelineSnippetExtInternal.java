package top.fifthlight.blazerod.extension.internal;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.extension.RenderPipelineSnippetExt;
import top.fifthlight.blazerod.model.resource.VertexType;

import java.util.Optional;
import java.util.Set;

public interface RenderPipelineSnippetExtInternal extends RenderPipelineSnippetExt {
    void blazerod$setVertexType(@NotNull Optional<VertexType> type);

    void blazerod$setStorageBuffers(@NotNull Set<String> storageBuffers);
}
