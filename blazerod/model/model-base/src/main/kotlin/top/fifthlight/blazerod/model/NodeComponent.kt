package top.fifthlight.blazerod.model

sealed class NodeComponent {
    enum class Type(
        val requireMesh: Boolean,
        val singleInstanceOnly: Boolean,
    ) {
        MESH(
            requireMesh = false,
            singleInstanceOnly = true,
        ),
        SKIN(
            requireMesh = true,
            singleInstanceOnly = true,
        ),
        CAMERA(
            requireMesh = false,
            singleInstanceOnly = false,
        ),
        IK_TARGET(
            requireMesh = false,
            singleInstanceOnly = false,
        ),
        INFLUENCE_TARGET(
            requireMesh = false,
            singleInstanceOnly = false,
        );

        companion object {
            val requireMesh = entries.filter { it.requireMesh }
            val onlyHaveOne = entries.filter { it.singleInstanceOnly }
        }
    }

    abstract val type: Type

    data class MeshComponent(val mesh: Mesh): NodeComponent(){
        override val type: Type
            get() = Type.MESH
    }

    data class SkinComponent(val skin: Skin): NodeComponent() {
        override val type: Type
            get() = Type.SKIN
    }

    data class CameraComponent(val camera: Camera): NodeComponent() {
        override val type: Type
            get() = Type.CAMERA
    }

    data class IkTargetComponent(
        val ikTarget: IkTarget,
        val transformId: TransformId,
    ): NodeComponent() {
        override val type: Type
            get() = Type.IK_TARGET
    }

    data class InfluenceTargetComponent(
        val influence: Influence,
        val transformId: TransformId,
    ): NodeComponent() {
        override val type: Type
            get() = Type.INFLUENCE_TARGET
    }
}