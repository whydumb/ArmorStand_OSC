package top.fifthlight.blazerod.extension;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.model.resource.VertexType;

import java.util.Optional;
import java.util.Set;

public interface RenderPipelineExt {
    @NotNull
    Optional<VertexType> blazerod$getVertexType();

    @NotNull
    Set<String> blazerod$getStorageBuffers();
}