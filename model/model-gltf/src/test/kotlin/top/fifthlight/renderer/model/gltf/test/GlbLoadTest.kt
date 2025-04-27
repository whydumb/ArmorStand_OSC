package top.fifthlight.renderer.model.gltf.test

import top.fifthlight.renderer.model.gltf.GltfBinaryLoader
import java.nio.file.FileSystems
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.time.measureTime

class GlbLoadTest {
    @Test
    fun testAlicia() {
        val uri = this.javaClass.classLoader.getResource("AliciaSolid.vrm")!!.toURI()
        if (uri.scheme == "jar") {
            FileSystems.newFileSystem(uri, mapOf("create" to "true"))
        }
        val file = uri.toPath()
        measureTime {
            GltfBinaryLoader.load(file)
        }.let { duration ->
            println("Alicia load time: $duration")
        }
    }

    @Test
    fun testInterpolation() {
        val uri = this.javaClass.classLoader.getResource("InterpolationTest.glb")!!.toURI()
        if (uri.scheme == "jar") {
            FileSystems.newFileSystem(uri, mapOf("create" to "true"))
        }
        val file = uri.toPath()
        measureTime {
            GltfBinaryLoader.load(file)
        }.let { duration ->
            println("Interpolation load time: $duration")
        }
    }
}