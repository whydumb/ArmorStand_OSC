package top.fifthlight.armorstand.ui.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import top.fifthlight.armorstand.ArmorStandClient.scope
import top.fifthlight.armorstand.ui.screen.BaseArmorStandScreen

fun <T : BaseArmorStandScreen<T>> BaseArmorStandScreen<T>.textField(
    placeHolder: Text? = null,
    width: Int = this.width,
    height: Int = 20,
    text: Flow<String>,
    onChanged: (String) -> Unit,
) = TextFieldWidget(textRenderer, width, height, Text.empty()).apply {
    placeHolder?.let {
        setPlaceholder(placeHolder)
    }
    setChangedListener {
        onChanged(it)
    }
    scope.launch {
        text.collect { newText ->
            if (newText != this@apply.text) {
                this@apply.text = newText
            }
        }
    }
}