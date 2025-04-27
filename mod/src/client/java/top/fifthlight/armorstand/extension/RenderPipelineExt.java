package top.fifthlight.armorstand.extension;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.armorstand.model.VertexType;

import java.util.List;
import java.util.Optional;

public interface RenderPipelineExt {
    @NotNull
    List<String> armorStand$getUniformBuffers();

    @NotNull
    Optional<VertexType> armorStand$getVertexType();
}