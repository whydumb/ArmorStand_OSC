package top.fifthlight.armorstand.state

import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.util.math.MathHelper
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.armorstand.animation.AnimationItem
import top.fifthlight.armorstand.animation.Timeline
import top.fifthlight.armorstand.model.ModelInstance
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.armorstand.util.toRadian
import top.fifthlight.renderer.model.HumanoidTag

sealed class ModelController {
    open fun update(vanillaState: PlayerEntityRenderState) = Unit
    abstract fun apply(instance: ModelInstance)

    private class JointItem(
        private val initialMatrix: Matrix4fc,
        private val index: Int,
    ) {
        private val targetMatrix = Matrix4f()

        inline fun update(instance: ModelInstance, crossinline func: Matrix4f.() -> Unit) {
            targetMatrix.set(initialMatrix)
            func(targetMatrix)
            instance.setTransformMatrix(index, targetMatrix)
        }
    }

    class LiveUpdated private constructor(
        private val center: JointItem?,
        private val head: JointItem?,
    ) : ModelController() {
        private var bodyYaw: Float = 0f
        private var headYaw: Float = 0f
        private var headPitch: Float = 0f

        constructor(scene: RenderScene): this(
            center = scene.getBone(HumanoidTag.CENTER),
            head = scene.getBone(HumanoidTag.HEAD),
        )

        companion object {
            private fun RenderScene.getBone(tag: HumanoidTag) =
                humanoidTagTransformMap.getInt(tag).takeIf { it != -1 }?.let { index ->
                    JointItem(
                        initialMatrix = defaultTransforms[index]?.matrix ?: Matrix4f(),
                        index = index,
                    )
                }
        }

        override fun update(vanillaState: PlayerEntityRenderState) {
            bodyYaw = MathHelper.PI - vanillaState.bodyYaw.toRadian()
            headYaw = -vanillaState.relativeHeadYaw.toRadian()
            headPitch = -vanillaState.pitch.toRadian()
        }

        override fun apply(instance: ModelInstance) {
            center?.update(instance) {
                rotateY(bodyYaw)
            }
            head?.update(instance) {
                rotateYXZ(headYaw, headPitch, 0f)
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
                ).also {
                    it.play()
                }
            )
        }

        override fun apply(instance: ModelInstance) {
            items.forEach {
                it.animation.apply(instance, it.timeline.currentTime)
            }
        }
    }
}