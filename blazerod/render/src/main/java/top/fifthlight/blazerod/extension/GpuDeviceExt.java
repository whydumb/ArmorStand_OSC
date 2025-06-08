package top.fifthlight.blazerod.extension;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.render.VertexBuffer;

import java.util.List;

public interface GpuDeviceExt {

    @NotNull
    VertexBuffer blazerod$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount);
}
