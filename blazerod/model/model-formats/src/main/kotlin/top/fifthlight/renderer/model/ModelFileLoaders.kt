package top.fifthlight.renderer.model

import top.fifthlight.renderer.model.gltf.GltfBinaryLoader
import top.fifthlight.renderer.model.pmd.PmdLoader
import top.fifthlight.renderer.model.pmx.PmxLoader
import top.fifthlight.renderer.model.util.readRemaining
import top.fifthlight.renderer.model.vmd.VmdLoader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object ModelFileLoaders {
    val loaders = listOf(
        GltfBinaryLoader,
        PmxLoader,
        PmdLoader,
        VmdLoader,
    )

    private val embedThumbnailLoaders by lazy {
        loaders.filter { ModelFileLoader.Ability.EMBED_THUMBNAIL in it.abilities }
    }

    private val probeBytes by lazy {
        loaders.maxOf { it.probeLength }
    }

    private fun probeLoader(loaders: List<ModelFileLoader>, path: Path) = FileChannel.open(path, StandardOpenOption.READ).use { channel ->
        val buffer = ByteBuffer.allocate(probeBytes)
        channel.readRemaining(buffer)
        buffer.flip()

        for (loader in loaders) {
            buffer.position(0)
            if (buffer.remaining() >= loader.probeLength) {
                if (loader.probe(buffer)) {
                    return@use loader
                }
            }
        }
        null
    }

    fun probeAndLoad(path: Path, basePath: Path = path.parent ?: error("no base path: $path")): ModelFileLoader.LoadResult? {
        val loader = probeLoader(loaders, path)
        return loader?.load(path, basePath)
    }

    fun getEmbedThumbnail(path: Path, basePath: Path? = path.parent): ModelFileLoader.ThumbnailResult? {
        val loader = probeLoader(embedThumbnailLoaders, path)
        return loader?.getThumbnail(path, basePath)
    }
}