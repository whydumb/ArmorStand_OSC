package top.fifthlight.renderer.model

data class Metadata(
    val title: String? = null,
    // For PMX
    val titleUniversal: String? = null,
    val commentLocal: String,
    val commentUniversal: String,

    val version: String? = null,
    val authors: List<String>? = null,
    val copyrightInformation: String? = null,
    val contactInformation: String? = null,
    val references: List<String>? = null,
    val thirdPartyLicenses: String? = null,
    val thumbnail: Texture? = null,
    
    // License information
    val licenseType: String? = null,
    val licenseUrl: String? = null,
    
    // Usage permissions
    val allowedUser: AllowedUser? = null,
    val allowViolentUsage: Boolean? = null,
    val allowSexualUsage: Boolean? = null,
    val commercialUsage: CommercialUsage? = null,
    val allowPoliticalOrReligiousUsage: Boolean? = null,
    val allowAntisocialOrHateUsage: Boolean? = null,
    val creditNotation: CreditNotation? = null,
    val allowRedistribution: Boolean? = null,
    val modificationPermission: ModificationPermission? = null,
) {
    enum class AllowedUser {
        ONLY_AUTHOR,
        EXPLICITLY_LICENSED_PERSON,
        EVERYONE
    }

    enum class CommercialUsage {
        DISALLOW,
        ALLOW,
        PERSONAL_NON_PROFIT,
        PERSONAL_PROFIT,
        CORPORATION,
    }

    enum class CreditNotation {
        REQUIRED,
        UNNECESSARY,
    }

    enum class ModificationPermission {
        PROHIBITED,
        ALLOW_MODIFICATION,
        ALLOW_MODIFICATION_REDISTRIBUTION,
    }
}
