package top.fifthlight.armorstand.util

import top.fifthlight.renderer.model.ModelFileLoader
import top.fifthlight.renderer.model.gltf.GltfBinaryLoader
import top.fifthlight.renderer.model.pmd.PmdLoader
import top.fifthlight.renderer.model.pmx.PmxLoader
import top.fifthlight.renderer.model.util.readRemaining
import top.fifthlight.renderer.model.vmd.VmdLoader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object ModelLoaders {
    val loaders = listOf(
        GltfBinaryLoader,
        PmxLoader,
        PmdLoader,
        VmdLoader,
    )

    val modelExtensions = loaders
        .filter { ModelFileLoader.Ability.MODEL in it.abilities }
        .flatMap { it.extensions }
    val animationExtensions = loaders
        .filter { ModelFileLoader.Ability.ANIMATION in it.abilities }
        .flatMap { it.extensions }
    val embedThumbnailLoaders = loaders
        .filter { ModelFileLoader.Ability.EMBED_THUMBNAIL in it.abilities }
    val embedThumbnailExtensions = embedThumbnailLoaders.flatMap { it.extensions }
    val probeBytes = loaders.maxOf { it.probeLength }

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