package top.fifthlight.armorstand.helper;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.armorstand.extension.internal.RenderObjectExtInternal;
import top.fifthlight.armorstand.render.IndexBuffer;
import top.fifthlight.armorstand.render.VertexBuffer;

import java.util.function.Consumer;

public class RenderObjectHelper {
    public static RenderPass.RenderObject create(
            int vertexBufferSlot,
            VertexBuffer vertexBuffer,
            @Nullable IndexBuffer indexBuffer,
            int firstIndex,
            int indexCount,
            @Nullable Consumer<RenderPass.UniformUploader> uniformUploaderConsumer
    ) {
        GpuBuffer indexBufferInner = null;
        VertexFormat.IndexType indexType = null;
        if (indexBuffer != null) {
            indexBufferInner = indexBuffer.getBuffer().getInner();
            indexType = indexBuffer.getType();
        }
        var renderObject = new RenderPass.RenderObject(
                vertexBufferSlot,
                null,
                indexBufferInner,
                indexType,
                firstIndex,
                indexCount,
                uniformUploaderConsumer
        );
        ((RenderObjectExtInternal) (Object) renderObject).armorStand$setVertexBuffer(vertexBuffer);
        return renderObject;
    }
}
