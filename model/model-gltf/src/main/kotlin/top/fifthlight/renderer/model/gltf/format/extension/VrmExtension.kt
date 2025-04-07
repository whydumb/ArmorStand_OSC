package top.fifthlight.renderer.model.gltf.format.extension

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.joml.Vector3f

@Serializable
internal data class VrmV0Extension(
    val exporterVersion: String? = null,
    val meta: Meta? = null,
    val firstPerson: FirstPerson? = null,
    val humanoid: Humanoid? = null,
) {
    @Serializable
    data class Meta(
        val title: String? = null,
        val author: String? = null,
        val allowedUserName: AllowedUserName? = null,
        val violentUssageName: ViolentUsage? = null,
        val sexualUssageName: SexualUsage? = null,
        val commercialUssageName: CommercialUsage? = null,
        val licenseName: String? = null,
        val version: String? = null,
        val contactInformation: String? = null,
        val reference: String? = null,
        val texture: Int? = null,
        val otherPermissionUrl: String? = null,
        val otherLicenseUrl: String? = null
    ) {
        @Serializable
        enum class AllowedUserName {
            @SerialName("OnlyAuthor")
            ONLY_AUTHOR,
            @SerialName("ExplicitlyLicensedPerson")
            EXPLICITLY_LICENSED_PERSON,
            @SerialName("Everyone")
            EVERYONE
        }

        @Serializable
        enum class ViolentUsage {
            @SerialName("Disallow")
            DISALLOW,
            @SerialName("Allow")
            ALLOW,
        }

        @Serializable
        enum class SexualUsage {
            @SerialName("Disallow")
            DISALLOW,
            @SerialName("Allow")
            ALLOW,
        }

        @Serializable
        enum class CommercialUsage {
            @SerialName("Disallow")
            DISALLOW,
            @SerialName("Allow")
            ALLOW,
        }
    }

    @Serializable
    data class FirstPersonBoneOffset(
        val x: Float,
        val y: Float,
        val z: Float,
    ) {
        val vector
            get() = Vector3f(x, y, z)
    }

    @Serializable
    data class FirstPerson(
        val firstPersonBone: Int? = null,
        val firstPersonBoneOffset: FirstPersonBoneOffset? = null,
        val meshAnnotations: List<MeshAnnotation>? = null,
        val lookAtTypeName: String? = null,
        val lookAtHorizontalInner: DegreeMap? = null,
        val lookAtHorizontalOuter: DegreeMap? = null,
        val lookAtVerticalDown: DegreeMap? = null,
        val lookAtVerticalUp: DegreeMap? = null
    ) {
        @Serializable
        data class MeshAnnotation(
            val mesh: Int,
            val firstPersonFlag: String
        )

        @Serializable
        data class DegreeMap(
            val curve: List<Float>? = null,
            val xRange: Float? = null,
            val yRange: Float? = null
        )
    }

    @Serializable
    data class Humanoid(
        val humanBones: List<HumanBone>? = null,
    ) {
        @Serializable
        data class HumanBone(
            val bone: String,
            val node: Int,
        )
    }
}

@Serializable
data class VrmV1Extension(
    val meta: Meta? = null,
    val humanoid: Humanoid? = null,
) {
    @Serializable
    data class Meta(
        val name: String? = null,
        val authors: List<String>? = null,
        val licenseUrl: String? = null,
        val version: String? = null,
        val copyrightInformation: String? = null,
        val contactInformation: String? = null,
        val references: List<String>? = null,
        val thirdPartyLicenses: String? = null,
        val thumbnailImage: Int? = null,
        val avatarPermission: AvatarPermission? = null,
        val allowExcessivelyViolentUsage: Boolean? = null,
        val allowExcessivelySexualUsage: Boolean? = null,
        val commercialUsage: CommercialUsage? = null,
        val allowPoliticalOrReligiousUsage: Boolean? = null,
        val allowAntisocialOrHateUsage: Boolean? = null,
        val creditNotation: CreditNotation? = null,
        val allowRedistribution: Boolean? = null,
        val modification: Modification? = null,
        val otherLicenseUrl: String? = null
    ) {
        @Serializable
        enum class AvatarPermission {
            @SerialName("onlyAuthor")
            ONLY_AUTHOR,
            @SerialName("onlySeparatelyLicensedPerson")
            ONLY_SEPARATELY_LICENSED_PERSON,
            @SerialName("everyone")
            EVERYONE,
        }

        @Serializable
        enum class CommercialUsage {
            @SerialName("personalNonProfit")
            PERSONAL_NON_PROFIT,
            @SerialName("personalProfit")
            PERSONAL_PROFIT,
            @SerialName("corporation")
            CORPORATION,
        }

        @Serializable
        enum class CreditNotation {
            @SerialName("required")
            REQUIRED,
            @SerialName("unnecessary")
            UNNECESSARY,
        }

        @Serializable
        enum class Modification {
            @SerialName("prohibited")
            PROHIBITED,
            @SerialName("allowModification")
            ALLOW_MODIFICATION,
            @SerialName("allowModificationRedistribution")
            ALLOW_MODIFICATION_REDISTRIBUTION,
        }
    }

    @Serializable
    data class Humanoid(
        val humanBones: Map<String, HumanBone>? = null,
    ) {
        @Serializable
        data class HumanBone(
            val node: Int,
        )
    }
}
