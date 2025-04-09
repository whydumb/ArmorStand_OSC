package top.fifthlight.armorstand.animation

interface KeyFrameIndexer {
    // Total time for this animation
    val duration: Float

    // Total indices
    val indices: Int

    data class FindResult(
        var startFrame: Int = 0,
        var endFrame: Int = 0,
        var startTime: Float = 0f,
        var endTime: Float = 0f,
    ) {
        fun clear() {
            startFrame = 0
            endFrame = 0
            startTime = 0f
            endTime = 0f
        }
    }

    // Find two keyframe for specified timestamp, return index and time
    fun findKeyFrames(time: Float, result: FindResult)
}

class FloatArrayKeyFrameIndexer(private val times: FloatArray) : KeyFrameIndexer {
    override val duration = if (times.isEmpty()) 0f else times.last()
    override val indices = times.size

    override fun findKeyFrames(time: Float, result: KeyFrameIndexer.FindResult) {
        when {
            times.isEmpty() -> {
                result.clear()
                return
            }
            times.size < 2 || time <= times[0] -> {
                result.startFrame = 0
                result.endFrame = 0
                result.startTime = times[0]
                result.endTime = times[0]
                return
            }

            time >= times.last() -> {
                result.startFrame = times.lastIndex
                result.endFrame = times.lastIndex
                result.startTime = times.last()
                result.endTime = times.last()
                return
            }
        }

        val maxFrames = 30
        val maxTime = 1.0f

        val searchStartIndex = result.startFrame.coerceIn(0, times.lastIndex - 1)
        val searchEndIndex = (result.startFrame + maxFrames).coerceIn(0, times.lastIndex - 1)
        var currentIndex = result.startFrame.coerceIn(0, times.lastIndex - 1)

        while (currentIndex < times.lastIndex && currentIndex < searchEndIndex && times[currentIndex] - times[searchStartIndex] <= maxTime) {
            if (time >= times[currentIndex] && time < times[currentIndex + 1]) {
                result.startFrame = currentIndex
                result.endFrame = currentIndex + 1
                result.startTime = times[currentIndex]
                result.endTime = times[currentIndex + 1]
                return
            }

            if (times[currentIndex] > time) {
                break
            }

            currentIndex++
        }

        val searchResult = times.binarySearch(time)
        val foundIndex = if (searchResult >= 0) {
            searchResult
        } else {
            val insertionPoint = -searchResult - 1
            (insertionPoint - 1).coerceAtLeast(0)
        }.coerceIn(0 until times.lastIndex)

        result.startFrame = foundIndex
        result.endFrame = foundIndex + 1
        result.startTime = times[foundIndex]
        result.endTime = times[foundIndex + 1]
    }

    companion object {
        fun createFixedRate(duration: Float, fps: Int): KeyFrameIndexer {
            require(duration > 0 && fps > 0) { "Duration and fps should be greater than zero" }
            val frameCount = (duration * fps).toInt() + 1
            return FloatArrayKeyFrameIndexer(FloatArray(frameCount) { it * (1f / fps) })
        }
    }
}
