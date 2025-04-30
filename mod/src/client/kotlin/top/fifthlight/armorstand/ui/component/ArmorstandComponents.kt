package top.fifthlight.armorstand.ui.component

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.SpriteComponent
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier

object ArmorstandComponents {
    private val loadingIconSprite by lazy {
        MinecraftClient.getInstance().guiAtlasManager.getSprite(Identifier.of("armorstand", "loading"))
    }
    fun loadingIcon(): SpriteComponent = Components.sprite(loadingIconSprite)
}
