package top.fifthlight.armorstand.mixin.gl;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import top.fifthlight.armorstand.extension.TextureFormatExt;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.textures.TextureFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlConst.class)
public class GlConstMixin {
    @Inject(method = "toGlInternalId(Lcom/mojang/blaze3d/textures/TextureFormat;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlInternalId(TextureFormat textureFormat, CallbackInfoReturnable<Integer> cir) {
        if (textureFormat == TextureFormatExt.RGBA32F) {
            cir.setReturnValue(GL30C.GL_RGBA32F);
        } else if (textureFormat == TextureFormatExt.RGB32F) {
            cir.setReturnValue(GL30C.GL_RGB32F);
        } else if (textureFormat == TextureFormatExt.RG32F) {
            cir.setReturnValue(GL30C.GL_RG32F);
        } else if (textureFormat == TextureFormatExt.R32F) {
            cir.setReturnValue(GL30C.GL_R32F);
        } else if (textureFormat == TextureFormatExt.R32I) {
            cir.setReturnValue(GL30C.GL_R32I);
        }
    }

    @Inject(method = "toGlExternalId(Lcom/mojang/blaze3d/textures/TextureFormat;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlExternalId(TextureFormat textureFormat, CallbackInfoReturnable<Integer> cir) {
        if (textureFormat == TextureFormatExt.RGBA32F) {
            cir.setReturnValue(GL11C.GL_RGBA);
        } else if (textureFormat == TextureFormatExt.RGB32F) {
            cir.setReturnValue(GL11C.GL_RGB);
        } else if (textureFormat == TextureFormatExt.RG32F) {
            cir.setReturnValue(GL30C.GL_RG);
        } else if (textureFormat == TextureFormatExt.R32F) {
            cir.setReturnValue(GL11C.GL_RED);
        } else if (textureFormat == TextureFormatExt.R32I) {
            cir.setReturnValue(GL30C.GL_RED);
        }
    }

    @Inject(method = "toGlType(Lcom/mojang/blaze3d/textures/TextureFormat;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlType(TextureFormat textureFormat, CallbackInfoReturnable<Integer> cir) {
        if (textureFormat == TextureFormatExt.RGBA32F) {
            cir.setReturnValue(GL11C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.RGB32F) {
            cir.setReturnValue(GL11C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.RG32F) {
            cir.setReturnValue(GL30C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.R32F) {
            cir.setReturnValue(GL30C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.R32I) {
            cir.setReturnValue(GL30C.GL_INT);
        }
    }
}
