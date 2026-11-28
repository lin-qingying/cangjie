package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticPackage
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import java.nio.file.Path

/** 已按当前 legacy driver 协议导入的位置 CHIR 输入。 */
data class CjmpLegacyChirInput(
    val sourcePath: Path,
    val semanticPackage: OfficialChirV113SemanticPackage,
)

private val LEGACY_CJMP_CHIR_INPUTS =
    CompilerConfigurationKey.create<List<CjmpLegacyChirInput>>("LEGACY_CJMP_CHIR_INPUTS")

/**
 * 旧版 positional CHIR 的生产语义输入，供前端阶段和后续 CHIR 链接阶段共享。
 */
var CompilerConfiguration.cjmpLegacyChirInputs: List<CjmpLegacyChirInput>
    get() = get(LEGACY_CJMP_CHIR_INPUTS).orEmpty()
    set(value) {
        put(LEGACY_CJMP_CHIR_INPUTS, value.toList())
    }
