package org.cangnova.cangjie.config

/**
 * CLI 相关的配置键集合。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.cli.common.CLIConfigurationKeys`。
 */
object CLIConfigurationKeys {
    @JvmField
    val CONTENT_ROOTS = CompilerConfigurationKey.create<List<ContentRoot>>("CONTENT_ROOTS")

    @Deprecated(
        message = "Use CLIConfigurationKeys.CONTENT_ROOTS instead.",
        replaceWith = ReplaceWith("CLIConfigurationKeys.CONTENT_ROOTS"),
    )
    @JvmField
    val CLI_SOURCE_FILE_PATHS = CompilerConfigurationKey.create<List<String>>("CLI_SOURCE_FILE_PATHS")


}
