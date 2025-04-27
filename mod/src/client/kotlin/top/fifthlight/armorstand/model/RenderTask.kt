package top.fifthlight.armorstand.model

import net.minecraft.util.Identifier
import org.joml.Matrix4f
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

        data object Primitive: Type<RenderTask.Primitive, RenderPrimitive>() {
            override fun createMap() = mutableMapOf<RenderPrimitive, MutableList<RenderTask.Primitive>>()
            override fun createList() = mutableListOf<RenderTask.Primitive>()

            override fun execute(key: RenderPrimitive, tasks: List<RenderTask.Primitive>) {
                key.renderInstanced(tasks)
            }
        }
    }

    data class Primitive(
        var primitive: RenderPrimitive? = null,
        var skinData: RenderSkinData? = null,
        var modelViewProjMatrix: Matrix4f = Matrix4f(),
        var modelViewMatrix: Matrix4f = Matrix4f(),
        var light: Int = 0,
    ): RenderTask<Primitive, RenderPrimitive>(), AutoCloseable {
        override val type: Type<Primitive, RenderPrimitive>
            get() = Type.Primitive
        override val key: RenderPrimitive
            get() = primitive!!

        override fun close() {
            POOL.release(this)
        }

        companion object {
            private val POOL = ObjectPool<Primitive>(
                identifier = Identifier.of("armorstand", "mesh"),
                create = ::Primitive,
                onReleased = {
                    primitive = null
                    skinData = null
                }
            )

            fun acquire() = POOL.acquire()
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