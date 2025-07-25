package top.fifthlight.armorstand.event

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.screen.Screen

object ScreenEvents {
    @JvmField
    val UNLOCK_CURSOR: Event<UnlockMouse> =
        EventFactory.createArrayBacked(UnlockMouse::class.java, UnlockMouse { true }, { callbacks ->
            UnlockMouse { screen ->
                var unlock = true
                for (callback in callbacks) {
                    unlock = callback.onMouseUnlocked(screen) && unlock
                }
                unlock
            }
        })

    fun interface UnlockMouse {
        fun onMouseUnlocked(screen: Screen): Boolean
    }

    @JvmField
    val MOVE_VIEW: Event<MoveView> =
        EventFactory.createArrayBacked(MoveView::class.java, MoveView { true }, { callbacks ->
            MoveView { screen ->
                var moved = true
                for (callback in callbacks) {
                    moved = callback.onViewMoved(screen) && moved
                }
                moved
            }
        })

    fun interface MoveView {
        fun onViewMoved(screen: Screen?): Boolean
    }
}