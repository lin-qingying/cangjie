package org.cangnova.cangjie.cfir.common

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.platform.isCommon

/**
 * 结构外 `.cjo` 的 detached 模块数据。
 *
 * Kotlin 没有对应状态：`.kt` / `.class` 总能按 use-site 或依赖解析找到模块；仓颉 `.cjo` 可以被直接打开
 * 而不属于项目结构中的任何 library/builtins 模块。此时反序列化出的声明仍需要一个 owner 才能物化，
 * 否则 `readFile` 只能返回 null，索引里也不会有该文件的 stub 条目（`S9` 索引洞）。
 *
 * 关键约束：
 * - 构造时显式 [bindSession]，不允许 `LLCfirModuleData.session` 那种 `boundSession ?:
 *   LLCfirSessionCache.getSession(...)` fallback：结构外文件没有 `CaModule`，走到 fallback 只会抛
 *   `ClassCastException`；
 * - 不继承 `LLCfirModuleData`：它要求 `LLCfirSession`（带 `caModule` / `builtinTypes`），结构外文件给不出；
 * - 不持有项目结构状态，SDK 或模块变化后由调用方整体重建（per-file 实例）。
 */
class DetachedCjoModuleData(
    /** 结构外 `.cjo` 的包全限定名，仅用于诊断标识。 */
    val packageFqName: FqName,
    /** 绑定目标：结构外 `.cjo` 专用的最小 session（`cfir-providers` 中的 `DetachedCjoSession`）。 */
    detachedSession: CfirSession,
    /**
     * 高层目标平台。
     *
     * detached 阶段 `.cjo` 头部不携带后端身份，只能按项目默认平台登记；它目前只影响
     * cjvm/cjnative 专属 checker，而 detached session 仅服务反编译 stub 物化。
     */
    platformIdentity: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CfirModuleData() {
    /** 后端身份；不与 [CfirModuleData.targetPlatform] 同名，避免遮蔽覆写成员。 */
    private val platformIdentity: TargetPlatform = platformIdentity

    init {
        bindSession(detachedSession)
    }

    /** 模块名只用于诊断；结构外 `.cjo` 不参与任何依赖解析。 */
    override val name: Name = Name.identifier("<detached:${packageFqName.asString()}>")

    override val dependencies: List<CfirModuleData>
        get() = emptyList()

    override val refinementDependencies: List<CfirModuleData>
        get() = emptyList()

    override val allRefinementDependencies: List<CfirModuleData>
        get() = emptyList()

    override val targetPlatform: TargetPlatform
        get() = platformIdentity

    /** 底层平台细节未知，detached 不参与平台相关语义。 */
    override val platform: CfirPlatform
        get() = CfirPlatform.DEFAULT

    override val isCommon: Boolean
        get() = platformIdentity.isCommon()

    override val session: CfirSession
        get() = boundSession ?: error("detached module data for ${packageFqName.asString()} is not bound to a session")

    override val stableModuleName: String?
        get() = null

    /** 库 session 语义：`.cjo` 内容按库合并规则处理重声明。 */
    override val areRedeclarationsEquivalent: Boolean
        get() = true
}
