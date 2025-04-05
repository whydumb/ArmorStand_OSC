package top.fifthlight.armorstand

import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import org.joml.Quaternionf

object PlayerRenderer {
    @JvmStatic
    fun render(
        livingEntityRenderState: PlayerEntityRenderState,
        matrixStack: MatrixStack,
        light: Int,
    ): Boolean {
        val model = ModelHolder.model
        if (model == null) {
            return false
        }

        model.update()

        val backupItem = matrixStack.peek().copy()
        matrixStack.pop()

        matrixStack.push()
        val scale = livingEntityRenderState.baseScale
        matrixStack.scale(scale, scale, scale)
        matrixStack.multiply(Quaternionf().setAngleAxis(Math.toRadians(180.0 - livingEntityRenderState.bodyYaw.toDouble()), 0.0, 1.0, 0.0))
        model.render(matrixStack, light)
        matrixStack.pop()

        matrixStack.push()
        matrixStack.peek().apply {
            positionMatrix.set(backupItem.positionMatrix)
            normalMatrix.set(backupItem.normalMatrix)
        }
        return true
    }
}