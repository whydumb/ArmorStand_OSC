package top.fifthlight.armorstand.extension.internal.gl;

import org.jetbrains.annotations.Nullable;
import top.fifthlight.armorstand.extension.RenderPassExt;
import top.fifthlight.armorstand.render.VertexBuffer;

public interface GlRenderPassImplExtInternal extends RenderPassExt {
    @Nullable
    VertexBuffer armorStand$getVertexBuffer();
}
