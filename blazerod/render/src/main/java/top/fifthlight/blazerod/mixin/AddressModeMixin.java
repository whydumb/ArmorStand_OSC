package top.fifthlight.blazerod.mixin;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.TextureFormat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.blazerod.extension.AddressModeExt;

// Some hacky code
@Mixin(AddressMode.class)
public abstract class AddressModeMixin {
    @Shadow
    @Final
    @Mutable
    private static AddressMode[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void onInitialize(CallbackInfo ci) {
        var newValues = new AddressMode[$VALUES.length + 1];
        var nextOrdinal = $VALUES.length;

        var MIRRORED_REPEAT = blazerod$invokeInit("MIRRORED_REPEAT", nextOrdinal);
        AddressModeExt.MIRRORED_REPEAT = MIRRORED_REPEAT;
        newValues[nextOrdinal] = MIRRORED_REPEAT;

        $VALUES = newValues;
    }

    @Invoker("<init>")
    public static AddressMode blazerod$invokeInit(String name, int ordinal) {
        throw new AssertionError();
    }
}
