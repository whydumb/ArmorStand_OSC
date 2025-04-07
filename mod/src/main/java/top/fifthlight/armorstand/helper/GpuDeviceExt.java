package top.fifthlight.armorstand.helper;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.armorstand.render.GpuTextureBuffer;
import top.fifthlight.armorstand.render.TextureBufferFormat;
import top.fifthlight.armorstand.render.VertexBuffer;

import java.util.List;
import java.util.function.Supplier;

public interface GpuDeviceExt {
    enum FillType {
        ZERO_FILLED,
        BYTE_ONE_FILLED,
        FLOAT_ONE_FILLED,
    }

    @NotNull
    GpuBuffer armorStand$createBuffer(@Nullable Supplier<String> labelGetter, BufferType type, BufferUsage usage, int size, FillType fillType);

    @NotNull
    VertexBuffer armorStand$createVertexBuffer(VertexFormat.DrawMode mode, List<VertexBuffer.VertexElement> elements, int verticesCount);

    @NotNull
    GpuTextureBuffer armorStand$createTextureBuffer(@Nullable String label, TextureBufferFormat format, GpuBuffer buffer);
}
