package top.fifthlight.armorstand.animation

class Timeline(
    private val duration: Float,
    private val loop: Boolean = true,
    private val speed: Float = 1f
) {
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    private var isPaused: Boolean = true

    val currentTime: Float
        get() {
            if (isPaused) return pausedTime / 1000f

            val elapsed = (System.currentTimeMillis() - startTime) * speed
            val totalTime = elapsed + pausedTime

            if (!loop) {
                return (totalTime / 1000f).coerceAtMost(duration)
            }

            val loopDuration = (duration * 1000).toLong()
            if (loopDuration <= 0) return 0f

            val loopedTime = totalTime % loopDuration
            return (if (loopedTime < 0) loopedTime + loopDuration else loopedTime) / 1000f
        }

    val isPlaying: Boolean
        get() = !isPaused

    val isFinished: Boolean
        get() = !loop && currentTime >= duration

    fun play() {
        if (isPaused) {
            startTime = System.currentTimeMillis() - (pausedTime / speed).toLong()
            isPaused = false
        }
    }

    fun pause() {
        if (!isPaused) {
            pausedTime = (System.currentTimeMillis() - startTime) * speed.toLong()
            isPaused = true
        }
    }

    fun reset() {
        startTime = System.currentTimeMillis()
        pausedTime = 0L
    }

    fun seek(time: Float) {
        val targetTime = time.coerceIn(0f, duration)
        pausedTime = (targetTime * 1000).toLong()
        if (!isPaused) {
            startTime = System.currentTimeMillis() - (pausedTime / speed).toLong()
        }
    }
}
