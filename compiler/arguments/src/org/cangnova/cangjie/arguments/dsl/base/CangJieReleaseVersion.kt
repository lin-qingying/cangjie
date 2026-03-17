package org.cangnova.cangjie.arguments.dsl.base

enum class CangJieReleaseVersion {
    V_1_0_5,


}

data class CangJieReleaseVersionLifecycle(
    val introducedVersion: CangJieReleaseVersion,
    val stabilizedVersion: CangJieReleaseVersion? = null,
    val deprecatedVersion: CangJieReleaseVersion? = null,
    val removedVersion: CangJieReleaseVersion? = null,
)

interface WithCangJieReleaseVersionsMetadata {
    val releaseVersionsMetadata: CangJieReleaseVersionLifecycle
}
