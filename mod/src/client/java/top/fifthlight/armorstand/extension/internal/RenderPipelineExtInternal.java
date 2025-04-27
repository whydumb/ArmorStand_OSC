package top.fifthlight.armorstand.extension.internal;

import top.fifthlight.armorstand.extension.RenderPipelineExt;
import top.fifthlight.armorstand.model.VertexType;

import java.util.List;
import java.util.Optional;

public interface RenderPipelineExtInternal extends RenderPipelineExt {
    void armorStand$setVertexType(Optional<VertexType> type);
    void armorStand$setUniformBuffers(List<String> uniformBuffers);
}
