package top.fifthlight.blazerod.mixin;

import com.mojang.blaze3d.systems.RenderPass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.blazerod.extension.internal.RenderObjectExtInternal;
import top.fifthlight.blazerod.render.VertexBuffer;

@Mixin(RenderPass.RenderObject.class)
public abstract class RenderObjectMixin implements RenderObjectExtInternal {
    @Unique
    VertexBuffer vertexBuffer;

    @Override
    @Nullable
    public VertexBuffer blazerod$getVertexBuffer() {
        return vertexBuffer;
    }

    @Override
    public void blazerod$setVertexBuffer(@NotNull VertexBuffer vertexBuffer) {
        if (this.vertexBuffer != null) {
            throw new IllegalStateException("Can't set BlazeRod VertexBuffer to a RenderPass already having a Blaze3D vertex buffer");
        }
        this.vertexBuffer = vertexBuffer;
    }
}
