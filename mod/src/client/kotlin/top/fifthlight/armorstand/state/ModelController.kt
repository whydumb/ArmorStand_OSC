package top.fifthlight.armorstand.state

import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.util.math.MathHelper
import top.fifthlight.armorstand.extension.internal.PlayerEntityRenderStateExtInternal
import top.fifthlight.armorstand.ui.model.AnimationViewModel
import top.fifthlight.armorstand.util.toRadian
import top.fifthlight.blazerod.animation.AnimationItem
import top.fifthlight.blazerod.animation.Timeline
import top.fifthlight.blazerod.model.*
import top.fifthlight.blazerod.model.resource.RenderExpression
import top.fifthlight.blazerod.model.resource.RenderExpressionGroup
import java.util.*
import kotlin.math.abs

sealed class ModelController {
    open fun update(uuid: UUID, vanillaState: PlayerEntityRenderState) = Unit
    abstract fun apply(instance: ModelInstance)

    private class JointItem(
        private val nodeIndex: Int,
    ) {
        fun update(instance: ModelInstance, func: NodeTransform.Decomposed.() -> Unit) {
            instance.setTransformDecomposed(nodeIndex, TransformId.RELATIVE_ANIMATION, func)
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
                humanoidTagMap[tag]?.let { node -> JointItem(nodeIndex = node.nodeIndex) }

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
                rotation.rotationY(bodyYaw)
            }
            head?.update(instance) {
                rotation.rotationYXZ(headYaw, headPitch, 0f)
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

    class LiveSwitched(
        private val animationSet: FullAnimationSet,
    ) : ModelController() {
        sealed class State {
            abstract fun getItem(set: FullAnimationSet): AnimationItem

            data object Idle : State() {
                override fun getItem(set: FullAnimationSet) = set.idle
            }

            data object Walking : State() {
                override fun getItem(set: FullAnimationSet) = set.walk
            }

            data object ElytraFly : State() {
                override fun getItem(set: FullAnimationSet) = set.elytraFly
            }

            data object Swimming : State() {
                override fun getItem(set: FullAnimationSet) = set.swim
            }

            data object Sleeping : State() {
                override fun getItem(set: FullAnimationSet) = set.sleep
            }

            data object Riding : State() {
                override fun getItem(set: FullAnimationSet) = set.ride
            }

            data object OnHorse : State() {
                override fun getItem(set: FullAnimationSet) = set.onHorse
            }

            data object Dying : State() {
                override fun getItem(set: FullAnimationSet) = set.die
            }

            data object Sprinting : State() {
                override fun getItem(set: FullAnimationSet) = set.sprint
            }

            data object Sneaking : State() {
                override fun getItem(set: FullAnimationSet) = set.sneak
            }
        }

        private var state: State = State.Idle
        private var item: AnimationItem? = null
        private var timeline: Timeline? = null
        private var reset = false
        private var bodyYaw: Float = 0f

        companion object {
            private val PlayerEntityRenderState.vehicleType
                get() = (this as PlayerEntityRenderStateExtInternal).`armorStand$getRidingEntityType`()
            private val PlayerEntityRenderState.isSprinting
                get() = (this as PlayerEntityRenderStateExtInternal).`armorStand$isSprinting`()
            private val horseEntityTypes = listOf(
                EntityType.HORSE,
                EntityType.DONKEY,
                EntityType.MULE,
                EntityType.LLAMA,
                EntityType.SKELETON_HORSE,
                EntityType.ZOMBIE_HORSE,
            )
        }

        private fun getState(vanillaState: PlayerEntityRenderState): State = when (vanillaState.pose) {
            EntityPose.STANDING -> if (vanillaState.isSprinting) {
                State.Sprinting
            } else if (vanillaState.limbAmplitudeInverse.also { println(it) } > 0.2) {
                State.Walking
            } else {
                State.Idle
            }

            EntityPose.SITTING -> if (vanillaState.vehicleType in horseEntityTypes) {
                State.OnHorse
            } else {
                State.Riding
            }

            EntityPose.CROUCHING -> State.Sneaking
            EntityPose.GLIDING -> State.ElytraFly
            EntityPose.SLEEPING -> State.Sleeping
            EntityPose.SWIMMING -> State.Swimming
            EntityPose.DYING -> State.Dying

            else -> State.Idle
        }

        override fun update(uuid: UUID, vanillaState: PlayerEntityRenderState) {
            bodyYaw = MathHelper.PI - vanillaState.bodyYaw.toRadian()
            val newState = getState(vanillaState)
            if (newState != state) {
                this.state = newState
            }
            val newItem = newState.getItem(animationSet)
            if (newItem != item || timeline == null) {
                timeline = Timeline(
                    duration = newItem.duration.toDouble(),
                    speed = AnimationViewModel.playSpeed.value,
                    loop = true,
                )
                item = newItem
                reset = true
            }
        }

        override fun apply(instance: ModelInstance) {
            instance.setTransformDecomposed(instance.scene.rootNode.nodeIndex, TransformId.RELATIVE_ANIMATION) {
                rotation.rotationY(bodyYaw)
            }
            val timeline = timeline ?: return
            val item = item ?: return
            if (reset) {
                instance.clearTransform()
                reset = false
            }
            val time = timeline.getCurrentTime(System.nanoTime())
            item.apply(instance, time.toFloat())
        }
    }
}
