package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import top.fifthlight.armorstand.util.AbstractRefCount

class ModelInstance private constructor(
    val scene: RenderScene,
    val transforms: Array<Matrix4f>,
    val transformsDirty: Array<Boolean>,
    val skinData: Array<RenderSkinData>,
) : AbstractRefCount() {
    constructor(scene: RenderScene) : this(
        scene = scene,
        transforms = Array(scene.defaultTransforms.size) {
            Matrix4f().also { matrix ->
                scene.defaultTransforms[it]?.matrix?.let { matrix.set(it) }
            }
        },
        transformsDirty = Array(scene.defaultTransforms.size) { true },
        skinData = Array(scene.skins.size) {
            val skin = scene.skins[it]
            RenderSkinData(skin)
        },
    )

    init {
        scene.increaseReferenceCount()
    }

    private val updateMatrixStack = MatrixStack()

    fun setTransform(index: Int, transform: Matrix4f) {
        transforms[index].set(transform)
        transformsDirty[index] = true
    }

    fun update() {
        scene.update(this, updateMatrixStack)
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()
        skinData.forEach { it.upload(device, commandEncoder) }
    }

    fun render(matrixStack: MatrixStack, light: Int) {
        scene.render(this, matrixStack, light)
    }

    override fun onClosed() {
        scene.decreaseReferenceCount()
    }
}