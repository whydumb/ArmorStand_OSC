package top.fifthlight.renderer.model

data class Accessor(
    val bufferView: BufferView?,
    val byteOffset: Int = 0,
    val componentType: ComponentType,
    val normalized: Boolean = false,
    val count: Int,
    val type: AccessorType,
    val max: List<Float>? = null,
    val min: List<Float>? = null,
    val name: String? = null,
) {
    val length
        get() = count * componentType.byteLength * type.components

    enum class ComponentType(val byteLength: Int) {
        BYTE(1),
        UNSIGNED_BYTE(1),
        SHORT(2),
        UNSIGNED_SHORT(2),
        UNSIGNED_INT(4),
        FLOAT(4),
    }

    enum class AccessorType(val components: Int) {
        SCALAR(1),
        VEC2(2),
        VEC3(3),
        VEC4(4),
        MAT2(4),
        MAT3(9),
        MAT4(16),
    }
}