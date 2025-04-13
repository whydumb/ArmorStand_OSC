package top.fifthlight.armorstand.model

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.RenderLayers
import top.fifthlight.armorstand.util.iteratorOf

sealed class RenderNode : AbstractRefCount(), Iterable<RenderNode> {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "node")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    var parent: RenderNode? = null

    open fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) = Unit
    open fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) = Unit
    open fun renderDebug(
        instance: ModelInstance,
        matrixStack: MatrixStack,
        globalMatrix: Matrix4fc,
        vertexConsumerProvider: VertexConsumerProvider,
        textRenderer: TextRenderer,
        dispatcher: EntityRenderDispatcher,
        light: Int,
    ) = Unit

    class Group(val children: List<RenderNode>) : RenderNode() {
        init {
            children.forEach { it.increaseReferenceCount() }
        }

        override fun onClosed() {
            children.forEach { it.decreaseReferenceCount() }
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) =
            children.forEach { it.render(instance, matrixStack, globalMatrix, light) }

        override fun renderDebug(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            globalMatrix: Matrix4fc,
            vertexConsumerProvider: VertexConsumerProvider,
            textRenderer: TextRenderer,
            dispatcher: EntityRenderDispatcher,
            light: Int,
        ) = children.forEach {
            it.renderDebug(
                instance,
                matrixStack,
                globalMatrix,
                vertexConsumerProvider,
                textRenderer,
                dispatcher,
                light
            )
        }

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) =
            children.forEach { it.update(instance, matrixStack, updateTransform) }

        override fun iterator(): Iterator<RenderNode> = children.iterator()
    }

    class Mesh(
        val mesh: RenderMesh,
        val skinIndex: Int?,
    ) : RenderNode() {
        init {
            mesh.increaseReferenceCount()
        }

        override fun onClosed() {
            mesh.decreaseReferenceCount()
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) {
            val skinData = skinIndex?.let { instance.skinData[it] }
            if (skinData != null) {
                mesh.render(globalMatrix, light, skinData)
            } else {
                mesh.render(matrixStack.peek().positionMatrix, light, skinData)
            }
        }

        override fun iterator() = iteratorOf<RenderNode>()
    }

    class Transform(
        val transformIndex: Int,
        val child: RenderNode,
    ) : RenderNode() {
        init {
            child.increaseReferenceCount()
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) {
            val transform = instance.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.push()
                matrixStack.multiplyPositionMatrix(matrix)
                child.render(instance, matrixStack, globalMatrix, light)
                matrixStack.pop()
            } ?: run {
                child.render(instance, matrixStack, globalMatrix, light)
            }
        }

        override fun renderDebug(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            globalMatrix: Matrix4fc,
            vertexConsumerProvider: VertexConsumerProvider,
            textRenderer: TextRenderer,
            dispatcher: EntityRenderDispatcher,
            light: Int,
        ) {
            val transform = instance.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.push()
                matrixStack.multiplyPositionMatrix(matrix)
                child.renderDebug(
                    instance,
                    matrixStack,
                    globalMatrix,
                    vertexConsumerProvider,
                    textRenderer,
                    dispatcher,
                    light,
                )
                matrixStack.pop()
            } ?: run {
                child.renderDebug(
                    instance,
                    matrixStack,
                    globalMatrix,
                    vertexConsumerProvider,
                    textRenderer,
                    dispatcher,
                    light,
                )
            }
        }

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) {
            val transformsDirty = instance.transformsDirty[transformIndex]
            if (transformsDirty) {
                instance.transformsDirty[transformIndex] = false
            }
            val updateTransform = updateTransform || transformsDirty
            val transform = instance.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.push()
                matrixStack.multiplyPositionMatrix(matrix)
                child.update(instance, matrixStack, updateTransform)
                matrixStack.pop()
            } ?: run {
                child.update(instance, matrixStack, updateTransform)
            }
        }

        override fun iterator() = iteratorOf<RenderNode>(child)

        override fun onClosed() {
            child.decreaseReferenceCount()
        }
    }

    class Joint(
        val name: String?,
        val skinIndex: Int,
        val jointIndex: Int,
    ) : RenderNode() {
        private val cacheMatrix = Matrix4f()
        private val translation = Vector3f()

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) {
            if (!updateTransform) {
                return
            }

            val matrix = cacheMatrix
            matrix.set(matrixStack.peek().positionMatrix)
            val skinData = instance.skinData[skinIndex]

            val inverseMatrix = skinData.skin.inverseBindMatrices?.get(jointIndex)
            inverseMatrix?.let { matrix.mul(it) }
            skinData.setMatrix(jointIndex, matrix)
        }

        private val parentJoint: Joint? by lazy {
            var current = parent

            while (current != null) {
                when (current) {
                    is Transform -> {
                        current = current.parent
                        continue
                    }

                    is Group -> {
                        for (sibling in current.children) {
                            if (sibling is Joint && sibling != this && sibling.skinIndex == skinIndex) {
                                return@lazy sibling
                            }
                        }
                    }

                    else -> return@lazy null
                }
                current = current.parent
            }

            null
        }

        override fun renderDebug(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            globalMatrix: Matrix4fc,
            vertexConsumerProvider: VertexConsumerProvider,
            textRenderer: TextRenderer,
            dispatcher: EntityRenderDispatcher,
            light: Int,
        ) {
            if (ArmorStandClient.showBoneLabel) {
                name?.let { name ->
                    val x = -textRenderer.getWidth(name) / 2f
                    matrixStack.push()
                    matrixStack.peek().positionMatrix.getTranslation(translation)
                    matrixStack.peek().positionMatrix.translation(translation)
                    matrixStack.peek().rotate(dispatcher.rotation)
                    matrixStack.peek().scale(0.005F, -0.005F, 0.005F)
                    textRenderer.draw(
                        name,
                        x,
                        0f,
                        Colors.WHITE,
                        false,
                        matrixStack.peek().positionMatrix,
                        vertexConsumerProvider,
                        TextRenderer.TextLayerType.NORMAL,
                        0x33000000,
                        light
                    )
                    matrixStack.pop()
                }
            }

            val matrix = matrixStack.peek().positionMatrix
            cacheMatrix.set(matrix)
            val buffer = vertexConsumerProvider.getBuffer(RenderLayers.DEBUG_BONE_LINES)
            buffer.vertex(matrix, 0f, 0f, 0f).color(Colors.RED)
            buffer.vertex(matrix, 0.01f, 0f, 0f).color(Colors.RED)
            buffer.vertex(matrix, 0f, 0f, 0f).color(Colors.GREEN)
            buffer.vertex(matrix, 0f, 0.01f, 0f).color(Colors.GREEN)
            buffer.vertex(matrix, 0f, 0f, 0f).color(Colors.BLUE)
            buffer.vertex(matrix, 0f, 0f, 0.01f).color(Colors.BLUE)
            parentJoint?.let { parent ->
                buffer.vertex(parent.cacheMatrix, 0f, 0f, 0f).color(Colors.YELLOW)
                buffer.vertex(matrix, 0f, 0f, 0f).color(Colors.CYAN)
            }
        }

        override fun iterator() = iteratorOf<RenderNode>()

        override fun onClosed() {}
    }
}
