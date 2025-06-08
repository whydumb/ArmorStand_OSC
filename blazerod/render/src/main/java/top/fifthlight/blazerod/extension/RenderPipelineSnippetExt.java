package top.fifthlight.blazerod.extension;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.model.VertexType;

import java.util.Optional;

public interface RenderPipelineSnippetExt {
    @NotNull
    Optional<VertexType> blazerod$getVertexType();
}
