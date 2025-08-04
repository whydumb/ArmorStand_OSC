package top.fifthlight.blazerod.util

import kotlinx.coroutines.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPassImpl
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.Callback
import org.slf4j.LoggerFactory
import java.lang.AutoCloseable
import java.util.concurrent.Executors

sealed class ResourceLoader protected constructor() : AutoCloseable {
    abstract val loadDispatcher: CoroutineDispatcher
    abstract val cpuDispatcher: CoroutineDispatcher
    abstract val ioDispatcher: CoroutineDispatcher

    class Blaze3D : ResourceLoader() {
        override val loadDispatcher: CoroutineDispatcher
            get() = Dispatchers.BlazeRod.Main
        override val cpuDispatcher: CoroutineDispatcher
            get() = Dispatchers.BlazeRod.Main
        override val ioDispatcher: CoroutineDispatcher
            get() = Dispatchers.BlazeRod.Main

        override fun close() {}
    }

    class Async : ResourceLoader() {
        private val executor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform()
                .name("BlazeRod async loading thread")
                .daemon(false)
                .priority(Thread.MIN_PRIORITY)
                .factory()
        )
        override val loadDispatcher = executor.asCoroutineDispatcher()
        private val windowHandle: Long
        private var errorCallback: Callback? = null
        override val cpuDispatcher: CoroutineDispatcher
            get() = Dispatchers.Default
        override val ioDispatcher: CoroutineDispatcher
            get() = Dispatchers.IO

        init {
            val gameWindowHandle = MinecraftClient.getInstance().window.handle

            GLFW.glfwDefaultWindowHints()
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
            val handle = GLFW.glfwCreateWindow(
                1,
                1,
                "BlazeRod async loading window",
                0,
                gameWindowHandle
            )
            if (handle == 0L) {
                error("Failed to create BlazeRod offscreen loading window")
            }
            windowHandle = handle

            runBlocking {
                withContext(loadDispatcher) {
                    GLFW.glfwMakeContextCurrent(windowHandle)
                    GL.createCapabilities()
                    if (RenderPassImpl.IS_DEVELOPMENT) {
                        errorCallback = GLUtil.setupDebugMessageCallback()
                    }
                    asyncLoadThread = Thread.currentThread()
                }
            }
        }

        override fun close() {
            runBlocking(loadDispatcher) {
                errorCallback?.free()
                GLFW.glfwDestroyWindow(windowHandle)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ResourceLoader::class.java)
        private const val FORCE_USE_BLAZE3D_LOADER = false
        private var _instance: ResourceLoader? = null

        @JvmStatic
        @Volatile
        var asyncLoadThread: Thread? = null
            private set
        val instance
            get() = _instance ?: error("ResourceLoader not initialized")

        @JvmStatic
        fun isOnResourceLoadingThread() = Thread.currentThread() == asyncLoadThread

        private fun shouldUseBlaze3DLoader(): Boolean {
            if (FORCE_USE_BLAZE3D_LOADER) {
                return true
            }
            if (System.getProperty("blazerod.resource_loader.force_blaze3d") == "true") {
                return true
            }
            return false
        }

        fun initialize() {
            if (_instance != null) {
                return
            }
            LOGGER.info("Loading BlazeRod resource loader...")
            _instance = if (shouldUseBlaze3DLoader()) {
                LOGGER.info("Using Blaze3D resource loader")
                Blaze3D()
            } else {
                LOGGER.info("Using Async resource loader")
                try {
                    Async()
                } catch (ex: Exception) {
                    LOGGER.error("Failed to initialize Async resource loader, fallback to Blaze3D", ex)
                    Blaze3D()
                }
            }
        }

        fun close() {
            if (_instance == null) {
                return
            }
            LOGGER.info("Closing BlazeRod resource loader...")
            _instance?.close()
        }

        fun finishLoading() {
            if (!isOnResourceLoadingThread()) {
                return
            }
            GL11.glFinish()
        }
    }
}