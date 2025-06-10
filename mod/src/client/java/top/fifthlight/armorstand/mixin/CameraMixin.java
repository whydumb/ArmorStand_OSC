package top.fifthlight.armorstand.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.fifthlight.armorstand.config.ConfigHolder;
import top.fifthlight.armorstand.config.GlobalConfig;

@Mixin(Camera.class)
public class CameraMixin {
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"))
    public float thirdPersonDistance(float f) {
        return (float) (f * ConfigHolder.INSTANCE.getConfig().getValue().getThirdPersonDistanceScale());
    }
}
