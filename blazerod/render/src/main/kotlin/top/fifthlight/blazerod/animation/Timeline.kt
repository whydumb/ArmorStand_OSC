package top.fifthlight.blazerod.animation

class Timeline(
    val duration: Double,
    loop: Boolean = true,
    speed: Double = 1.0,
) {
    private var baseTimeNanos: Long = 0L
    private var basePosition: Double = 0.0
    private var pausedAt: Double = 0.0
    var isPaused: Boolean = true
        private set
    var speed: Double = speed
        private set
    var loop: Boolean = loop
        private set

    val isPlaying: Boolean
        get() = !isPaused

    fun isFinished(currentNanos: Long): Boolean {
        if (loop) return false
        val time = getCurrentTime(currentNanos)
        return time >= duration
    }

    fun getCurrentTime(currentNanos: Long): Double {
        if (isPaused) return pausedAt

        val elapsedNanos = currentNanos - baseTimeNanos
        val elapsedSeconds = elapsedNanos * 1e-9 * speed
        var position = basePosition + elapsedSeconds

        if (loop && duration > 0) {
            position %= duration
            if (position < 0) {
                position += duration
            }
        } else if (position > duration) {
            position = duration
        } else if (position < 0) {
            position = 0.0
        }

        return position
    }

    fun play(currentNanos: Long) {
        if (isPaused) {
            baseTimeNanos = currentNanos
            basePosition = pausedAt
            isPaused = false
        }
    }

    fun pause(currentNanos: Long) {
        if (!isPaused) {
            pausedAt = getCurrentTime(currentNanos)
            isPaused = true
        }
    }

    fun reset(currentNanos: Long) {
        baseTimeNanos = currentNanos
        basePosition = 0.0
        pausedAt = 0.0
        isPaused = true
    }

    fun setLoop(newLoop: Boolean) {
        loop = newLoop
    }

    fun setSpeed(currentNanos: Long, newSpeed: Double) {
        if (speed == newSpeed) return

        val currentPosition = getCurrentTime(currentNanos)

        basePosition = currentPosition
        baseTimeNanos = currentNanos
        speed = newSpeed

        if (isPaused) {
            pausedAt = currentPosition
        }
    }

    fun seek(currentNanos: Long, newTime: Double) {
        val clampedTime = when {
            loop && duration > 0 -> newTime % duration
            newTime < 0 -> 0.0
            newTime > duration -> duration
            else -> newTime
        }

        baseTimeNanos = currentNanos
        basePosition = clampedTime

        if (isPaused) {
            pausedAt = clampedTime
        }
    }
}
