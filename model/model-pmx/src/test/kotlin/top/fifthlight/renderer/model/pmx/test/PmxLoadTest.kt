package top.fifthlight.renderer.model.pmx.test

import top.fifthlight.renderer.model.pmx.PmxLoader
import java.nio.file.FileSystems
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.time.measureTime

class PmxLoadTest {
    @Test
    fun testLoad() {
        val uri = this.javaClass.classLoader.getResource("model.pmx")!!.toURI()
        if (uri.scheme == "jar") {
            runCatching {
                FileSystems.newFileSystem(uri, mapOf("create" to "true"))
            }
        }
        val file = uri.toPath()
        measureTime {
            PmxLoader.load(file)
        }.let { duration ->
            println("PMX load time: $duration")
        }
    }
}