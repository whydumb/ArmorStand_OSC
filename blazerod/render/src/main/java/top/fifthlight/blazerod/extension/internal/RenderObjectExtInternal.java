package top.fifthlight.blazerod.extension.internal;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.extension.RenderObjectExt;
import top.fifthlight.blazerod.render.VertexBuffer;

public interface RenderObjectExtInternal extends RenderObjectExt {
    void blazerod$setVertexBuffer(@NotNull VertexBuffer vertexBuffer);
}
