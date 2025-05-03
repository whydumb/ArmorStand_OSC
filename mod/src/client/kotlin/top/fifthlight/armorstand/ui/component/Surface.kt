package top.fifthlight.armorstand.ui.component

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.EntryListWidget
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import java.util.function.Function

fun interface Surface {
    fun draw(context: DrawContext, x: Int, y: Int, width: Int, height: Int)

    companion object {
        val empty = Surface { context, x, y, width, height -> }

        fun combine(vararg surfaces: Surface) = Surface { context, x, y, width, height ->
            for (surface in surfaces) {
                surface.draw(context, x, y, width, height)
            }
        }

        fun color(color: Int) = Surface { context, x, y, width, height ->
            context.fill(x, y, x + width, y + height, color)
        }

        fun border(color: Int) = Surface { context, x, y, width, height ->
            context.drawBorder(x, y, width, height, color)
        }

        fun padding(padding: Insets, surface: Surface) = Surface { context, x, y, width, height ->
            surface.draw(
                context = context,
                x = x + padding.left,
                y = y + padding.top,
                width = (width - padding.left - padding.right).coerceAtLeast(0),
                height = (height - padding.top - padding.bottom).coerceAtLeast(0),
            )
        }

        fun texture(
            identifier: Identifier,
            textureWidth: Int = 256,
            textureHeight: Int = 256,
            u: Float = 0f,
            v: Float = 0f,
            regionWidth: Int = textureWidth,
            regionHeight: Int = textureHeight,
        ) = Surface { context, x, y, width, height ->
            context.drawTexture(
                RenderLayer::getGuiTextured,
                identifier,
                x,
                y,
                u,
                v,
                width,
                height,
                textureWidth,
                textureHeight,
            )
        }

        fun headerSeparator() = if (MinecraftClient.getInstance().world != null) {
            Screen.INWORLD_HEADER_SEPARATOR_TEXTURE
        } else {
            Screen.HEADER_SEPARATOR_TEXTURE
        }.let { texture ->
            Surface { context, x, y, width, height ->
                context.drawTexture(
                    RenderLayer::getGuiTextured,
                    texture,
                    x,
                    y,
                    0.0F,
                    0.0F,
                    width,
                    2,
                    32,
                    2
                )
            }
        }

        fun footerSeparator() = if (MinecraftClient.getInstance().world != null) {
            Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE
        } else {
            Screen.FOOTER_SEPARATOR_TEXTURE
        }.let { texture ->
            Surface { context, x, y, width, height ->
                context.drawTexture(
                    RenderLayer::getGuiTextured,
                    texture,
                    x,
                    y + height - 2,
                    0.0f,
                    0.0f,
                    width,
                    2,
                    32,
                    2
                )
            }
        }

        fun separator() = combine(headerSeparator(), footerSeparator())

        fun listBackground() = if (MinecraftClient.getInstance().world != null) {
            EntryListWidget.INWORLD_MENU_LIST_BACKGROUND_TEXTURE
        } else {
            EntryListWidget.MENU_LIST_BACKGROUND_TEXTURE
        }.let { texture(it) }

        fun listBackgroundWithSeparator() = combine(
            padding(Insets(2, 0), listBackground()),
            separator(),
        )
    }
}