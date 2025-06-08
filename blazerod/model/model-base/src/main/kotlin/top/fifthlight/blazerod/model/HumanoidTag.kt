package top.fifthlight.blazerod.model

enum class HumanoidTag(
    val vrmName: String? = null,
    val pmxEnglish: String? = null,
    val pmxJapanese: String? = null,
) {
    CENTER(vrmName = "hips", pmxEnglish = "Root", pmxJapanese = "センター"),
    HEAD(vrmName = "head", pmxEnglish = "Head", pmxJapanese = "頭");

    companion object {
        private val vrmNameMap = entries.associateBy { it.vrmName }
        private val pmxEnglishMap = entries.associateBy { it.pmxEnglish }
        private val pmxJapaneseMap = entries.associateBy { it.pmxJapanese }

        fun fromVrmName(name: String): HumanoidTag? = vrmNameMap[name]
        fun fromPmxEnglish(name: String): HumanoidTag? = pmxEnglishMap[name]
        fun fromPmxJapanese(name: String): HumanoidTag? = pmxJapaneseMap[name]
    }
}
