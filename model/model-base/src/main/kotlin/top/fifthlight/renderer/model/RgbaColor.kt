package top.fifthlight.renderer.model

data class RgbaColor(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float,
) {
    constructor(rgba: FloatArray): this(rgba[0], rgba[1], rgba[2], rgba[3])
}

data class RgbColor(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    constructor(rgba: FloatArray): this(rgba[0], rgba[1], rgba[2])
}
