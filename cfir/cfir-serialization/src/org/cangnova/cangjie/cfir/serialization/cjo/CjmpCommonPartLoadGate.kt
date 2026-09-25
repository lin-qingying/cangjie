package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind

/**
 * CJMP common-part cjo 加载门（官方 `ASTLoader::PreloadCommonPartOfPackage` 对位，计划 §8.3）。
 *
 * 门序列与官方一致：
 * 1. cjo 格式版本（官方 `CheckCjoVersion`，**缺版本即拒**）；
 * 2. 包名一致（官方 `module_common_cjo_wrong_package`）；
 * 3. features 子集 `common ⊆ specific`（官方 `feature_is_not_subset_of_child_set`）；
 * 4. 编译选项匹配（官方 `module_common_cjo_no_options` / `_debug_mismatch` / `_opt_mismatch`）。
 *
 * 诊断不在此上报（加载期 `DiagnosticReporter` 尚不可用），按 G17 约定返回结构化诊断，
 * 由加载器收进 session 的 [org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticsComponent]，
 * 再由装配层外显。**属性位装填保持无版本判断**（D15）：兼容性完全由格式版本门承担。
 */
object CjmpCommonPartLoadGate {
    /** 当前支持的 cjo 格式版本（= [CjoConstants]，官方 `ModuleFormat.CjoVersion` 0.1.0）。 */
    val supportedVersion: CjoModuleVersion = CjoModuleVersion(
        CjoConstants.VERSION_MAJOR.toUByte(),
        CjoConstants.VERSION_MINOR.toUByte(),
        CjoConstants.VERSION_PATCH.toUByte(),
    )

    /** 官方 `OptimizationLevel` 的字符串→序号映射（O0..Oz，与 `OptimizationLevelToString` 逆序对位）。 */
    private val OPT_LEVEL_ORDINALS: Map<String, UByte> = mapOf(
        "O0" to 0u,
        "O1" to 1u,
        "O2" to 2u,
        "O3" to 3u,
        "Os" to 4u,
        "Oz" to 5u,
    )

    /** 优化级别字符串 → 官方序号；未知级别按 `O0` 处理（调用方负责合法性）。 */
    fun optLevelOrdinal(level: String): UByte = OPT_LEVEL_ORDINALS[level] ?: 0u

    /** 官方序号 → 优化级别字符串（`OptimizationLevelToString` 对位）。 */
    private fun optLevelName(ordinal: UByte): String =
        OPT_LEVEL_ORDINALS.entries.firstOrNull { it.value == ordinal }?.key ?: "(unsupported)"

    /**
     * ① cjo 格式版本门。
     *
     * 官方判据：major 相等 ∧ minor ≤ 当前；**缺失版本同样拒绝**（官方 v1.0.0 教训：
     * 写版本不校验 = 无防御，缺版本静默消费更危险）。
     */
    fun checkFormatVersion(packageName: String, version: CjoModuleVersion?): CfirCjmpLoadDiagnostic? = when {
        version == null -> CfirCjmpLoadDiagnostic(
            kind = CfirCjmpLoadDiagnosticKind.CJO_VERSION,
            packageName = packageName,
            message = "cjo of '$packageName' has no format version; refuse to consume it",
        )

        version.major != supportedVersion.major || version.minor > supportedVersion.minor ->
            CfirCjmpLoadDiagnostic(
                kind = CfirCjmpLoadDiagnosticKind.CJO_VERSION,
                packageName = packageName,
                message = "cjo format version $version of '$packageName' is not supported " +
                    "(supported major ${supportedVersion.major}, minor <= ${supportedVersion.minor})",
            )

        else -> null
    }

    /** ② 包名一致门（官方 `module_common_cjo_wrong_package`）。 */
    fun checkPackageName(expected: String, actual: String): CfirCjmpLoadDiagnostic? =
        if (expected == actual) {
            null
        } else {
            CfirCjmpLoadDiagnostic(
                kind = CfirCjmpLoadDiagnosticKind.WRONG_PACKAGE,
                packageName = actual,
                message = "common part is for another package '$actual', " +
                    "expected the same as for current package '$expected'.",
            )
        }

    /**
     * ③ features 子集门：common part **每个文件**的 features 必须是 specific 侧 features 的子集
     *（官方 `ValidateCommonSpecificFeatureSetsRelations` 逐文件判定，诊断
     * `feature_is_not_subset_of_child_set`，附注列出多出的 feature）。
     *
     * @param commonFileFeatures common part 文件名 → 该文件 features（每个 feature 为分段标识列表）。
     */
    fun checkFeaturesSubset(
        packageName: String,
        commonFileFeatures: Map<String, List<List<String>>>,
        specificFeatures: Set<String>,
    ): List<CfirCjmpLoadDiagnostic> = commonFileFeatures.mapNotNull { (fileName, features) ->
        val extra = features
            .map { identifiers -> identifiers.joinToString(".") }
            .filter { feature -> feature.isNotEmpty() && feature !in specificFeatures }
        if (extra.isEmpty()) return@mapNotNull null
        CfirCjmpLoadDiagnostic(
            kind = CfirCjmpLoadDiagnosticKind.FEATURE_NOT_SUBSET,
            packageName = packageName,
            message = "parent feature set must be subset of child feature set, package: '$packageName', " +
                "file with conflicted features: '$fileName'; extra feature from common part file: " +
                extra.joinToString(" ") { "'$it'" },
        )
    }

    /**
     * ④ 编译选项匹配门（官方 `module_common_cjo_no_options` / `_debug_mismatch` / `_opt_mismatch`）。
     *
     * 选项缺失只告警不拒绝（官方 `ValidateOptions` 在 `options == nullptr` 时报 WARNING 后继续）；
     * debug/优化级别不一致必须拒绝——否则后续 desugar/CHIR 差异会崩溃。
     */
    fun checkOptions(
        packageName: String,
        options: CjoModuleOptionInfo?,
        debug: Boolean,
        optLevel: UByte,
    ): List<CfirCjmpLoadDiagnostic> = buildList {
        if (options == null) {
            add(
                CfirCjmpLoadDiagnostic(
                    kind = CfirCjmpLoadDiagnosticKind.OPTIONS_MISMATCH,
                    packageName = packageName,
                    message = "common part cjo is missing serialized options, possibly compiled by an old cjc",
                    isError = false,
                ),
            )
            return@buildList
        }
        if (options.debug != debug) {
            add(
                CfirCjmpLoadDiagnostic(
                    kind = CfirCjmpLoadDiagnosticKind.OPTIONS_MISMATCH,
                    packageName = packageName,
                    message = "common part is compiled with different debug mode: " +
                        "${if (options.debug) "enabled" else "disabled"} in common and " +
                        "${if (debug) "enabled" else "disabled"} in current",
                ),
            )
        }
        if (options.optLevel != optLevel) {
            add(
                CfirCjmpLoadDiagnostic(
                    kind = CfirCjmpLoadDiagnosticKind.OPTIONS_MISMATCH,
                    packageName = packageName,
                    message = "common part is compiled with different optimization level: " +
                        "${optLevelName(options.optLevel)} in common and ${optLevelName(optLevel)} " +
                        "in the current compilation",
                ),
            )
        }
    }
}
