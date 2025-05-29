package top.fifthlight.armorstand.state

import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.util.math.MathHelper
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.armorstand.animation.AnimationItem
import top.fifthlight.armorstand.animation.Timeline
import top.fifthlight.armorstand.model.ModelInstance
import top.fifthlight.armorstand.model.RenderExpression
import top.fifthlight.armorstand.model.RenderNode
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.armorstand.util.toRadian
import top.fifthlight.renderer.model.Expression
import top.fifthlight.renderer.model.HumanoidTag
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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

    class LiveUpdated private constructor(
        private val center: JointItem?,
        private val head: JointItem?,
        private val blinkExpression: RenderExpression?,
    ) : ModelController() {
        private var bodyYaw: Float = 0f
        private var headYaw: Float = 0f
        private var headPitch: Float = 0f
        private var blinkProgress: Float = 0f

        constructor(scene: RenderScene) : this(
            center = scene.getBone(HumanoidTag.CENTER),
            head = scene.getBone(HumanoidTag.HEAD),
            blinkExpression = scene.expressions.firstOrNull { it.tag == Expression.Tag.Blink.BLINK },
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
            blinkExpression?.let { expression ->
                expression.bindings.forEach { binding ->
                    when (binding) {
                        is RenderExpression.Binding.MorphTarget -> {
                            instance.setGroupWeight(binding.morphedPrimitiveIndex, binding.groupIndex, blinkProgress)
                        }
                    }
                }
            }
        }
    }

    class Predefined(
        animations: List<AnimationItem>,
    ) : ModelController() {
        private data class Item(
            val animation: AnimationItem,
            val timeline: Timeline,
        )

        private val items = animations.map {
            Item(
                animation = it,
                timeline = Timeline(
                    duration = it.duration,
                    loop = true,
                ).also { timeline ->
                    timeline.play(System.nanoTime())
                }
            )
        }

        override fun apply(instance: ModelInstance) {
            items.forEach {
                it.animation.apply(instance, it.timeline.getCurrentTime(System.nanoTime()))
            }
        }
    }
}