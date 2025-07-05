package top.fifthlight.blazerod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.blazerod.event.RenderEvents;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;rotate()V", shift = At.Shift.AFTER))
    private static void onFlipFrame(long windowHandle, TracyFrameCapturer tracyFrameCapturer, CallbackInfo ci) {
        RenderEvents.FLIP_FRAME.invoker().onFrameFlipped();
    }
}
