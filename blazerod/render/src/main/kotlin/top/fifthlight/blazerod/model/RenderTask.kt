package top.fifthlight.blazerod.model

import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.model.data.ModelMatricesBuffer
import top.fifthlight.blazerod.model.data.MorphTargetBuffer
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.util.CowBuffer
import top.fifthlight.blazerod.util.ObjectPool

class RenderTask private constructor(
    private var _instance: ModelInstance? = null,
    private var _light: Int = -1,
    private var _modelViewMatrix: Matrix4f = Matrix4f(),
    private var _modelMatricesBuffer: CowBuffer<ModelMatricesBuffer>? = null,
    private var _skinBuffer: List<CowBuffer<RenderSkinBuffer>>? = null,
    private var _morphTargetBuffer: List<CowBuffer<MorphTargetBuffer>>? = null,
    private var released: Boolean = true,
) {
    val instance: ModelInstance
        get() = checkNotNull(_instance) { "Bad RenderTask" }
    val light: Int
        get() = _light.also { if (it < 0) { throw IllegalStateException("Bad RenderTask") } }
    val modelViewMatrix: Matrix4f
        get() = _modelViewMatrix
    val modelMatricesBuffer: CowBuffer<ModelMatricesBuffer>
        get() = checkNotNull(_modelMatricesBuffer) { "Bad RenderTask" }
    val skinBuffer: List<CowBuffer<RenderSkinBuffer>>
        get() = checkNotNull(_skinBuffer) { "Bad RenderTask" }
    val morphTargetBuffer: List<CowBuffer<MorphTargetBuffer>>
        get() = checkNotNull(_morphTargetBuffer) { "Bad RenderTask" }

    private fun clear() {
        _instance?.decreaseReferenceCount()
        _modelMatricesBuffer?.decreaseReferenceCount()
        _skinBuffer?.forEach { it.decreaseReferenceCount() }
        _morphTargetBuffer?.forEach { it.decreaseReferenceCount() }
        _instance = null
        _light = -1
        _modelViewMatrix.identity()
        _modelMatricesBuffer = null
        _skinBuffer = null
        _morphTargetBuffer = null
    }

    fun release() {
        if (released) {
            return
        }
        POOL.release(this)
        released = true
    }

    companion object {
        private val POOL = ObjectPool<RenderTask>(
            identifier = Identifier.of("blazerod", "render_task"),
            create = ::RenderTask,
            onReleased = {
                clear()
            },
            onClosed = {
                clear()
            },
        )

        @JvmStatic
        fun acquire(
            instance: ModelInstance,
            modelViewMatrix: Matrix4fc,
            light: Int,
            modelMatricesBuffer: CowBuffer<ModelMatricesBuffer>,
            skinBuffer: List<CowBuffer<RenderSkinBuffer>>?,
            morphTargetBuffer: List<CowBuffer<MorphTargetBuffer>>?,
        ) = POOL.acquire().apply {
            instance.increaseReferenceCount()
            modelMatricesBuffer.increaseReferenceCount()
            skinBuffer?.forEach { it.increaseReferenceCount() }
            morphTargetBuffer?.forEach { it.increaseReferenceCount() }
            this._instance = instance
            this._light = light
            this._modelViewMatrix.set(modelViewMatrix)
            this._modelMatricesBuffer = modelMatricesBuffer
            this._skinBuffer = skinBuffer
            this._morphTargetBuffer = morphTargetBuffer
            released = false
        }
    }
}

class TaskMap: AutoCloseable {
    private var closed = false
    private val tasks = mutableMapOf<RenderScene, MutableList<RenderTask>>()

    private fun checkNotClosed() = check(!closed) { "TaskMap is closed" }

    fun addTask(task: RenderTask) {
        checkNotClosed()
        tasks.getOrPut(task.instance.scene) { mutableListOf() }.add(task)
    }

    fun executeTasks(executor: (RenderScene, List<RenderTask>) -> Unit) {
        checkNotClosed()
        for ((scene, tasks) in tasks) {
            if (tasks.size > BlazeRod.INSTANCE_SIZE) {
                for (chunk in tasks.chunked(BlazeRod.INSTANCE_SIZE)) {
                    executor(scene, chunk)
                }
            } else {
                executor(scene, tasks)
            }
            for (task in tasks) {
                task.release()
            }
        }
        tasks.clear()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        for ((_, tasks) in tasks) {
            for (task in tasks) {
                task.release()
            }
        }
        tasks.clear()
    }
}