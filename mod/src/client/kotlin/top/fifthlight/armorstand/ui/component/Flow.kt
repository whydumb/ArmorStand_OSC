package top.fifthlight.armorstand.ui.component

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T, C> Flow<T>.component(
    scope: CoroutineScope,
    containerFactory: () -> C,
    onUpdate: C.(T) -> Unit,
): C = containerFactory().also { container ->
    scope.launch {
        collect {
            onUpdate(container, it)
        }
    }
}

inline fun <T, C: Component> EditableFlowLayout.refreshList(
    list: List<T>,
    componentFactory: (T) -> C,
) {
    for ((index, item) in list.withIndex()) {
        if (index < children().size) {
            setChild(index, componentFactory(item))
        } else {
            child(componentFactory(item))
        }
    }
    while (children().size > list.size) {
        removeChild(children()[list.size])
    }
}

inline fun <T, reified C: Component> FlowLayout.updateList(
    list: List<T>,
    componentFactory: () -> C,
    componentUpdater: C.(T) -> Unit,
) {
    for ((index, item) in list.withIndex()) {
        val component = children().getOrNull(index) ?: componentFactory()
        check(component is C) { "Bad component type: want ${C::class.java.simpleName}, but got ${component.javaClass.simpleName}" }
        componentUpdater(component, item)
    }
    while (children().size > list.size) {
        removeChild(children()[list.size])
    }
}