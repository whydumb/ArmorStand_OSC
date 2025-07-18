package top.fifthlight.armorstand.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.armorstand.extension.internal.PlayerEntityRenderStateExtInternal;

import java.util.UUID;

@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateExtInternal {
    @Unique
    private UUID uuid;

    @Unique
    @Nullable
    private EntityType<?> ridingEntityType;

    @Unique
    private boolean sprinting;

    @Override
    public void armorStand$setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID armorStand$getUuid() {
        return uuid;
    }

    @Override
    public void armorStand$setRidingEntityType(@Nullable EntityType<?> entityType) {
        this.ridingEntityType = entityType;
    }

    @Nullable
    @Override
    public EntityType<?> armorStand$getRidingEntityType() {
        return ridingEntityType;
    }

    @Override
    public void armorStand$setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    @Override
    public boolean armorStand$isSprinting() {
        return sprinting;
    }
}