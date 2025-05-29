package top.fifthlight.armorstand

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import top.fifthlight.armorstand.model.TaskMap
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.armorstand.util.FramedObjectPool
import java.util.*

object PlayerRenderer {
    private var renderingWorld = false
    private val taskMap = TaskMap()

    fun flipObjectPools() {
        FramedObjectPool.frame()
    }

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
        val entry = ModelInstanceManager.get(uuid, System.nanoTime())
        if (entry !is ModelInstanceManager.ModelInstanceItem.Model) {
            return false
        }

        val controller = entry.controller
        val instance = entry.instance

        controller.update(uuid, vanillaState)
        controller.apply(instance)
        instance.update()

        val backupItem = matrixStack.peek().copy()
        matrixStack.pop()
        matrixStack.push()

        val modelMatrix = matrixStack.peek().positionMatrix

        modelMatrix.mulLocal(RenderSystem.getModelViewStack())
        if (renderingWorld) {
            taskMap.addTask(instance.schedule(modelMatrix, light))
        } else {
            instance.render(modelMatrix, light)
        }

        matrixStack.pop()
        matrixStack.push()
        matrixStack.peek().apply {
            positionMatrix.set(backupItem.positionMatrix)
            normalMatrix.set(backupItem.normalMatrix)
        }
        return true
    }

    fun executeDraw() {
        renderingWorld = false
        taskMap.executeTasks()
    }
}
