package top.fifthlight.armorstand

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.model.TaskMap
import top.fifthlight.armorstand.state.ModelInstanceManager
import java.util.*

object PlayerRenderer {
    private var renderingWorld = false
    private val taskMap = TaskMap()

    fun startRenderWorld() {
        renderingWorld = true
    }

    @JvmStatic
    fun appendPlayer(
        uuid: UUID,
        vanillaState: PlayerEntityRenderState,
        matrixStack: MatrixStack,
        light: Int,
    ): Boolean {
        val entry = ModelInstanceManager.get(uuid, System.currentTimeMillis())
        if (entry !is ModelInstanceManager.Item.Model) {
            return false
        }

        val controller = entry.controller
        val instance = entry.instance

        controller.update(vanillaState)
        controller.apply(instance)
        instance.update()

        val backupItem = matrixStack.peek().copy()
        matrixStack.pop()

        if (uuid == MinecraftClient.getInstance().player?.uuid) {
            val scale = ConfigHolder.config.value.modelScale.toFloat()
            matrixStack.scale(scale, scale, scale)
        }

        if (renderingWorld) {
            instance.schedule(matrixStack, light) { task ->
                taskMap.addTask(task)
            }
        } else {
            instance.render(matrixStack, light)
        }

        matrixStack.push()
        matrixStack.peek().apply {
            positionMatrix.set(backupItem.positionMatrix)
            normalMatrix.set(backupItem.normalMatrix)
        }
        return true
    }

    fun executeDraw(context: WorldRenderContext) {
        renderingWorld = false
        taskMap.executeTasks()
    }
}
