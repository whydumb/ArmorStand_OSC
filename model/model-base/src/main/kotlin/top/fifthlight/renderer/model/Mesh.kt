package top.fifthlight.renderer.model

data class Mesh(
    val primitives: List<Primitive>,
    val firstPersonFlag: FirstPersonFlag = FirstPersonFlag.BOTH,
) {
    enum class FirstPersonFlag {
        AUTO,
        THIRD_PERSON_ONLY,
        FIRST_PERSON_ONLY,
        BOTH,
    }
}