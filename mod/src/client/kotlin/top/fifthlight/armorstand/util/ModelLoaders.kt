package top.fifthlight.armorstand.util

import top.fifthlight.blazerod.model.ModelFileLoader
import top.fifthlight.blazerod.model.ModelFileLoaders

object ModelLoaders {
    private val loaders
        get() = ModelFileLoaders.loaders
    val modelExtensions by lazy {
        loaders
            .flatMap { it.extensions.entries }
            .mapNotNull { (extension, abilities) ->
                extension.takeIf { ModelFileLoader.Ability.MODEL in abilities }
            }
    }
    val animationExtensions by lazy {
        loaders
            .flatMap { it.extensions.entries }
            .mapNotNull { (extension, abilities) ->
                extension.takeIf { ModelFileLoader.Ability.EXTERNAL_ANIMATION in abilities }
            }
    }
    val embedThumbnailExtensions by lazy {
        loaders
            .flatMap { it.extensions.entries }
            .mapNotNull { (extension, abilities) ->
                extension.takeIf { ModelFileLoader.Ability.EMBED_THUMBNAIL in abilities }
            }
    }
}