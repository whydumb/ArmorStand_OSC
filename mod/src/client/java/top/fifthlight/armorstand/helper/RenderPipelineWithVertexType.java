package top.fifthlight.armorstand.helper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.armorstand.model.VertexType;

public interface RenderPipelineWithVertexType {
    void armorStand$setVertexType(@NotNull VertexType type);

    @Nullable
    VertexType armorStand$getVertexType();
}