package top.fifthlight.renderer.model

data class Expression(
    val name: String? = null,
    val tag: Tag,
    val isBinary: Boolean = false,
    val bindings: List<Binding>,
) {
    sealed interface Tag {
        data object Neutral: Tag
        enum class Emotion: Tag {
            HAPPY,
            ANGRY,
            SAD,
            RELAXED,
            SURPRISED,
        }
        enum class Lip: Tag {
            AA,
            IH,
            OU,
            EE,
            OH,
        }
        enum class Blink: Tag {
            BLINK,
            BLINK_LEFT,
            BLINK_RIGHT,
        }
        enum class Gaze: Tag {
            LOOK_UP,
            LOOK_DOWN,
            LOOK_LEFT,
            LOOK_RIGHT,
        }
    }

    sealed class Binding {
        data class NodeMorphTarget(
            val nodeId: NodeId,
            val index: Int,
            val weight: Float,
        ): Binding()

        data class MeshMorphTarget(
            val meshId: MeshId,
            val index: Int,
            val weight: Float,
        ): Binding()
    }
}
