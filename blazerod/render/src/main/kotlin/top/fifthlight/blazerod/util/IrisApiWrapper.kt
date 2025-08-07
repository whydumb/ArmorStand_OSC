package top.fifthlight.blazerod.util

import net.fabricmc.loader.api.FabricLoader
import net.irisshaders.iris.api.v0.IrisApi

object IrisApiWrapper {
    private val hasIris = FabricLoader.getInstance().isModLoaded("iris")
    private val irisApi = if (hasIris) {
        IrisApi.getInstance()
    } else {
        null
    }

    val shaderPackInUse: Boolean
        get() = irisApi?.isShaderPackInUse == true
}