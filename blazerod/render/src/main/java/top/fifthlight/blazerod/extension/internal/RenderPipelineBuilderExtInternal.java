package top.fifthlight.blazerod.extension.internal;

import top.fifthlight.blazerod.extension.RenderPipelineBuilderExt;
import top.fifthlight.blazerod.model.resource.VertexType;

import java.util.Optional;
import java.util.Set;

public interface RenderPipelineBuilderExtInternal extends RenderPipelineBuilderExt {
    Optional<VertexType> blazerod$getVertexType();

    Set<String> blazerod$getStorageBuffers();
}
