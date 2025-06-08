package top.fifthlight.renderer.model.pmd.format

import top.fifthlight.renderer.model.RgbColor
import top.fifthlight.renderer.model.RgbaColor

internal data class PmdMaterial(
    val diffuseColor: RgbaColor,
    val specularStrength: Float,
    val specularColor: RgbColor,
    val ambientColor: RgbColor,
    val toonIndex: Int,
    val edgeFlag: Boolean,
    val verticesCount: Int,
    val textureFilename: String?,
    val sphereFilename: String?,
)
