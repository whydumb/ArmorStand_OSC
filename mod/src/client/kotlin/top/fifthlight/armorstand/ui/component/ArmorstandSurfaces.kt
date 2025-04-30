package top.fifthlight.armorstand.ui.component

import io.wispforest.owo.shader.OwoBlurRenderer
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.util.ScissorStack
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.EntryListWidget
import net.minecraft.client.render.RenderLayer

object ArmorstandSurfaces {
    val VANILLA_BLUR = Surface { context, component ->
        val client = MinecraftClient.getInstance()
        val blurSizeInt = client.options.menuBackgroundBlurrinessValue
        if (blurSizeInt < 1) {
            return@Surface
        }
        val blurSize = blurSizeInt.toFloat() * 2f
        OwoBlurRenderer.drawBlur(context, component, 16, blurSize / 2, blurSize)
    }

    val OPTIONS_BACKGROUND: Surface = Surface
        .panorama(Screen.ROTATING_PANORAMA_RENDERER, false)
        .and(VANILLA_BLUR)

    val VANILLA_TRANSLUCENT: Surface = Surface { context, component ->
        val client = MinecraftClient.getInstance()
        val backgroundTexture = if (client.world == null) {
            Screen.MENU_BACKGROUND_TEXTURE
        } else {
            Screen.INWORLD_MENU_BACKGROUND_TEXTURE
        }
        Screen.renderBackgroundTexture(
            context,
            backgroundTexture,
            component.x(),
            component.y(),
            0.0f,
            0.0f,
            component.width(),
            component.height(),
        )
    }

    val SCREEN_BACKGROUND: Surface
        get() = if (MinecraftClient.getInstance().world == null) {
            OPTIONS_BACKGROUND
        } else {
            VANILLA_TRANSLUCENT.and(VANILLA_BLUR)
        }

    val LIST_SEPARATOR = Surface { context, component ->
        context.draw()
        ScissorStack.drawUnclipped {
            val headerTexture = if (MinecraftClient.getInstance().world == null) {
                Screen.HEADER_SEPARATOR_TEXTURE
            } else {
                Screen.INWORLD_HEADER_SEPARATOR_TEXTURE
            }
            context.drawTexture(
                RenderLayer::getGuiTextured,
                headerTexture,
                component.x(),
                component.y(),
                0.0f,
                0.0f,
                component.width(),
                2,
                32,
                2,
            )

            val footerTexture = if (MinecraftClient.getInstance().world == null) {
                Screen.FOOTER_SEPARATOR_TEXTURE
            } else {
                Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE
            }
            context.drawTexture(
                RenderLayer::getGuiTextured,
                footerTexture,
                component.x(),
                component.y() + component.height() - 2,
                0.0f,
                0.0f,
                component.width(),
                2,
                32,
                2
            )
        }
    }

    val LIST_BACKGROUND: Surface
        get() = if (MinecraftClient.getInstance().world == null) {
            Surface.tiled(EntryListWidget.MENU_LIST_BACKGROUND_TEXTURE, 32, 32)
        } else {
            Surface.tiled(EntryListWidget.INWORLD_MENU_LIST_BACKGROUND_TEXTURE, 32, 32)
        }

    val LIST_BACKGROUND_WITH_SEPARATOR: Surface
        get() = LIST_BACKGROUND.and(LIST_SEPARATOR)

    fun bottomLine(color: Int) = Surface { context, component ->
        context.drawHorizontalLine(component.x(), component.x() + component.width(), component.y() + component.height() - 1, color)
    }
}