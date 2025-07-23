package top.fifthlight.blazerod.extension.internal;

import top.fifthlight.blazerod.extension.RenderPipelineExt;
import top.fifthlight.blazerod.model.resource.VertexType;

import java.util.Optional;
import java.util.Set;

public interface RenderPipelineExtInternal extends RenderPipelineExt {
    void blazerod$setVertexType(Optional<VertexType> type);

    void blazerod$setStorageBuffers(Set<String> storageBuffers);
}
