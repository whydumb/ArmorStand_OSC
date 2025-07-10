package top.fifthlight.blazerod.mixin.gl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.gl.BufferManager;
import org.lwjgl.opengl.ARBClearBufferObject;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.blazerod.extension.CommandEncoderExt;
import top.fifthlight.blazerod.extension.internal.gl.BufferManagerExtInternal;
import top.fifthlight.blazerod.render.gl.ClearTypeParam;

import java.util.Set;

@Mixin(BufferManager.class)
public abstract class BufferManagerMixin implements BufferManagerExtInternal {
    @Unique
    private static final boolean allowGlClearBufferObject = true;

    @Unique
    private static boolean glClearBufferObjectEnabled = false;

    @Inject(method = "create(Lorg/lwjgl/opengl/GLCapabilities;Ljava/util/Set;)Lnet/minecraft/client/gl/BufferManager;", at = @At("HEAD"))
    private static void onCreate(GLCapabilities capabilities, Set<String> usedCapabilities, CallbackInfoReturnable<BufferManager> cir) {
        if (capabilities.GL_ARB_clear_buffer_object && allowGlClearBufferObject) {
            usedCapabilities.add("GL_ARB_clear_buffer_object");
            glClearBufferObjectEnabled = true;
        }
    }

    @Override
    public boolean blazerod$isGlClearBufferObjectEnabled() {
        return glClearBufferObjectEnabled;
    }

    @Mixin(BufferManager.ARBBufferManager.class)
    private abstract static class ARBBufferManagerMixin implements BufferManagerExtInternal {
        @Override
        public void blazerod$clearBufferData(int buffer, int offset, int size, CommandEncoderExt.ClearType clearType) {
            if (!blazerod$isGlClearBufferObjectEnabled()) {
                throw new IllegalStateException("Clear buffer when GL_ARB_clear_buffer_object is not supported");
            }
            var param = ClearTypeParam.fromClearType(clearType);
            if (size % param.getAlign() != 0) {
                throw new IllegalArgumentException("Bad clear byte length " + size + " for clear type " + clearType);
            }
            ARBDirectStateAccess.glClearNamedBufferSubData(buffer, param.getInternalFormat(), offset, size, param.getFormat(), param.getType(), param.getData());
        }
    }

    @Mixin(BufferManager.DefaultBufferManager.class)
    private abstract static class DefaultBufferManagerMixin implements BufferManagerExtInternal {
        @Override
        public void blazerod$clearBufferData(int buffer, int offset, int size, CommandEncoderExt.ClearType clearType) {
            if (!blazerod$isGlClearBufferObjectEnabled()) {
                throw new IllegalStateException("Clear buffer when GL_ARB_clear_buffer_object is not supported");
            }
            var param = ClearTypeParam.fromClearType(clearType);
            if (size % param.getAlign() != 0) {
                throw new IllegalArgumentException("Bad clear byte length " + size + " for clear type " + clearType);
            }
            GlStateManager._glBindBuffer(GlConst.GL_COPY_WRITE_BUFFER, buffer);
            ARBClearBufferObject.glClearBufferSubData(GlConst.GL_COPY_WRITE_BUFFER, param.getInternalFormat(), offset, size, param.getFormat(), param.getType(), param.getData());
            GlStateManager._glBindBuffer(GlConst.GL_COPY_WRITE_BUFFER, 0);
        }
    }
}
