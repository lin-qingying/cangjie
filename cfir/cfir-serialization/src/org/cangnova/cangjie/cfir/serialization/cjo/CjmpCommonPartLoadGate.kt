package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirCjmpOptimizationLevel
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic

/**
 * CJMP common-part cjo 加载门（官方 `ASTLoader::PreloadCommonPartOfPackage` 对位，计划 §8.3）。
 *
 * 门序列与官方一致：
 * 1. cjo 格式版本（官方 `CheckCjoVersion`，**缺版本即拒**）；
 * 2. 包名一致（官方 `module_common_cjo_wrong_package`）；
 * 3. features 子集 `common ⊆ specific`（官方 `feature_is_not_subset_of_child_set`）；
 * 4. 编译选项匹配（官方 `module_common_cjo_no_options` / `_debug_mismatch` / `_opt_mismatch`）。
 *
 * 诊断不在此上报（加载期 `DiagnosticReporter` 尚不可用），而是返回结构化事实，
 * 由反序列化符号 provider 保留并交给共享 checker 外显。**属性位装填保持无版本判断**（D15）：兼容性完全由格式版本门承担。
 */
object CjmpCommonPartLoadGate {
    /** 当前支持的 cjo 格式版本（= [CjoConstants]，官方 `ModuleFormat.CjoVersion` 0.1.0）。 */
    val supportedVersion: CjoModuleVersion = CjoModuleVersion(
        CjoConstants.VERSION_MAJOR.toUByte(),
        CjoConstants.VERSION_MINOR.toUByte(),
        CjoConstants.VERSION_PATCH.toUByte(),
    )

    /**
     * ① cjo 格式版本门。
     *
     * 官方判据：major 相等 ∧ minor ≤ 当前；**缺失版本同样拒绝**（官方 v1.0.0 教训：
     * 写版本不校验 = 无防御，缺版本静默消费更危险）。
     */
    fun checkFormatVersion(packageName: String, version: CjoModuleVersion?): CfirCjmpLoadDiagnostic? = when {
        version == null -> CfirCjmpLoadDiagnostic.CjoVersion(
            requestedPackageName = packageName,
            actualVersion = null,
            supportedMajor = supportedVersion.major.toInt(),
            supportedMinor = supportedVersion.minor.toInt(),
        )

        version.major != supportedVersion.major || version.minor > supportedVersion.minor ->
            CfirCjmpLoadDiagnostic.CjoVersion(
                requestedPackageName = packageName,
                actualVersion = version.toString(),
                supportedMajor = supportedVersion.major.toInt(),
                supportedMinor = supportedVersion.minor.toInt(),
            )

        else -> null
    }

    /** ② 包名一致门（官方 `module_common_cjo_wrong_package`）。 */
    fun checkPackageName(expected: String, actual: String): CfirCjmpLoadDiagnostic? =
        if (expected == actual) {
            null
        } else {
            CfirCjmpLoadDiagnostic.WrongPackage(
                requestedPackageName = expected,
                actualPackageName = actual,
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
        CfirCjmpLoadDiagnostic.FeaturesNotSubset(
            requestedPackageName = packageName,
            commonFileName = fileName,
            extraFeatures = extra,
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
        optLevel: CfirCjmpOptimizationLevel,
    ): List<CfirCjmpLoadDiagnostic> = buildList {
        if (options == null) {
            add(
                CfirCjmpLoadDiagnostic.OptionsMissing(requestedPackageName = packageName),
            )
            return@buildList
        }
        if (options.debug != debug) {
            add(
                CfirCjmpLoadDiagnostic.DebugMismatch(
                    requestedPackageName = packageName,
                    commonDebug = options.debug,
                    currentDebug = debug,
                ),
            )
        }
        val commonOptLevel = CfirCjmpOptimizationLevel.fromCjoValueOrNull(options.optLevel)
        if (commonOptLevel == null || commonOptLevel != optLevel) {
            add(
                CfirCjmpLoadDiagnostic.OptLevelMismatch(
                    requestedPackageName = packageName,
                    commonOptLevel = commonOptLevel?.displayName
                        ?: "unsupported CJO optimization-level value ${options.optLevel}",
                    currentOptLevel = optLevel.displayName,
                ),
            )
        }
    }
}
