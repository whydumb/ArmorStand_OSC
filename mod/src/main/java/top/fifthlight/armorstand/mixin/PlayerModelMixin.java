package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.fifthlight.armorstand.PlayerRenderer;
import top.fifthlight.armorstand.helper.PlayerEntityRenderStateWithUuid;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerModelMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    @Nullable
    public
    <T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
    RenderLayer render(LivingEntityRenderer<T, S, M> instance,
                       S state,
                       boolean showBody,
                       boolean translucent,
                       boolean showOutline,
                       Operation<RenderLayer> original,
                       S livingEntityRenderState,
                       MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider,
                       int light
    ) {
        if (!(state instanceof PlayerEntityRenderState)) {
            return original.call(instance, state, showBody, translucent, showOutline);
        }
        var uuid = ((PlayerEntityRenderStateWithUuid) state).armorStand$getUuid();
        if (uuid == null) {
            return original.call(instance, state, showBody, translucent, showOutline);
        }
        @SuppressWarnings("unchecked")
        var entityRenderer = ((EntityRenderer<T, S>) (Object) this);
        var textRenderer = entityRenderer.getTextRenderer();
        if (!PlayerRenderer.render(uuid, (PlayerEntityRenderState) state, matrixStack, light, vertexConsumerProvider, textRenderer, entityRenderer.dispatcher)) {
            return original.call(instance, state, showBody, translucent, showOutline);
        }
        return null;
    }
}
