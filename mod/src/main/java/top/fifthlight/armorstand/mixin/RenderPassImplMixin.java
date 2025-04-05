package top.fifthlight.armorstand.mixin;

import net.minecraft.client.gl.RenderPassImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;

@Mixin(RenderPassImpl.class)
public abstract class RenderPassImplMixin implements RenderPassWithVertexBuffer {
    @Unique
    VertexBuffer vertexBuffer;

    @Override
    public @Nullable VertexBuffer armorStand$getVertexBuffer() {
        return vertexBuffer;
    }

    @Override
    public void armorStand$setVertexBuffer(@NotNull VertexBuffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
    }
}
