package top.fifthlight.armorstand.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.armorstand.helper.PlayerEntityRenderStateWithUuid;

import java.util.UUID;

@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateWithUuid {
    @Unique
    private UUID uuid;

    @Override
    public void armorStand$setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID armorStand$getUuid() {
        return uuid;
    }
}