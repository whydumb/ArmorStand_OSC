package top.fifthlight.armorstand.mixin;

import com.mojang.blaze3d.buffers.BufferType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.helper.BufferTypeExt;

// Some hacky code
@Mixin(BufferType.class)
public abstract class BufferTypeMixin {
    @Shadow
    @Final
    @Mutable
    private static BufferType[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void onInitialize(CallbackInfo ci) {
        var newValues = new BufferType[$VALUES.length + 1];
        var nextOrdinal = $VALUES.length;

        var TEXTURE_BUFFER = armorstand$invokeInit("TEXTURE_BUFFER", nextOrdinal);
        BufferTypeExt.TEXTURE_BUFFER = TEXTURE_BUFFER;
        newValues[nextOrdinal] = TEXTURE_BUFFER;
        // nextOrdinal++;

        $VALUES = newValues;
    }

    @Invoker("<init>")
    public static BufferType armorstand$invokeInit(String name, int internalId) {
        throw new AssertionError();
    }
}