package top.fifthlight.armorstand.model

import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.util.ObjectPool

sealed class RenderTask<T: RenderTask<T, K>, K: Any>: AutoCloseable {
    abstract val type: Type<T, K>
    abstract val key: K
    abstract override fun close()

    sealed class Type<T: RenderTask<T, K>, K: Any> {
        abstract fun createMap(): MutableMap<K, MutableList<T>>
        abstract fun createList(): MutableList<T>

        @Suppress("UNCHECKED_CAST")
        @JvmName("executeFallback")
        fun execute(key: Any, tasks: List<Any>) = execute(key as K, tasks as List<T>)

        abstract fun execute(key: K, tasks: List<T>)

        data object Instance: Type<RenderTask.Instance, RenderScene>() {
            override fun createMap() = mutableMapOf<RenderScene, MutableList<RenderTask.Instance>>()
            override fun createList() = mutableListOf<RenderTask.Instance>()

            override fun execute(key: RenderScene, tasks: List<RenderTask.Instance>) {
                tasks.chunked(ArmorStandClient.INSTANCE_SIZE) { chunk ->
                    key.renderInstanced(chunk)
                }
            }
        }
    }

    class Instance private constructor(): RenderTask<Instance, RenderScene>(), AutoCloseable {
        override val type: Type<Instance, RenderScene>
            get() = Type.Instance
        override val key: RenderScene
            get() = instance.scene
        private var _instance: ModelInstance? = null
        var instance: ModelInstance
            get() = _instance!!
            set(value) { _instance = value }
        val viewModelMatrix: Matrix4f = Matrix4f()
        var light: Int = -1

        override fun close() {
            POOL.release(this)
        }

        companion object {
            private val POOL = ObjectPool<Instance>(
                identifier = Identifier.of("armorstand", "mesh"),
                create = ::Instance,
                onReleased = {
                    _instance = null
                    light = -1
                }
            )

            fun acquire(
                instance: ModelInstance,
                modelMatrix: Matrix4fc,
                light: Int,
            ) = POOL.acquire().apply {
                this.instance = instance
                this.viewModelMatrix.set(modelMatrix)
                this.light = light
            }
        }
    }
}

class TaskMap {
    private val taskTypeMap = mutableMapOf<RenderTask.Type<*, *>, MutableMap<Any, MutableList<RenderTask<*, *>>>>()

    fun addTask(task: RenderTask<*, *>) {
        val taskMap = taskTypeMap.getOrPut(task.type) {
            @Suppress("UNCHECKED_CAST")
            task.type.createMap() as MutableMap<Any, MutableList<RenderTask<*, *>>>
        }
        val taskList = taskMap.getOrPut(task.key) {
            task.type.createList() as MutableList<RenderTask<*, *>>
        }
        taskList.add(task)
    }

    fun executeTasks() {
        for ((type, taskMap) in taskTypeMap) {
            for ((key, tasks) in taskMap) {
                type.execute(key, tasks)
                for (task in tasks) {
                    task.close()
                }
            }
        }
        taskTypeMap.clear()
    }
}