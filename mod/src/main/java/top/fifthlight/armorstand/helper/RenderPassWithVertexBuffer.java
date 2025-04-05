package top.fifthlight.armorstand.helper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.armorstand.render.VertexBuffer;

public interface RenderPassWithVertexBuffer {
    void armorStand$setVertexBuffer(@NotNull VertexBuffer vertexBuffer);

    @Nullable
    VertexBuffer armorStand$getVertexBuffer();
}
