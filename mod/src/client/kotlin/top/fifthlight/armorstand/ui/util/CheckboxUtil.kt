package top.fifthlight.armorstand.ui.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.CheckboxWidget
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.model.ViewModel
import top.fifthlight.armorstand.ui.screen.ArmorStandScreen

fun <T : ArmorStandScreen<T, M>, M: ViewModel> ArmorStandScreen<T, M>.checkbox(
    text: Text,
    value: Flow<Boolean>,
    onValueChanged: (Boolean) -> Unit,
): CheckboxWidget = CheckboxWidget.builder(text, MinecraftClient.getInstance().textRenderer)
    .callback { checkbox, checked -> onValueChanged(checked) }
    .build()
    .apply {
        scope.launch {
            value.collect {
                checked = it
            }
        }
    }