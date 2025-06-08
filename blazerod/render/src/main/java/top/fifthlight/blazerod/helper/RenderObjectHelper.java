package top.fifthlight.blazerod.helper;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.blazerod.extension.internal.RenderObjectExtInternal;
import top.fifthlight.blazerod.render.IndexBuffer;
import top.fifthlight.blazerod.render.VertexBuffer;

import java.util.function.BiConsumer;

public class RenderObjectHelper {
    public static <T> RenderPass.RenderObject<T> create(
            int vertexBufferSlot,
            VertexBuffer vertexBuffer,
            @Nullable IndexBuffer indexBuffer,
            int firstIndex,
            int indexCount,
            @Nullable BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
    ) {
        GpuBuffer indexBufferInner = null;
        VertexFormat.IndexType indexType = null;
        if (indexBuffer != null) {
            indexBufferInner = indexBuffer.getBuffer().getInner();
            indexType = indexBuffer.getType();
        }
        var renderObject = new RenderPass.RenderObject<>(
                vertexBufferSlot,
                null,
                indexBufferInner,
                indexType,
                firstIndex,
                indexCount,
                uniformUploaderConsumer
        );
        ((RenderObjectExtInternal) (Object) renderObject).blazerod$setVertexBuffer(vertexBuffer);
        return renderObject;
    }
}
