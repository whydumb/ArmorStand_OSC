package top.fifthlight.armorstand.extension.internal.gl;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPassImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GlResourceManagerExtInternal {
    void armorStand$drawInstancedBoundObjectWithRenderPass(@NotNull RenderPassImpl pass, int instances, int first, int count, @Nullable VertexFormat.IndexType indexType);
}
