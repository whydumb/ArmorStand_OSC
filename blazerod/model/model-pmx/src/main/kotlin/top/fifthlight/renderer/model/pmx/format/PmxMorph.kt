package top.fifthlight.renderer.model.pmx.format

data class PmxMorph(
    val nameLocal: String,
    val nameUniversal: String,
    val panelType: PanelType,
    val type: Type,
) {
    enum class PanelType(val value: Int) {
        HIDDEN(0),
        EYEBROWS(1),
        EYES(2),
        MOUTH(3),
        OTHER(4),
    }

    enum class Type(val value: Int) {
        GROUP(0),
        VERTEX(1),
        BONE(2),
        UV(3),
        UV_EXT1(4),
        UV_EXT2(5),
        UV_EXT3(6),
        UV_EXT4(7),
        MATERIAL(8),
        FLIP(9),
        IMPULSE(10),
    }
}