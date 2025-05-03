@file:Suppress("UnusedReceiverParameter")

package top.fifthlight.armorstand.ui.dsl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import top.fifthlight.armorstand.ui.component.BorderLayout
import top.fifthlight.armorstand.ui.component.Insets
import top.fifthlight.armorstand.ui.component.LinearLayout
import top.fifthlight.armorstand.ui.component.Surface
import top.fifthlight.armorstand.ui.component.WrapperLayoutWidget
import top.fifthlight.armorstand.ui.util.Dimensions

fun <T, S: Screen> Context<T, S>.positioner(block: Positioner.() -> Unit): Positioner = Positioner.create().apply(block)

fun <T : Screen, S : Screen, C> Context<T, S>.add(component: C) where C : Element, C : Drawable, C : Selectable {
    element.addDrawableChild(component)
}

fun <T, S : Screen> Context<T, S>.borderLayout(
    dimensions: Dimensions = Dimensions(),
    direction: BorderLayout.Direction,
    block: Context<BorderLayout, S>.() -> Unit,
) = LayoutContext(
    element = BorderLayout(
        left = dimensions.x,
        top = dimensions.y,
        widthKt = dimensions.width,
        heightKt = dimensions.height,
        direction = direction,
    ),
    client = client,
    viewScope = viewScope,
    screen = screen,
).also { context ->
    block(context)
    context.element.apply {
        forEachChild { child ->
            screen.addDrawableChild(child)
        }
        refreshPositions()
    }
}.element

fun <C : ClickableWidget, S : Screen> Context<BorderLayout, S>.first(
    component: C,
    positioner: Positioner = Positioner.create(),
) = element.setFirstElement(component, positioner) { widget, width, height ->
    widget.width = width
    widget.height = height
}

fun <C : ClickableWidget, S : Screen> Context<BorderLayout, S>.center(
    component: C,
    positioner: Positioner = Positioner.create(),
) = element.setCenterElement(component, positioner) { widget, width, height ->
    widget.width = width
    widget.height = height
}

fun <C : ClickableWidget, S : Screen> Context<BorderLayout, S>.second(
    component: C,
    positioner: Positioner = Positioner.create(),
) = element.setSecondElement(component, positioner) { widget, width, height ->
    widget.width = width
    widget.height = height
}

fun <S : Screen> Context<BorderLayout, S>.first(
    component: WrapperLayoutWidget,
    positioner: Positioner = Positioner.create(),
) = element.setFirstElement(component, positioner) { widget, width, height -> widget.setDimension(width, height) }

fun <S : Screen> Context<BorderLayout, S>.center(
    component: WrapperLayoutWidget,
    positioner: Positioner = Positioner.create(),
) = element.setCenterElement(component, positioner) { widget, width, height -> widget.setDimension(width, height) }

fun <S : Screen> Context<BorderLayout, S>.second(
    component: WrapperLayoutWidget,
    positioner: Positioner = Positioner.create(),
) = element.setSecondElement(component, positioner) { widget, width, height -> widget.setDimension(width, height) }

fun <S : Screen> Context<BorderLayout, S>.first(
    component: Widget,
    positioner: Positioner = Positioner.create(),
) = element.setFirstElement(component, positioner) { widget, width, height -> }

fun <S : Screen> Context<BorderLayout, S>.center(
    component: Widget,
    positioner: Positioner = Positioner.create(),
) = element.setCenterElement(component, positioner) { widget, width, height -> }

fun <S : Screen> Context<BorderLayout, S>.second(
    component: Widget,
    positioner: Positioner = Positioner.create(),
) = element.setSecondElement(component, positioner) { widget, width, height -> }

fun <T, S : Screen> Context<T, S>.linearLayout(
    direction: LinearLayout.Direction,
    align: LinearLayout.Align = LinearLayout.Align.START,
    gap: Int = 0,
    padding: Insets = Insets.ZERO,
    dimensions: Dimensions = Dimensions(),
    surface: Surface = Surface.empty,
    block: Context<LinearLayout, S>.() -> Unit,
) = LayoutContext(
    element = LinearLayout(
        left = dimensions.x,
        top = dimensions.y,
        widthKt = dimensions.width,
        heightKt = dimensions.height,
        direction = direction,
        align = align,
        gap = gap,
        padding = padding,
        surface = surface,
    ),
    client = client,
    viewScope = viewScope,
    screen = screen,
).also { context ->
    block(context)
    context.element.apply {
        screen.addDrawable(this)
        forEachChild { child ->
            screen.addDrawableChild(child)
        }
        refreshPositions()
    }
}.element

fun <T, S : Screen, E> Context<T, S>.dynamicLinearLayout(
    direction: LinearLayout.Direction,
    align: LinearLayout.Align = LinearLayout.Align.START,
    gap: Int = 0,
    padding: Insets = Insets.ZERO,
    dimensions: Dimensions = Dimensions(),
    surface: Surface = Surface.empty,
    data: Flow<E>,
    block: Context<LinearLayout, S>.(E) -> Unit,
) = LayoutContext(
    element = LinearLayout(
        left = dimensions.x,
        top = dimensions.y,
        widthKt = dimensions.width,
        heightKt = dimensions.height,
        direction = direction,
        align = align,
        gap = gap,
        padding = padding,
        surface = surface,
    ),
    client = client,
    viewScope = viewScope,
    screen = screen,
).also { context ->
    context.element.apply {
        viewScope.launch {
            data.collect {
                forEachChild { child ->
                    screen.remove(child)
                }
                clear()
                block(context, it)
                forEachChild { child ->
                    screen.addDrawableChild(child)
                }
                refreshPositions()
            }
        }
        screen.addDrawable(this)
    }
}.element

fun <S : Screen> Context<LinearLayout, S>.add(
    component: Widget,
    positioner: Positioner = Positioner.create(),
) = element.add(component, positioner)
