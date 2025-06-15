package top.fifthlight.armorstand

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.blazerod.model.CameraTransform
import top.fifthlight.blazerod.model.RenderCamera
import top.fifthlight.blazerod.model.TaskMap
import java.lang.ref.WeakReference
import java.util.*

object PlayerRenderer {
    private var renderingWorld = false
    private val taskMap = TaskMap()

    private var prevModelItem = WeakReference<ModelInstanceManager.ModelInstanceItem.Model?>(null)
    val selectedCameraIndex = MutableStateFlow<Int?>(null)
    private val _totalCameras = MutableStateFlow<List<RenderCamera>?>(listOf())
    val totalCameras = _totalCameras.asStateFlow()
    private var cameraTransform: CameraTransform? = null

    @JvmStatic
    fun getCurrentCameraTransform(): CameraTransform? {
        cameraTransform?.let { return it }
        val entry = ModelInstanceManager.getSelfItem(load = false) ?: return null
        if (prevModelItem.get() != entry) {
            selectedCameraIndex.value = null
            if (entry is ModelInstanceManager.ModelInstanceItem.Model) {
                _totalCameras.value = entry.instance.scene.cameras
                prevModelItem = WeakReference(entry)
            } else {
                _totalCameras.value = listOf()
            }
            return null
        }
        val selectedIndex = selectedCameraIndex.value ?: return null
        val instance = entry.instance
        instance.updateCamera()

        return instance.modelData.cameraTransforms.getOrNull(selectedIndex).also {
            cameraTransform = it
        } ?: run {
            selectedCameraIndex.value = null
            null
        }
    }

    fun startRenderWorld() {
        renderingWorld = true
    }

    private val matrix = Matrix4f()

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

        matrix.set(matrixStack.peek().positionMatrix)
        matrix.scale(ConfigHolder.config.value.modelScale)
        matrix.mulLocal(RenderSystem.getModelViewStack())
        if (renderingWorld) {
            taskMap.addTask(instance.schedule(matrix, light))
        } else {
            instance.render(matrix, light)
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

    fun endFrame() {
        cameraTransform = null
    }
}
