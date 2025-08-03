package top.fifthlight.blazerod.extension.internal;

import net.minecraft.client.gl.GlBackend;
import top.fifthlight.blazerod.extension.CommandEncoderExt;
import top.fifthlight.blazerod.render.gl.ComputePassImpl;

public interface CommandEncoderExtInternal extends CommandEncoderExt {
    GlBackend blazerod$getBackend();

    void blazerod$dispatchCompute(ComputePassImpl pass, int x, int y, int z);
}
