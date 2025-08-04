package top.fifthlight.blazerod.model.load

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.future
import top.fifthlight.blazerod.model.Model
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.util.BlazeRod

object ModelLoader {
    suspend fun loadModel(model: Model): RenderScene? = coroutineScope {
        val loadInfo = ModelPreprocessor.preprocess(
            scope = this,
            dispatcher = Dispatchers.Default,
            model = model,
        ) ?: return@coroutineScope null
        val gpuInfo = ModelResourceLoader.load(
            scope = this,
            dispatcher = Dispatchers.BlazeRod.Main,
            info = loadInfo,
        )
        SceneReconstructor.reconstruct(
            dispatcher = Dispatchers.BlazeRod.Main,
            info = gpuInfo
        )
    }

    @JvmStatic
    fun loadModelAsFuture(model: Model) = CoroutineScope(Dispatchers.Default).future {
        loadModel(model)
    }
}