package top.fifthlight.armorstand.ui.util

import io.wispforest.owo.ui.component.CheckboxComponent
import io.wispforest.owo.ui.component.DiscreteSliderComponent
import io.wispforest.owo.ui.component.SliderComponent
import io.wispforest.owo.ui.component.TextAreaComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T> TextAreaComponent.sync(scope: CoroutineScope, state: Flow<T>, stateGetter: (T) -> String, updater: (String) -> Unit) = apply {
    scope.launch {
        state.collect {
            val newText = stateGetter(it)
            if (text != newText) {
                text(newText)
            }
        }
    }
    onChanged().subscribe {
        updater(it)
    }
}

fun <T> TextBoxComponent.sync(scope: CoroutineScope, state: Flow<T>, stateGetter: (T) -> String, updater: (String) -> Unit) = apply {
    scope.launch {
        state.collect {
            val newText = stateGetter(it)
            if (text != newText) {
                text(newText)
            }
        }
    }
    onChanged().subscribe {
        updater(it)
    }
}

fun <T> CheckboxComponent.sync(scope: CoroutineScope, state: Flow<T>, stateGetter: (T) -> Boolean, updater: (Boolean) -> Unit) = apply {
    scope.launch {
        state.collect {
            checked(stateGetter(it))
        }
    }
    onChanged {
        updater(it)
    }
}

fun <T> DiscreteSliderComponent.sync(scope: CoroutineScope, state: Flow<T>, stateGetter: (T) -> Double, updater: (Double) -> Unit) = apply {
    scope.launch {
        state.collect {
            setFromDiscreteValue(stateGetter(it))
        }
    }
    slideEnd().subscribe {
        updater(discreteValue())
    }
}

fun <T> SliderComponent.sync(scope: CoroutineScope, state: Flow<T>, stateGetter: (T) -> Double, updater: (Double) -> Unit) = apply {
    scope.launch {
        state.collect {
            value(stateGetter(it))
        }
    }
    slideEnd().subscribe {
        updater(value())
    }
}