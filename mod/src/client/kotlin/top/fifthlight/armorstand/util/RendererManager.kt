package top.fifthlight.armorstand.util

import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.config.GlobalConfig
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.blazerod.model.renderer.*

object RendererManager : AutoCloseable {
    private var closed = false

    private fun requireNotClosed() = check(!closed) { "RendererManager is closed" }

    private fun getConfigRendererType() = when (ConfigHolder.config.value.renderer) {
        GlobalConfig.Renderer.VERTEX_SHADER_TRANSFORM -> VertexShaderTransformRenderer.Type
        GlobalConfig.Renderer.CPU_TRANSFORM -> CpuTransformRenderer.Type
        GlobalConfig.Renderer.COMPUTE_SHADER_TRANSFORM -> ComputeShaderTransformRenderer.Type
    }

    private var _currentRenderer: Renderer<*, *>? = null
    private var currentRendererSupportInstancing = false
    val currentRenderer: Renderer<*, *>
        get() {
            requireNotClosed()
            return _currentRenderer ?: changeRenderer(getConfigRendererType())
        }
    val currentRendererInstanced: InstancedRenderer<*, *>?
        get() {
            requireNotClosed()
            return if (currentRendererSupportInstancing) {
                currentRenderer as InstancedRenderer<*, *>
            } else {
                null
            }
        }

    private fun changeRenderer(type: Renderer.Type<*, *>): Renderer<*, *> {
        val renderer = type.create()
        if (_currentRenderer != null) {
            ModelInstanceManager.cleanAll()
        }
        _currentRenderer = renderer
        currentRendererSupportInstancing = renderer.type.supportInstancing
        return renderer
    }

    fun rotate() {
        _currentRenderer?.rotate()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        _currentRenderer?.close()
    }
}