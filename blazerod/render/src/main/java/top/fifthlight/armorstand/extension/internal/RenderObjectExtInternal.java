package top.fifthlight.armorstand.extension.internal;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.armorstand.extension.RenderObjectExt;
import top.fifthlight.armorstand.render.VertexBuffer;

public interface RenderObjectExtInternal extends RenderObjectExt {
    void armorStand$setVertexBuffer(@NotNull VertexBuffer vertexBuffer);
}
