package top.fifthlight.blazerod.extension.internal.gl;

import top.fifthlight.blazerod.extension.CommandEncoderExt.ClearType;

public interface BufferManagerExtInternal {
    boolean blazerod$isGlClearBufferObjectEnabled();
    void blazerod$clearBufferData(int buffer, int offset, int size, ClearType clearType);
    void blazerod$copyBuffer(int target, int source, int targetOffset, int sourceOffset, int size);
}