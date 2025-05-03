package top.fifthlight.armorstand.ui.component

data class Insets(
    val top: Int,
    val left: Int,
    val bottom: Int,
    val right: Int,
) {
    constructor(all: Int) : this(all, all, all, all)
    constructor(vertical: Int, horizonal: Int) : this(vertical, horizonal, vertical, horizonal)

    companion object {
        val ZERO = Insets(0)
    }
}
