package top.fifthlight.armorstand.extension.internal;

import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerEntityRenderStateExtInternal {
    void armorStand$setUuid(UUID uuid);
    UUID armorStand$getUuid();

    void armorStand$setRidingEntityType(@Nullable EntityType<?> ridingEntityType);

    @Nullable
    EntityType<?> armorStand$getRidingEntityType();

    void armorStand$setSprinting(boolean sprinting);

    boolean armorStand$isSprinting();
}
