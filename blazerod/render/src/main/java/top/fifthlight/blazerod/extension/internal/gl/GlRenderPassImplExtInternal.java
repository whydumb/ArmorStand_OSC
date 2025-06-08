package top.fifthlight.blazerod.extension.internal.gl;

import org.jetbrains.annotations.Nullable;
import top.fifthlight.blazerod.extension.RenderPassExt;
import top.fifthlight.blazerod.render.VertexBuffer;

public interface GlRenderPassImplExtInternal extends RenderPassExt {
    @Nullable
    VertexBuffer blazerod$getVertexBuffer();
}
