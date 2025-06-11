package top.fifthlight.armorstand.state

import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.util.math.MathHelper
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.armorstand.ui.model.AnimationViewModel
import top.fifthlight.blazerod.animation.AnimationItem
import top.fifthlight.blazerod.animation.Timeline
import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.RenderExpression
import top.fifthlight.blazerod.model.RenderNode
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.util.toRadian
import top.fifthlight.blazerod.model.Expression
import top.fifthlight.blazerod.model.HumanoidTag
import top.fifthlight.blazerod.model.RenderExpressionGroup
import java.util.UUID
import kotlin.math.abs

sealed class ModelController {
    open fun update(uuid: UUID, vanillaState: PlayerEntityRenderState) = Unit
    abstract fun apply(instance: ModelInstance)

    private class JointItem(
        private val initialMatrix: Matrix4fc,
        private val transformIndex: Int,
    ) {
        private val targetMatrix = Matrix4f()

        inline fun update(instance: ModelInstance, crossinline func: Matrix4f.() -> Unit) {
            targetMatrix.set(initialMatrix)
            func(targetMatrix)
            instance.setTransformMatrix(transformIndex, targetMatrix)
        }
    }

    private sealed class ExpressionItem {
        abstract fun apply(instance: ModelInstance, weight: Float)

        fun RenderExpression.apply(instance: ModelInstance, weight: Float) = bindings.forEach { binding ->
            when (binding) {
                is RenderExpression.Binding.MorphTarget -> {
                    instance.setGroupWeight(binding.morphedPrimitiveIndex, binding.groupIndex, weight)
                }
            }
        }

        data class Expression(
            val expression: RenderExpression,
        ) : ExpressionItem() {
            override fun apply(instance: ModelInstance, weight: Float) = expression.apply(instance, weight)
        }

        data class Group(
            val group: RenderExpressionGroup,
        ) : ExpressionItem() {
            override fun apply(instance: ModelInstance, weight: Float) = group.items.forEach { item ->
                val expression = instance.scene.expressions[item.expressionIndex]
                expression.apply(instance, weight * item.influence)
            }
        }
    }

    class LiveUpdated private constructor(
        private val center: JointItem?,
        private val head: JointItem?,
        private val blinkExpression: ExpressionItem?,
    ) : ModelController() {
        private var bodyYaw: Float = 0f
        private var headYaw: Float = 0f
        private var headPitch: Float = 0f
        private var blinkProgress: Float = 0f

        constructor(scene: RenderScene) : this(
            center = scene.getBone(HumanoidTag.CENTER),
            head = scene.getBone(HumanoidTag.HEAD),
            blinkExpression = scene.getExpression(Expression.Tag.BLINK),
        )

        companion object {
            private fun RenderScene.getBone(tag: HumanoidTag) =
                humanoidTagToTransformMap.getInt(tag).takeIf { it != -1 }?.let { transformIndex ->
                    val nodeIndex = transformNodeIndices.getInt(transformIndex)
                    val transformNode = nodes[nodeIndex] as RenderNode.Transform
                    JointItem(
                        initialMatrix = transformNode.defaultTransform?.matrix ?: Matrix4f(),
                        transformIndex = transformIndex,
                    )
                }

            private fun RenderScene.getExpression(tag: Expression.Tag) =
                expressions.firstOrNull { it.tag == tag }?.let { ExpressionItem.Expression(it) }
                    ?: expressionGroups.firstOrNull { it.tag == tag }?.let { ExpressionItem.Group(it) }
        }

        override fun update(uuid: UUID, vanillaState: PlayerEntityRenderState) {
            bodyYaw = MathHelper.PI - vanillaState.bodyYaw.toRadian()
            headYaw = -vanillaState.relativeHeadYaw.toRadian()
            headPitch = -vanillaState.pitch.toRadian()
            blinkProgress = abs(System.currentTimeMillis() % 2000 - 1000) / 1000f
        }

        override fun apply(instance: ModelInstance) {
            center?.update(instance) {
                rotateY(bodyYaw)
            }
            head?.update(instance) {
                rotateYXZ(headYaw, headPitch, 0f)
            }
            blinkExpression?.apply(instance, blinkProgress)
        }
    }

    class Predefined(
        private val animation: AnimationItem,
    ) : ModelController() {
        val timeline: Timeline = Timeline(
            duration = animation.duration.toDouble(),
            speed = AnimationViewModel.playSpeed.value,
            loop = true,
        ).also { timeline ->
            timeline.play(System.nanoTime())
        }

        override fun apply(instance: ModelInstance) {
            val time = timeline.getCurrentTime(System.nanoTime())
            animation.apply(instance, time.toFloat())
        }
    }
}