@file:Suppress("UnusedReceiverParameter")

package top.fifthlight.armorstand.ui.dsl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CheckboxWidget
import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import top.fifthlight.armorstand.ui.util.Dimensions
import kotlin.math.pow
import kotlin.math.roundToInt

fun <T, S : Screen> Context<T, S>.button(
    text: Text,
    dimensions: Dimensions = Dimensions(width = 150, height = 20),
    action: () -> Unit,
): ButtonWidget = ButtonWidget.builder(text) { action() }
    .dimensions(dimensions.x, dimensions.y, dimensions.width, dimensions.height)
    .build()

fun <T, S : Screen> Context<T, S>.autoWidthButton(
    text: Text,
    height: Int = 20,
    padding: Int = 8,
    action: () -> Unit,
): ButtonWidget = ButtonWidget.builder(text) { action() }
    .dimensions(0, 0, client.textRenderer.getWidth(text) + padding * 2, height)
    .build()

fun <T, S : Screen> Context<T, S>.label(
    text: Text,
): TextWidget = TextWidget(text, client.textRenderer)

fun <T, S : Screen> Context<T, S>.checkbox(
    text: Text,
    value: Flow<Boolean>,
    onValueChanged: (Boolean) -> Unit,
): CheckboxWidget = CheckboxWidget.builder(text, client.textRenderer)
    .callback { checkbox, checked -> onValueChanged(checked) }
    .build()
    .apply {
        viewScope.launch {
            value.collect {
                checked = it
            }
        }
    }

fun <T, S : Screen> Context<T, S>.slider(
    textFactory: (String) -> Text,
    dimensions: Dimensions = Dimensions(width = 150, height = 20),
    min: Double = 0.0,
    max: Double = 1.0,
    decimalPlaces: Int = 2,
    value: Flow<Double>,
    onValueChanged: (Double) -> Unit,
): SliderWidget =
    object : SliderWidget(dimensions.x, dimensions.y, dimensions.width, dimensions.height, ScreenTexts.EMPTY, 0.0) {
        private val scale = 10.0.pow(decimalPlaces)
        fun Double.round() = (this * scale).roundToInt() / scale

        private val realValue
            get() = (this.value * (max - min) + min).round()

        override fun updateMessage() {
            message = textFactory("%.${decimalPlaces}f".format(realValue))
        }

        override fun applyValue() {
            onValueChanged(realValue)
        }

        fun setValue(value: Double) {
            val d = this.value
            this.value = MathHelper.clamp(value, 0.0, 1.0)
            if (d != this.value) {
                this.applyValue()
            }

            this.updateMessage()
        }
    }.apply {
        viewScope.launch {
            value.collect {
                setValue((it - min) / (max - min))
            }
        }
    }
