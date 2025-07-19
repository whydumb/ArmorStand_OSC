package top.fifthlight.blazerod.model.gltf.test

import top.fifthlight.blazerod.model.ModelFileLoader
import top.fifthlight.blazerod.model.gltf.GltfBinaryLoader
import java.nio.file.FileSystems
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.measureTime

class GlbLoadTest {
    @Test
    fun testAlicia() {
        val uri = this.javaClass.classLoader.getResource("AliciaSolid.vrm")!!.toURI()
        if (uri.scheme == "jar") {
            runCatching {
                FileSystems.newFileSystem(uri, mapOf("create" to "true"))
            }
        }
        val file = uri.toPath()
        measureTime {
            GltfBinaryLoader().load(file)
        }.let { duration ->
            println("Alicia load time: $duration")
        }
    }

    @Test
    fun testArmorStand() {
        val uri = this.javaClass.classLoader.getResource("armorstand.vrm")!!.toURI()
        if (uri.scheme == "jar") {
            runCatching {
                FileSystems.newFileSystem(uri, mapOf("create" to "true"))
            }
        }
        val file = uri.toPath()
        measureTime {
            GltfBinaryLoader().load(file)
        }.let { duration ->
            println("armorstand load time: $duration")
        }
    }

    @Test
    fun testVrmThumbnail() {
        val uri = this.javaClass.classLoader.getResource("AliciaSolid.vrm")!!.toURI()
        if (uri.scheme == "jar") {
            runCatching {
                FileSystems.newFileSystem(uri, mapOf("create" to "true"))
            }
        }
        val file = uri.toPath()
        measureTime {
            val result = GltfBinaryLoader().getThumbnail(file)
            assertIs<ModelFileLoader.ThumbnailResult.Embed>(result)
            assertEquals(7739784, result.offset)
            assertEquals(138928, result.length)
        }.let { duration ->
            println("Alicia thumbnail time: $duration")
        }
    }

    @Test
    fun testInterpolation() {
        val uri = this.javaClass.classLoader.getResource("InterpolationTest.glb")!!.toURI()
        if (uri.scheme == "jar") {
            runCatching {
                FileSystems.newFileSystem(uri, mapOf("create" to "true"))
            }
        }
        val file = uri.toPath()
        measureTime {
            GltfBinaryLoader().load(file)
        }.let { duration ->
            println("Interpolation load time: $duration")
        }
    }
}