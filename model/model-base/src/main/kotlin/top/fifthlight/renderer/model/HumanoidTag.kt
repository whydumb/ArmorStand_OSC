package top.fifthlight.renderer.model

enum class HumanoidTag(
    val vrmName: String? = null,
    val pmxEnglish: String? = null,
    val pmxJapanese: String? = null,
) {
    CENTER(vrmName = "hips", pmxEnglish = "Root", pmxJapanese = "センター"),
    HEAD(vrmName = "head", pmxEnglish = "Head", pmxJapanese = "頭");

    companion object {
        private val vrmNameMap = entries.associate { Pair(it.vrmName, it) }
        private val pmxEnglishMap = entries.associate { Pair(it.pmxEnglish, it) }
        private val pmxJapaneseMap = entries.associate { Pair(it.pmxJapanese, it) }

        fun fromVrmName(name: String): HumanoidTag? = vrmNameMap[name]
        fun fromPmxEnglish(name: String): HumanoidTag? = pmxEnglishMap[name]
        fun fromPmxJapanese(name: String): HumanoidTag? = pmxJapaneseMap[name]
    }
}
