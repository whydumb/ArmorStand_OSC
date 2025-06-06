package top.fifthlight.armorstand.ui.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import top.fifthlight.armorstand.ui.model.ViewModel
import top.fifthlight.armorstand.ui.screen.ArmorStandScreen
import kotlin.math.pow
import kotlin.math.roundToInt

fun <T : ArmorStandScreen<T, M>, M : ViewModel> ArmorStandScreen<T, M>.slider(
    textFactory: (String) -> Text,
    width: Int = 150,
    height: Int = 20,
    min: Double = 0.0,
    max: Double = 1.0,
    decimalPlaces: Int = 2,
    value: Flow<Double>,
    onValueChanged: (Double) -> Unit,
): SliderWidget = object : SliderWidget(0, 0, width, height, ScreenTexts.EMPTY, 0.0) {
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
    scope.launch {
        value.collect {
            setValue((it - min) / (max - min))
        }
    }
}