package top.fifthlight.renderer.model.vmd.test

import top.fifthlight.renderer.model.vmd.VmdLoader
import java.nio.file.FileSystems
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.time.measureTime

class VmdLoadTest {
    @Test
    fun testLoad() {
        val uri = this.javaClass.classLoader.getResource("animation.vmd")!!.toURI()
        if (uri.scheme == "jar") {
            FileSystems.newFileSystem(uri, mapOf("create" to "true"))
        }
        val file = uri.toPath()
        measureTime {
            VmdLoader.load(file)
        }.let { duration ->
            println("VMD load time: $duration")
        }
    }
}