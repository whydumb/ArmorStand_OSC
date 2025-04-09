package top.fifthlight.renderer.model

data class Animation(
    val name: String? = null,
    val channels: List<AnimationChannel>,
)

enum class AnimationInterpolation(val outputMultiplier: Int) {
    LINEAR(1),
    STEP(1),
    CUBIC_SPLINE(3),
}

data class AnimationSampler(
    val input: Accessor,
    val interpolation: AnimationInterpolation,
    val output: Accessor,
) {
    init {
        require(input.componentType == Accessor.ComponentType.FLOAT) { "Animation's timeline input isn't FLOAT type, but ${input.componentType}" }
        require(input.type == Accessor.AccessorType.SCALAR) { "Animation's timeline input isn't scalar, but ${input.type}" }
        require(output.count == input.count * interpolation.outputMultiplier) { "Animation's output ${output.count} count doesn't match input ${input.count} for interpolation $interpolation" }
    }
}

data class AnimationChannel(
    val sampler: AnimationSampler,
    val targetNode: NodeId?,
    // For retargeting
    val targetNodeName: String?,
    val targetHumanoid: HumanoidTag?,
    val targetPath: Path,
) {
    enum class Path(
        val accessorType: Accessor.AccessorType,
        val componentType: List<Accessor.ComponentTypeItem>,
    ) {
        TRANSLATION(
            accessorType = Accessor.AccessorType.VEC3,
            componentType = listOf(Accessor.ComponentTypeItem(Accessor.ComponentType.FLOAT)),
        ),
        ROTATION(
            accessorType = Accessor.AccessorType.VEC4,
            componentType = listOf(
                Accessor.ComponentTypeItem(Accessor.ComponentType.FLOAT),
                Accessor.ComponentTypeItem(Accessor.ComponentType.BYTE, normalized = true),
                Accessor.ComponentTypeItem(Accessor.ComponentType.UNSIGNED_BYTE, normalized = true),
                Accessor.ComponentTypeItem(Accessor.ComponentType.SHORT, normalized = true),
                Accessor.ComponentTypeItem(Accessor.ComponentType.UNSIGNED_SHORT, normalized = true),
            ),
        ),
        SCALE(
            accessorType = Accessor.AccessorType.VEC3,
            componentType = listOf(Accessor.ComponentTypeItem(Accessor.ComponentType.FLOAT)),
        ),
        WEIGHTS(
            accessorType = Accessor.AccessorType.VEC3,
            componentType = listOf(
                Accessor.ComponentTypeItem(Accessor.ComponentType.FLOAT),
                Accessor.ComponentTypeItem(Accessor.ComponentType.BYTE, normalized = true),
                Accessor.ComponentTypeItem(Accessor.ComponentType.UNSIGNED_BYTE, normalized = true),
                Accessor.ComponentTypeItem(Accessor.ComponentType.SHORT, normalized = true),
                Accessor.ComponentTypeItem(Accessor.ComponentType.UNSIGNED_SHORT, normalized = true),
            ),
        ),
    }
}