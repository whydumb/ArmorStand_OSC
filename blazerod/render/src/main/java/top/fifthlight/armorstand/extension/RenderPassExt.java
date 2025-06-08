package top.fifthlight.armorstand.extension;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.armorstand.render.VertexBuffer;

public interface RenderPassExt {
    void armorStand$setVertexBuffer(@NotNull VertexBuffer vertexBuffer);
    void armorStand$draw(int baseVertex, int firstIndex, int count, int instanceCount);
}