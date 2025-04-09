package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.NodeTransform

class ModelInstance private constructor(
    val scene: RenderScene,
    val transforms: Array<NodeTransform?>,
    val transformsDirty: Array<Boolean>,
    val skinData: Array<RenderSkinData>,
) : AbstractRefCount() {
    constructor(scene: RenderScene) : this(
        scene = scene,
        transforms = Array(scene.defaultTransforms.size) { scene.defaultTransforms[it] },
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

    fun setTransformMatrix(index: Int, matrix: Matrix4f) {
        val transform = transforms[index] ?: NodeTransform.Matrix()
        if (transform is NodeTransform.Matrix) {
            transform.matrix.set(matrix)
        } else {
            transforms[index] = NodeTransform.Matrix(matrix)
        }
        transformsDirty[index] = true
    }

    inline fun setTransformDecomposed(index: Int, crossinline updater: NodeTransform.Decomposed.() -> Unit) {
        val transform = transforms[index] ?: NodeTransform.Decomposed()
        if (transform is NodeTransform.Decomposed) {
            updater(transform)
        } else {
            val prevMatrix = transform.matrix
            val newTransform = NodeTransform.Decomposed(
                translation = Vector3f().also { prevMatrix.getTranslation(it) },
                scale = Vector3f().also { prevMatrix.getScale(it) },
                rotation = Quaternionf().also { prevMatrix.getNormalizedRotation(it) },
            )
            updater(newTransform)
            transforms[index] = newTransform
        }
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

    fun renderDebug(matrixStack: MatrixStack, vertexConsumerProvider: VertexConsumerProvider) {
        scene.renderDebug(this, matrixStack, vertexConsumerProvider)
    }

    override fun onClosed() {
        scene.decreaseReferenceCount()
    }
}