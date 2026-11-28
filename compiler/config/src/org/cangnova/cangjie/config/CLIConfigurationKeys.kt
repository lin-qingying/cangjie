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

    /**
     * 旧版 CJMP driver 从位置参数接收的 CHIR 包文件；与源码根和 alpha.20 命名 CHIR 输入分开保存。
     */
    @JvmField
    val POSITIONAL_CHIR_INPUTS = CompilerConfigurationKey.create<List<java.nio.file.Path>>("POSITIONAL_CHIR_INPUTS")

    /**
     * CLI 将已验证的目录参数保留为 package-path 输入，不与普通源码根混合。
     */
    @JvmField
    val PACKAGE_PATHS = CompilerConfigurationKey.create<List<java.nio.file.Path>>("PACKAGE_PATHS")

    /** 已验证的位置编译工件；保留原始参数与解析路径，供对应后端阶段按输入种类消费。 */
    @JvmField
    val POSITIONAL_ARTIFACT_INPUTS =
        CompilerConfigurationKey.create<List<PositionalArtifactInput>>("POSITIONAL_ARTIFACT_INPUTS")

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
