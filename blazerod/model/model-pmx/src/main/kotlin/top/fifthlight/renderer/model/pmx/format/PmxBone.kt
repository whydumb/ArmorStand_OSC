package top.fifthlight.renderer.model.pmx.format

import org.joml.Vector3f

data class PmxBone(
    val nameLocal: String,
    val nameUniversal: String,
    val position: Vector3f,
    val parentBoneIndex: Int?,
    val layer: Int,
    val flags: Flags,
    val tailPosition: TailPosition,
    val inheritParentIndex: Int?,
    val inheritParentInfluence: Float?,
    val axisDirection: Vector3f?,
    val localCoordinate: LocalCoordinate?,
    val externalParentIndex: Int?,
    val ikData: IkData?,
) {
    init {
        if (flags.inheritRotation || flags.inheritTranslation) {
            inheritParentIndex!!
            inheritParentInfluence!!
        }
        if (flags.localCoordinate) {
            localCoordinate!!
        }
        if (flags.externalParentDeform) {
            externalParentIndex!!
        }
        if (flags.ik) {
            ikData!!
        }
    }

    data class Flags(
        val indexedTailPosition: Boolean,
        val rotatable: Boolean,
        val translatable: Boolean,
        val isVisible: Boolean,
        val enabled: Boolean,
        val ik: Boolean,
        val inheritRotation: Boolean,
        val inheritTranslation: Boolean,
        val fixedAxis: Boolean,
        val localCoordinate: Boolean,
        val physicsAfterDeform: Boolean,
        val externalParentDeform: Boolean,
    )

    sealed class TailPosition {
        data class Indexed(
            val boneIndex: Int,
        ): TailPosition()
        data class Scalar(
            val position: Vector3f,
        ): TailPosition()
    }

    data class IkLink(
        val index: Int,
        val limits: Limits?,
    ) {
        data class Limits(
            val limitMin: Vector3f,
            val limitMax: Vector3f,
        )
    }

    data class IkData(
        val targetIndex: Int,
        val loopCount: Int,
        val limitRadian: Float,
        val links: List<IkLink>,
    )

    data class LocalCoordinate(
        val xVector: Vector3f?,
        val yVector: Vector3f?,
    )
}
