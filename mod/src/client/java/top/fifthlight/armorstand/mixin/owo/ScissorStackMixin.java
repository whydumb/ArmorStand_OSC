package top.fifthlight.armorstand.mixin.owo;

import io.wispforest.owo.ui.util.ScissorStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Override these direct GL operation, so they are render backend independent
@Mixin(ScissorStack.class)
public abstract class ScissorStackMixin {
    @Redirect(method = "applyState", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glIsEnabled(I)Z"), require = 0)
    private static boolean overrideScissorTestEnabledWhenApplyState(int cap) {
        return true;
    }

    @Redirect(method = "drawUnclipped", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glIsEnabled(I)Z"), require = 0)
    private static boolean overrideScissorTestEnabledWhenDrawUnclipped(int cap) {
        return true;
    }
}
