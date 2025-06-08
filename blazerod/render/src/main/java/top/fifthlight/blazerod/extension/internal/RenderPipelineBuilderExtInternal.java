package top.fifthlight.blazerod.extension.internal;

import top.fifthlight.blazerod.extension.RenderPipelineBuilderExt;
import top.fifthlight.blazerod.model.VertexType;

import java.util.Optional;

public interface RenderPipelineBuilderExtInternal extends RenderPipelineBuilderExt {
    Optional<VertexType> blazerod$getVertexType();
}
