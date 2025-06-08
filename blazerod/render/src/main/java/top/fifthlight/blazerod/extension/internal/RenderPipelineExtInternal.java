package top.fifthlight.blazerod.extension.internal;

import top.fifthlight.blazerod.extension.RenderPipelineExt;
import top.fifthlight.blazerod.model.VertexType;

import java.util.Optional;

public interface RenderPipelineExtInternal extends RenderPipelineExt {
    void blazerod$setVertexType(Optional<VertexType> type);
}
