package top.fifthlight.renderer.model.gltf.test

import top.fifthlight.renderer.model.gltf.GltfLoader
import java.nio.file.FileSystems
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.time.measureTime

class GlbLoadTest {
    @Test
    fun testLoad() {
        val uri = this.javaClass.classLoader.getResource("AliciaSolid.vrm")!!.toURI()
        if (uri.scheme == "jar") {
            FileSystems.newFileSystem(uri, mapOf("create" to "true"))
        }
        val file = uri.toPath()
        measureTime {
            GltfLoader.loadBinary(file)
        }.let { duration ->
            println("GLTF load time: $duration")
        }
    }
}