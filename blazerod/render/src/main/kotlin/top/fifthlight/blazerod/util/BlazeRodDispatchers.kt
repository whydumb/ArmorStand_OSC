package top.fifthlight.blazerod.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import top.fifthlight.blazerod.BlazeRod

object BlazeRodDispatchers {
    val Main: CoroutineDispatcher
        get() = BlazeRod.mainDispatcher

    val Load: CoroutineDispatcher
        get() = ResourceLoader.instance.loadDispatcher
}

val Dispatchers.BlazeRod
    get() = BlazeRodDispatchers
