package top.fifthlight.blazerod.example.ballblock;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

// Shamelessly copied from https://docs.fabricmc.net/zh_cn/develop/blocks/block-entity-renderer
public class BallBlockEntityRenderer implements BlockEntityRenderer<BallBlockEntity> {
    public BallBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    private final Matrix4f matrix = new Matrix4f();

    @Override
    public void render(BallBlockEntity entity, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
        var instance = BallBlockMod.getBallInstance();
        if (instance == null) {
            return;
        }
        matrix.set(matrices.peek().getPositionMatrix());
        matrix.mulLocal(RenderSystem.getModelViewStack());
        instance.render(matrix, light);
    }
}
