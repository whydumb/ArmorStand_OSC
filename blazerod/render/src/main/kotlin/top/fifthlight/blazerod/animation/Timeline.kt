package top.fifthlight.blazerod.animation

import top.fifthlight.blazerod.util.TimeUtil

class Timeline(
    private val duration: Float,
    private val loop: Boolean = true,
    private val speed: Float = 1f
) {
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    private var isPaused: Boolean = true

    val isPlaying: Boolean
        get() = !isPaused

    fun isFinished(currentNanos: Long) = !loop && getCurrentTime(currentNanos) >= duration

    fun getCurrentTime(currentNanos: Long): Float {
        if (isPaused) return pausedTime / TimeUtil.NANOSECONDS_PER_SECOND.toFloat()

        val elapsed = (currentNanos - startTime) * speed
        val totalTime = elapsed + pausedTime

        if (!loop) {
            return (totalTime / TimeUtil.NANOSECONDS_PER_SECOND).coerceAtMost(duration)
        }

        val loopDuration = (duration * TimeUtil.NANOSECONDS_PER_SECOND).toLong()
        if (loopDuration <= 0) return 0f

        val loopedTime = totalTime % loopDuration
        return (if (loopedTime < 0) loopedTime + loopDuration else loopedTime) / TimeUtil.NANOSECONDS_PER_SECOND
    }

    fun play(currentNanos: Long) {
        if (isPaused) {
            startTime = currentNanos - (pausedTime / speed).toLong()
            isPaused = false
        }
    }

    fun pause(currentNanos: Long) {
        if (!isPaused) {
            pausedTime = (currentNanos - startTime) * speed.toLong()
            isPaused = true
        }
    }

    fun reset(currentNanos: Long) {
        startTime = currentNanos
        pausedTime = 0L
    }

    fun seek(currentNanos: Long, time: Float) {
        val targetTime = time.coerceIn(0f, duration)
        pausedTime = (targetTime * TimeUtil.NANOSECONDS_PER_SECOND).toLong()
        if (!isPaused) {
            startTime = currentNanos - (pausedTime / speed).toLong()
        }
    }
}
