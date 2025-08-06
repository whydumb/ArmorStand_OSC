@file:Suppress("NOTHING_TO_INLINE")

package top.fifthlight.blazerod.model.renderer

import top.fifthlight.blazerod.model.resource.RenderMaterial
import top.fifthlight.blazerod.util.BitmapItem

@JvmInline
value class PipelineInfo(val bitmap: BitmapItem = BitmapItem()) {
    constructor(
        doubleSided: Boolean = true,
        skinned: Boolean = false,
        instanced: Boolean = false,
        morphed: Boolean = false,
    ) : this(Unit.run {
        var item = BitmapItem()
        if (doubleSided) {
            item += ELEMENT_DOUBLE_SIDED
        }
        if (skinned) {
            item += ELEMENT_SKINNED
        }
        if (instanced) {
            item += ELEMENT_INSTANCED
        }
        if (morphed) {
            item += ELEMENT_MORPHED
        }
        item
    })

    constructor(material: RenderMaterial<*>, instanced: Boolean): this(
        doubleSided = material.doubleSided,
        skinned = material.skinned,
        instanced = instanced,
        morphed = material.morphed
    )

    val doubleSided
        get() = ELEMENT_DOUBLE_SIDED in bitmap
    val skinned
        get() = ELEMENT_SKINNED in bitmap
    val instanced
        get() = ELEMENT_INSTANCED in bitmap
    val morphed
        get() = ELEMENT_MORPHED in bitmap

    fun nameSuffix() = buildString {
        if (doubleSided) {
            append("_cull")
        } else {
            append("_no_cull")
        }
        if (skinned) {
            append("_skinned")
        }
        if (instanced) {
            append("_instanced")
        }
        if (morphed) {
            append("_morphed")
        }
    }

    companion object {
        val ELEMENT_DOUBLE_SIDED = BitmapItem.Element.of(0)
        val ELEMENT_SKINNED = BitmapItem.Element.of(1)
        val ELEMENT_INSTANCED = BitmapItem.Element.of(2)
        val ELEMENT_MORPHED = BitmapItem.Element.of(3)
    }

    inline operator fun plus(element: BitmapItem.Element) =
        PipelineInfo(bitmap + element)

    inline operator fun minus(element: BitmapItem.Element) =
        PipelineInfo(bitmap - element)

    inline operator fun contains(element: BitmapItem.Element) =
        element in bitmap

    override fun toString(): String {
        return "PipelineInfo(doubleSided=$doubleSided, skinned=$skinned, instanced=$instanced, morphed=$morphed)"
    }
}