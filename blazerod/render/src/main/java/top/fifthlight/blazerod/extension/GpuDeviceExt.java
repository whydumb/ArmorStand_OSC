package top.fifthlight.blazerod.extension;

import com.mojang.blaze3d.buffers.GpuBuffer;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.util.GpuShaderDataPool;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public interface GpuDeviceExt {
    @NotNull
    GpuShaderDataPool blazerod$getShaderDataPool();

    @NotNull
    GpuBuffer blazerod$createBuffer(@Nullable Supplier<String> labelSupplier, int usage, int extraUsage, int size);

    @NotNull
    GpuBuffer blazerod$createBuffer(@Nullable Supplier<String> labelSupplier, int usage, int extraUsage, ByteBuffer data);

    boolean blazerod$supportSsbo();

    boolean blazerod$supportSsboInVertexShader();
}
