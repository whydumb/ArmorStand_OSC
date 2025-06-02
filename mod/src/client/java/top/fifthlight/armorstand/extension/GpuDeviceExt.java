package top.fifthlight.armorstand.extension;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.armorstand.render.VertexBuffer;

import java.util.List;

public interface GpuDeviceExt {

    @NotNull
    VertexBuffer armorStand$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount);
}
