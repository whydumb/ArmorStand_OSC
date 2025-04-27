package top.fifthlight.armorstand.util

import top.fifthlight.renderer.model.ModelFileLoader
import top.fifthlight.renderer.model.gltf.GltfBinaryLoader
import top.fifthlight.renderer.model.pmd.PmdLoader
import top.fifthlight.renderer.model.pmx.PmxLoader
import top.fifthlight.renderer.model.util.readAll
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
    val probeBytes = loaders.maxOf { it.probeLength }

    fun probeAndLoad(path: Path, basePath: Path = path.parent ?: error("no base path: $path")): ModelFileLoader.Result? {
        val loader = FileChannel.open(path, StandardOpenOption.READ).use { channel ->
            val buffer = ByteBuffer.allocate(probeBytes)
            channel.readAll(buffer)
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
        return loader?.load(path, basePath)
    }
}