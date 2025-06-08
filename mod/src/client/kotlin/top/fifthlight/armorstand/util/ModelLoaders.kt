package top.fifthlight.armorstand.util

import top.fifthlight.blazerod.model.ModelFileLoader
import top.fifthlight.blazerod.model.ModelFileLoaders

object ModelLoaders {
    private val loaders
        get() = ModelFileLoaders.loaders
    val modelExtensions by lazy {
        loaders
            .filter { ModelFileLoader.Ability.MODEL in it.abilities }
            .flatMap { it.extensions }
    }
    val animationExtensions by lazy {
        loaders
            .filter { ModelFileLoader.Ability.ANIMATION in it.abilities }
            .flatMap { it.extensions }
    }
    val embedThumbnailExtensions by lazy {
        loaders
            .filter { ModelFileLoader.Ability.EMBED_THUMBNAIL in it.abilities }
            .flatMap { it.extensions }
    }
}