package top.fifthlight.armorstand

import net.minecraft.client.util.math.MatrixStack
import top.fifthlight.armorstand.state.PlayerModelManager
import java.util.*

object PlayerRenderer {
    @JvmStatic
    fun render(
        uuid: UUID,
        matrixStack: MatrixStack,
        light: Int,
    ): Boolean {
        val instance = PlayerModelManager[uuid]
        if (instance == null) {
            return false
        }

        val backupItem = matrixStack.peek().copy()
        matrixStack.pop()

        instance.render(matrixStack, light)

        matrixStack.push()
        matrixStack.peek().apply {
            positionMatrix.set(backupItem.positionMatrix)
            normalMatrix.set(backupItem.normalMatrix)
        }
        return true
    }
}