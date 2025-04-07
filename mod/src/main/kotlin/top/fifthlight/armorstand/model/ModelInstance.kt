package top.fifthlight.armorstand.model

import net.minecraft.client.util.math.MatrixStack
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.NodeTransform

class ModelInstance private constructor(
    val scene: RenderScene,
    val transforms: Array<NodeTransform?>,
    val skinData: Array<RenderSkinData?>,
): AbstractRefCount() {
    constructor(scene: RenderScene): this(
        scene = scene,
        transforms = Array(scene.defaultTransforms.size) { scene.defaultTransforms[it]?.clone() },
        skinData = Array(scene.skins.size) {
            val skin = scene.skins[it]
            RenderSkinData(skin)
        },
    )

    init {
        scene.increaseReferenceCount()
    }

    fun render(matrixStack: MatrixStack, light: Int) {
        scene.render(this, matrixStack, light)
    }

    override fun onClosed() {
        scene.decreaseReferenceCount()
    }
}