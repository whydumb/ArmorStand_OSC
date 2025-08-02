package top.fifthlight.blazerod.mixin.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.jtracy.Plot;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.blazerod.util.ResourceLoader;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    @Unique
    private static final AtomicInteger numTexturesAtomic = new AtomicInteger(0);
    @Unique
    private static final AtomicInteger numBuffersAtomic = new AtomicInteger(0);
    @Shadow
    private static int numTextures;
    @Shadow
    private static int numBuffers;
    @Shadow
    @Final
    private static Plot PLOT_TEXTURES;
    @Shadow
    @Final
    private static Plot PLOT_BUFFERS;
    @Unique
    private static int asyncActiveTexture;

    @Shadow
    @Final
    private static GlStateManager.Texture2DState[] TEXTURES;
    @Unique
    private static GlStateManager.Texture2DState[] ASYNC_TEXTURES;

    @Inject(method = "<clinit>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;TEXTURES:[Lcom/mojang/blaze3d/opengl/GlStateManager$Texture2DState;", shift = At.Shift.AFTER))
    private static void onInitTextures(CallbackInfo ci) {
        ASYNC_TEXTURES = IntStream.range(0, TEXTURES.length)
                .mapToObj(i -> new GlStateManager.Texture2DState())
                .toArray(GlStateManager.Texture2DState[]::new);
    }

    @Inject(method = "_genTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/jtracy/Plot;setValue(D)V", shift = At.Shift.AFTER))
    private static void onGenerateTexture(CallbackInfoReturnable<Integer> cir) {
        var textures = numTexturesAtomic.incrementAndGet();
        numTextures = textures;
        PLOT_TEXTURES.setValue(textures);
    }

    @Inject(method = "_bindTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V"), cancellable = true)
    private static void onBindTexture(int i, CallbackInfo ci) {
        if (!ResourceLoader.isOnResourceLoadingThread()) {
            return;
        }
        if (i != ASYNC_TEXTURES[asyncActiveTexture].boundTexture) {
            ASYNC_TEXTURES[asyncActiveTexture].boundTexture = i;
            GL11.glBindTexture(3553, i);
        }
        ci.cancel();
    }

    @Inject(method = "_activeTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V"), cancellable = true)
    private static void onActiveTexture(int i, CallbackInfo ci) {
        if (!ResourceLoader.isOnResourceLoadingThread()) {
            return;
        }
        if (asyncActiveTexture != i - GL13C.GL_TEXTURE0) {
            asyncActiveTexture = i - GL13C.GL_TEXTURE0;
            GL13.glActiveTexture(i);
        }
        ci.cancel();
    }

    @Inject(method = "_getActiveTexture", at = @At("HEAD"), cancellable = true)
    private static void onGetActiveTexture(CallbackInfoReturnable<Integer> cir) {
        if (!ResourceLoader.isOnResourceLoadingThread()) {
            return;
        }
        cir.setReturnValue(asyncActiveTexture + GL13C.GL_TEXTURE0);
    }

    @Inject(method = "_deleteTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V"), cancellable = true)
    private static void onDeleteTexture(int i, CallbackInfo ci) {
        if (!ResourceLoader.isOnResourceLoadingThread()) {
            return;
        }

        GL11.glDeleteTextures(i);

        for (var texture2DState : ASYNC_TEXTURES) {
            if (texture2DState.boundTexture == i) {
                texture2DState.boundTexture = -1;
            }
        }

        var textures = numTexturesAtomic.decrementAndGet();
        numTextures = textures;
        PLOT_TEXTURES.setValue(textures);
        ci.cancel();
    }

    @Inject(method = "_glGenBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/jtracy/Plot;setValue(D)V", shift = At.Shift.AFTER))
    private static void onGenerateBuffer(CallbackInfoReturnable<Integer> cir) {
        var buffers = numBuffersAtomic.incrementAndGet();
        numBuffers = buffers;
        PLOT_BUFFERS.setValue(buffers);
    }

    @Inject(method = "_glDeleteBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V"), cancellable = true)
    private static void onGlDeleteBuffers(int i, CallbackInfo ci) {
        if (!ResourceLoader.isOnResourceLoadingThread()) {
            return;
        }

        var buffers = numBuffersAtomic.decrementAndGet();
        numBuffers = buffers;
        PLOT_BUFFERS.setValue(buffers);
        GL15.glDeleteBuffers(i);
        ci.cancel();
    }
}