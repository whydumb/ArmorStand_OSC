package top.fifthlight.renderer.model;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class ByteBufferUtil {
    @NotNull
    public static ByteBuffer slice(ByteBuffer buffer, int index, int length) {
        return buffer.slice(index, length);
    }

    @NotNull
    public static ByteBuffer put(ByteBuffer buffer, int index, ByteBuffer src, int offset, int length) {
        return buffer.put(index, src, offset, length);
    }
}
