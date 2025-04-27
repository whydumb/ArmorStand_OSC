package top.fifthlight.armorstand.extension;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.armorstand.model.VertexType;

import java.util.List;
import java.util.Optional;

public interface RenderPipelineSnippetExt {
    @NotNull
    Optional<VertexType> armorstand$getVertexType();

    @NotNull
    Optional<List<String>> armorstand$getUniformBuffers();
}
