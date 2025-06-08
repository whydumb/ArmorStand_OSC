package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.textures.TextureFormat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.extension.TextureFormatExt;

// Some hacky code
@Mixin(TextureFormat.class)
public abstract class TextureFormatMixin {
    @Shadow
    @Final
    @Mutable
    private static TextureFormat[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void onInitialize(CallbackInfo ci) {
        var newValues = new TextureFormat[$VALUES.length + 5];
        var nextOrdinal = $VALUES.length;

        var RGBA32F = armorstand$invokeInit("RGBA32F", nextOrdinal, 16);
        TextureFormatExt.RGBA32F = RGBA32F;
        newValues[nextOrdinal] = RGBA32F;

        var RGB32F = armorstand$invokeInit("RGB32F", nextOrdinal, 12);
        TextureFormatExt.RGB32F = RGB32F;
        newValues[nextOrdinal] = RGB32F;

        var RG32F = armorstand$invokeInit("RG32F", nextOrdinal, 8);
        TextureFormatExt.RG32F = RG32F;
        newValues[nextOrdinal] = RG32F;

        var R32F = armorstand$invokeInit("R32F", nextOrdinal, 4);
        TextureFormatExt.R32F = R32F;
        newValues[nextOrdinal] = R32F;

        var R32I = armorstand$invokeInit("R32I", nextOrdinal, 4);
        TextureFormatExt.R32I = R32I;
        newValues[nextOrdinal] = R32I;

        $VALUES = newValues;
    }

    @ModifyReturnValue(method = "hasColorAspect", at = @At("RETURN"))
    private boolean onHasColorAspect(boolean original) {
        var format = (TextureFormat) (Object) this;
        return original || format == TextureFormatExt.RGBA32F || format == TextureFormatExt.RGB32F || format == TextureFormatExt.RG32F || format == TextureFormatExt.R32F;
    }

    @Invoker("<init>")
    public static TextureFormat armorstand$invokeInit(String name, int ordinal, int pixelSize) {
        throw new AssertionError();
    }
}
