package top.fifthlight.armorstand.extension.internal;

import top.fifthlight.armorstand.extension.RenderPipelineBuilderExt;
import top.fifthlight.armorstand.model.VertexType;

import java.util.Optional;

public interface RenderPipelineBuilderExtInternal extends RenderPipelineBuilderExt {
    Optional<VertexType> armorStand$getVertexType();
}
