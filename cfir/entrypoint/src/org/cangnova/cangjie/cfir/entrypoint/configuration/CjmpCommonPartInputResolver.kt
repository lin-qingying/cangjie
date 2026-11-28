package org.cangnova.cangjie.cfir.entrypoint.configuration

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.config.CjmpCommonPartInput
import org.cangnova.cangjie.config.CjmpCommonPartInputs

/** common-part 命令行输入归一化失败的原因。 */
public enum class CjmpCommonPartInputErrorKind {
    COMMON_CHIR_NOT_SUPPORTED,
    COMMON_CJO_CHIR_COUNT_MISMATCH,
    COMMON_PART_INPUT_FILE_INVALID,
}

/** 输入协议的消费边界；前端可只装配 CJO，命令行驱动要求完整的专用 CHIR 配对。 */
public enum class CjmpCommonPartInputConsumer {
    FRONTEND,
    COMPILATION_DRIVER,
}

/** 前端配置层对 common-part 输入参数的唯一解析结果。 */
public sealed interface CjmpCommonPartInputResolution {
    public data class Valid(public val inputs: CjmpCommonPartInputs) : CjmpCommonPartInputResolution

    public data class Invalid(
        public val kind: CjmpCommonPartInputErrorKind,
        public val commonCjoCount: Int,
        public val commonChirCount: Int,
        public val inputPath: String? = null,
    ) : CjmpCommonPartInputResolution {
        public val message: String
            get() = when (kind) {
                CjmpCommonPartInputErrorKind.COMMON_CHIR_NOT_SUPPORTED ->
                    "common .chir input is not supported for this language version"

                CjmpCommonPartInputErrorKind.COMMON_CJO_CHIR_COUNT_MISMATCH ->
                    "common .chir files count should be equal to common .cjo files count"

                CjmpCommonPartInputErrorKind.COMMON_PART_INPUT_FILE_INVALID ->
                    "common-part input '${inputPath}' must be an existing readable file"
            }
    }
}

/**
 * 将前端参数转换为 session、模块图和 CJO preload 共用的 parent 输入快照。
 *
 * CJO parent 数量与专用 CHIR 输入分别按各自语言特性解析；每个有效 parent 以同一位置的路径配对。
 */
public object CjmpCommonPartInputResolver {
    public fun resolve(
        commonPartCjoPaths: List<String>,
        commonPartChirPaths: List<String>,
        languageVersionSettings: LanguageVersionSettings,
        consumer: CjmpCommonPartInputConsumer = CjmpCommonPartInputConsumer.FRONTEND,
    ): CjmpCommonPartInputResolution {
        val supportsMultipleParents =
            languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.MultipleCommonParents)
        val supportsDedicatedChirInputs =
            languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.DedicatedCommonPartChirInputs)
        if (!supportsDedicatedChirInputs && commonPartChirPaths.isNotEmpty()) {
            return CjmpCommonPartInputResolution.Invalid(
                kind = CjmpCommonPartInputErrorKind.COMMON_CHIR_NOT_SUPPORTED,
                commonCjoCount = commonPartCjoPaths.size,
                commonChirCount = commonPartChirPaths.size,
            )
        }

        val selectedCjoPaths = if (supportsMultipleParents) {
            commonPartCjoPaths
        } else {
            commonPartCjoPaths.takeLast(1)
        }
        val chirPairingIsRequired = commonPartChirPaths.isNotEmpty() ||
                consumer == CjmpCommonPartInputConsumer.COMPILATION_DRIVER
        val hasInvalidChirPairing = supportsDedicatedChirInputs &&
                chirPairingIsRequired && selectedCjoPaths.size != commonPartChirPaths.size
        if (hasInvalidChirPairing) {
            return CjmpCommonPartInputResolution.Invalid(
                kind = CjmpCommonPartInputErrorKind.COMMON_CJO_CHIR_COUNT_MISMATCH,
                commonCjoCount = selectedCjoPaths.size,
                commonChirCount = commonPartChirPaths.size,
            )
        }

        val parents = if (supportsDedicatedChirInputs && commonPartChirPaths.isNotEmpty()) {
            selectedCjoPaths.zip(commonPartChirPaths) { cjoPath, chirPath ->
                CjmpCommonPartInput(cjoPath = cjoPath, chirPath = chirPath)
            }
        } else {
            selectedCjoPaths.map { cjoPath -> CjmpCommonPartInput(cjoPath = cjoPath, chirPath = null) }
        }
        return CjmpCommonPartInputResolution.Valid(CjmpCommonPartInputs.from(parents))
    }
}
