package top.fifthlight.blazerod.event

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object RenderEvents {
    @JvmField
    val FLIP_FRAME: Event<FlipFrame> =
        EventFactory.createArrayBacked(FlipFrame::class.java, FlipFrame {}, { callbacks ->
            FlipFrame {
                for (callback in callbacks) {
                    callback.onFrameFlipped()
                }
            }
        })

    fun interface FlipFrame {
        fun onFrameFlipped()
    }

    @JvmField
    val INITIALIZE_DEVICE: Event<InitializeDevice> =
        EventFactory.createArrayBacked(InitializeDevice::class.java, InitializeDevice {}, { callbacks ->
            InitializeDevice {
                for (callback in callbacks) {
                    callback.onDeviceInitialized()
                }
            }
        })

    fun interface InitializeDevice {
        fun onDeviceInitialized()
    }
}