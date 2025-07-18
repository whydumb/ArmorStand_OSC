package top.fifthlight.armorstand.state

import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import top.fifthlight.armorstand.util.ModelLoaders
import top.fifthlight.blazerod.animation.AnimationItem
import top.fifthlight.blazerod.animation.AnimationLoader
import top.fifthlight.blazerod.model.ModelFileLoaders
import top.fifthlight.blazerod.model.RenderScene
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

data class AnimationSet(
    val idle: AnimationItem? = null,
    val walk: AnimationItem? = null,
    val sprint: AnimationItem? = null,
    val sneak: AnimationItem? = null,
    val swingRight: AnimationItem? = null,
    val swingLeft: AnimationItem? = null,
    val elytraFly: AnimationItem? = null,
    val swim: AnimationItem? = null,
    val onClimbable: AnimationItem? = null,
    val onClimbableUp: AnimationItem? = null,
    val onClimbableDown: AnimationItem? = null,
    val sleep: AnimationItem? = null,
    val ride: AnimationItem? = null,
    val die: AnimationItem? = null,
    val onHorse: AnimationItem? = null,
    val crawl: AnimationItem? = null,
    val lieDown: AnimationItem? = null,
    val custom: Map<String, AnimationItem> = emptyMap(),
    val itemActive: Map<ItemActiveKey, AnimationItem> = emptyMap(),
) {
    data class ItemActiveKey(
        val itemName: Identifier,
        val hand: HandSide,
        val actionType: ActionType,
    ) {
        enum class HandSide { LEFT, RIGHT }
        enum class ActionType { USING, SWINGING }
    }

    operator fun plus(other: AnimationSet): AnimationSet {
        if (other == EMPTY) {
            return this
        } else if (this == EMPTY) {
            return other
        }
        return AnimationSet(
            idle = other.idle ?: this.idle,
            walk = other.walk ?: this.walk,
            sprint = other.sprint ?: this.sprint,
            sneak = other.sneak ?: this.sneak,
            swingRight = other.swingRight ?: this.swingRight,
            swingLeft = other.swingLeft ?: this.swingLeft,
            elytraFly = other.elytraFly ?: this.elytraFly,
            swim = other.swim ?: this.swim,
            onClimbable = other.onClimbable ?: this.onClimbable,
            onClimbableUp = other.onClimbableUp ?: this.onClimbableUp,
            onClimbableDown = other.onClimbableDown ?: this.onClimbableDown,
            sleep = other.sleep ?: this.sleep,
            ride = other.ride ?: this.ride,
            die = other.die ?: this.die,
            onHorse = other.onHorse ?: this.onHorse,
            crawl = other.crawl ?: this.crawl,
            lieDown = other.lieDown ?: this.lieDown,
            custom = this.custom + other.custom,
            itemActive = this.itemActive + other.itemActive,
        )
    }

    companion object {
        val EMPTY = AnimationSet()
    }
}

data class FullAnimationSet(
    val idle: AnimationItem,
    val walk: AnimationItem,
    val sprint: AnimationItem,
    val sneak: AnimationItem,
    val swingRight: AnimationItem,
    val swingLeft: AnimationItem,
    val elytraFly: AnimationItem,
    val swim: AnimationItem,
    val onClimbable: AnimationItem,
    val onClimbableUp: AnimationItem,
    val onClimbableDown: AnimationItem,
    val sleep: AnimationItem,
    val ride: AnimationItem,
    val die: AnimationItem,
    val onHorse: AnimationItem,
    val crawl: AnimationItem,
    val lieDown: AnimationItem,
    val custom: Map<String, AnimationItem> = emptyMap(),
    val itemActive: Map<AnimationSet.ItemActiveKey, AnimationItem> = emptyMap(),
) {
    companion object {
        fun from(animationSet: AnimationSet): FullAnimationSet? {
            return animationSet.idle?.let { idle ->
                FullAnimationSet(
                    idle = idle,
                    walk = animationSet.walk ?: idle,
                    sprint = animationSet.sprint ?: animationSet.walk ?: idle,
                    sneak = animationSet.sneak ?: idle,
                    swingRight = animationSet.swingRight ?: animationSet.swingLeft ?: idle,
                    swingLeft = animationSet.swingLeft ?: animationSet.swingRight ?: idle,
                    elytraFly = animationSet.elytraFly ?: animationSet.swim ?: animationSet.walk ?: idle,
                    swim = animationSet.swim ?: animationSet.walk ?: idle,
                    onClimbable = animationSet.onClimbable ?: animationSet.onClimbableUp ?: animationSet.onClimbableDown
                    ?: idle,
                    onClimbableUp = animationSet.onClimbableUp ?: animationSet.onClimbableDown
                    ?: animationSet.onClimbable ?: idle,
                    onClimbableDown = animationSet.onClimbableDown ?: animationSet.onClimbableUp
                    ?: animationSet.onClimbable ?: idle,
                    sleep = animationSet.sleep ?: animationSet.lieDown ?: idle,
                    ride = animationSet.ride ?: animationSet.onHorse ?: idle,
                    die = animationSet.die ?: animationSet.lieDown ?: animationSet.sleep ?: idle,
                    onHorse = animationSet.onHorse ?: animationSet.ride ?: idle,
                    crawl = animationSet.crawl ?: animationSet.sneak ?: idle,
                    lieDown = animationSet.lieDown ?: animationSet.sleep ?: idle,
                    custom = animationSet.custom,
                    itemActive = animationSet.itemActive
                )
            }
        }
    }
}

data object AnimationSetLoader {
    private val logger = LoggerFactory.getLogger(AnimationSetLoader::class.java)

    fun load(scene: RenderScene, directory: Path): AnimationSet {
        val files = try {
            if (!directory.isDirectory()) {
                return AnimationSet.EMPTY
            }
            directory.listDirectoryEntries()
        } catch (ex: Exception) {
            logger.warn("Failed to list animation directory: $directory", ex)
            return AnimationSet.EMPTY
        }

        var idle: AnimationItem? = null
        var walk: AnimationItem? = null
        var sprint: AnimationItem? = null
        var sneak: AnimationItem? = null
        var swingRight: AnimationItem? = null
        var swingLeft: AnimationItem? = null
        var elytraFly: AnimationItem? = null
        var swim: AnimationItem? = null
        var onClimbable: AnimationItem? = null
        var onClimbableUp: AnimationItem? = null
        var onClimbableDown: AnimationItem? = null
        var sleep: AnimationItem? = null
        var ride: AnimationItem? = null
        var die: AnimationItem? = null
        var onHorse: AnimationItem? = null
        var crawl: AnimationItem? = null
        var lieDown: AnimationItem? = null
        val custom: MutableMap<String, AnimationItem> = mutableMapOf()
        val itemActive: MutableMap<AnimationSet.ItemActiveKey, AnimationItem> = mutableMapOf()

        for (file in files) {
            val name = file.nameWithoutExtension
            val extension = file.extension
            if (extension !in ModelLoaders.animationExtensions) {
                continue
            }

            fun load() = try {
                val result = ModelFileLoaders.probeAndLoad(file, directory)
                val animation = result?.animations?.firstOrNull() ?: return null
                AnimationLoader.load(scene, animation)
            } catch (ex: Exception) {
                logger.warn("Failed to load animation file: $file", ex)
                null
            }

            when (name.lowercase()) {
                "idle" -> load()?.let { idle = it }
                "walk" -> load()?.let { walk = it }
                "sprint" -> load()?.let { sprint = it }
                "sneak" -> load()?.let { sneak = it }
                "swingright" -> load()?.let { swingRight = it }
                "swingleft" -> load()?.let { swingLeft = it }
                "elytraFly" -> load()?.let { elytraFly = it }
                "swim" -> load()?.let { swim = it }
                "onclimbable" -> load()?.let { onClimbable = it }
                "onclimbableup" -> load()?.let { onClimbableUp = it }
                "onclimbabledown" -> load()?.let { onClimbableDown = it }
                "sleep" -> load()?.let { sleep = it }
                "ride" -> load()?.let { ride = it }
                "die" -> load()?.let { die = it }
                "onhorse" -> load()?.let { onHorse = it }
                "crawl" -> load()?.let { crawl = it }
                "liedown" -> load()?.let { lieDown = it }
                else -> when {
                    name.startsWith("custom") -> {
                        val name = name.substringAfter("custom")
                        load()?.let { custom[name] = it }
                    }

                    name.startsWith("itemActive") -> {
                        val name = name.substringAfter("itemActive")
                        val parts = name.split("_")
                        if (parts.size != 3) {
                            continue
                        }
                        val (itemName, hand, action) = parts
                        val item = try {
                            Identifier.of(itemName.lowercase())
                        } catch (ex: Exception) {
                            logger.warn("Bad item name: $itemName", ex)
                            continue
                        }
                        val handSide = when (hand.lowercase()) {
                            "left" -> AnimationSet.ItemActiveKey.HandSide.LEFT
                            "right" -> AnimationSet.ItemActiveKey.HandSide.RIGHT
                            else -> continue
                        }
                        val actionType = when (action.lowercase()) {
                            "using" -> AnimationSet.ItemActiveKey.ActionType.USING
                            "swinging" -> AnimationSet.ItemActiveKey.ActionType.SWINGING
                            else -> continue
                        }
                        val animation = load() ?: continue
                        itemActive[AnimationSet.ItemActiveKey(item, handSide, actionType)] = animation
                    }
                }
            }
        }

        return AnimationSet(
            idle = idle,
            walk = walk,
            sprint = sprint,
            sneak = sneak,
            swingRight = swingRight,
            swingLeft = swingLeft,
            elytraFly = elytraFly,
            swim = swim,
            onClimbable = onClimbable,
            onClimbableUp = onClimbableUp,
            onClimbableDown = onClimbableDown,
            sleep = sleep,
            ride = ride,
            die = die,
            onHorse = onHorse,
            crawl = crawl,
            lieDown = lieDown,
        )
    }
}