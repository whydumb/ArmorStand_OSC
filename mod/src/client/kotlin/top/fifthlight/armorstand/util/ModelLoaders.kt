package top.fifthlight.armorstand.util

import top.fifthlight.renderer.model.ModelFileLoader
import top.fifthlight.renderer.model.ModelFileLoaders

object ModelLoaders {
    private val loaders
        get() = ModelFileLoaders.loaders
    val modelExtensions = loaders
        .filter { ModelFileLoader.Ability.MODEL in it.abilities }
        .flatMap { it.extensions }
    val animationExtensions = loaders
        .filter { ModelFileLoader.Ability.ANIMATION in it.abilities }
        .flatMap { it.extensions }
    val embedThumbnailExtensions = loaders
        .filter { ModelFileLoader.Ability.EMBED_THUMBNAIL in it.abilities }
        .flatMap { it.extensions }
}