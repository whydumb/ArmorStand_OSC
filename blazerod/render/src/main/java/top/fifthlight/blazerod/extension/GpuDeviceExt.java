package top.fifthlight.blazerod.extension;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.util.GpuShaderDataPool;
import top.fifthlight.blazerod.render.VertexBuffer;

import java.util.List;

public interface GpuDeviceExt {
    @NotNull
    GpuShaderDataPool blazerod$getShaderDataPool();

    @NotNull
    VertexBuffer blazerod$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount);

    boolean blazerod$supportSsbo();
}
