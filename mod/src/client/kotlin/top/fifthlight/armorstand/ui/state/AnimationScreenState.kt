package top.fifthlight.armorstand.ui.state

data class AnimationScreenState(
    val playState: PlayState = PlayState.None,
    val selectedAnimation: AnimationItem? = null,
    val animations: List<AnimationItem> = emptyList(),
) {
    data class AnimationItem(
        val name: String? = null,
        val duration: Double,
        val source: Source,
    ) {
        sealed class Source {
            data class Embed(val index: Int) : Source()
            data class External(val name: String) : Source()
        }
    }

    sealed class PlayState {
        data object None : PlayState()

        data class Paused(
            val progress: Double,
            val length: Double,
        ) : PlayState()

        data class Playing(
            val progress: Double,
            val length: Double,
        ) : PlayState()
    }
}