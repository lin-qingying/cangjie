package org.cangnova.cangjie.config

/**
 * CLI 相关的配置键集合。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.cli.common.CLIConfigurationKeys`。
 */
object CLIConfigurationKeys {
    /**
     * CLI 输入内容根集合，包括仓颉源码根、Java 源码根和 classpath 根。
     */
    @JvmField
    val CONTENT_ROOTS = CompilerConfigurationKey.create<List<ContentRoot>>("CONTENT_ROOTS")

    @Deprecated(
        message = "Use CLIConfigurationKeys.CONTENT_ROOTS instead.",
        replaceWith = ReplaceWith("CLIConfigurationKeys.CONTENT_ROOTS"),
    )
    @JvmField
    /**
     * 旧版 CLI 源文件路径键，保留用于兼容仍读取该键的调用方。
     */
    val CLI_SOURCE_FILE_PATHS = CompilerConfigurationKey.create<List<String>>("CLI_SOURCE_FILE_PATHS")


}
