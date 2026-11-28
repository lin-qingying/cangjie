package org.cangnova.cangjie.config

import java.nio.file.Path

/**
 * 在 driver 配置中保存旧版 CLI 的位置 CHIR 输入，不将其解释为源码根或命名 common-part CHIR。
 */
var CompilerConfiguration.positionalChirInputs: List<Path>
    get() = get(CLIConfigurationKeys.POSITIONAL_CHIR_INPUTS).orEmpty()
    set(value) {
        put(CLIConfigurationKeys.POSITIONAL_CHIR_INPUTS, value.toList())
    }
