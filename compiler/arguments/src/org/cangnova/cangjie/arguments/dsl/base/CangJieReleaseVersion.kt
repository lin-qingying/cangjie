package org.cangnova.cangjie.arguments.dsl.base

/**
 * 编译器参数 schema 可描述的仓颉发布版本。
 */
enum class CangJieReleaseVersion {
    /**
     * 仓颉 1.0.5 版本。
     */
    V_1_0_5,


}

/**
 * 编译器参数在发布版本中的生命周期元数据。
 */
data class CangJieReleaseVersionLifecycle(
    /**
     * 参数首次引入的版本。
     */
    val introducedVersion: CangJieReleaseVersion,
    /**
     * 参数语义稳定的版本；尚未稳定时为空。
     */
    val stabilizedVersion: CangJieReleaseVersion? = null,
    /**
     * 参数被标记为废弃的版本；尚未废弃时为空。
     */
    val deprecatedVersion: CangJieReleaseVersion? = null,
    /**
     * 参数从 schema 或编译器中移除的版本；尚未移除时为空。
     */
    val removedVersion: CangJieReleaseVersion? = null,
)

/**
 * 暴露仓颉发布版本生命周期元数据的参数模型接口。
 */
interface WithCangJieReleaseVersionsMetadata {
    /**
     * 当前模型对象对应的版本生命周期信息。
     */
    val releaseVersionsMetadata: CangJieReleaseVersionLifecycle
}
