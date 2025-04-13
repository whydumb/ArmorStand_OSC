package top.fifthlight.armorstand.mixin;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.opengl.GlConst;
import org.lwjgl.opengl.GL31C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.armorstand.helper.BufferTypeExt;

@Mixin(GlConst.class)
public class GlConstExt {
    @Inject(method = "toGl(Lcom/mojang/blaze3d/buffers/BufferType;)I", at = @At("HEAD"), cancellable = true)
    private static void onBufferTypeToGl(BufferType type, CallbackInfoReturnable<Integer> cir) {
        if (type == BufferTypeExt.TEXTURE_BUFFER) {
            cir.setReturnValue(GL31C.GL_TEXTURE_BUFFER);
        }
    }
}