package org.cangnova.cangjie.config

/**
 * CLI 配置中可作为输入内容根的统一标记接口。
 */
interface ContentRoot

/**
 * Mirrors KotlinSourceRoot while keeping Cangjie naming.
 */
data class CangJieSourceRoot(
    /**
     * 仓颉源码根路径。
     */
    val path: String,
    /**
     * 是否作为 common source root 参与多平台/共享源码处理。
     */
    val isCommon: Boolean,
    /**
     * HMPP 模块名称；非 HMPP 源码根为空。
     */
    val hmppModuleName: String?,
) : ContentRoot

/**
 * Java 源码根配置。
 */
data class JavaSourceRoot(
    /**
     * Java 源码根路径。
     */
    val path: String,
    /**
     * Java 源码根对应的包名前缀。
     */
    val packagePrefix: String?,
) : ContentRoot

/**
 * 编译 classpath 根配置。
 */
data class ClasspathRoot(
    /**
     * classpath 条目路径。
     */
    val path: String,
) : ContentRoot

/**
 * 向编译配置追加一个仓颉源码根。
 */
@JvmOverloads
fun CompilerConfiguration.addCangJieSourceRoot(
    path: String,
    isCommon: Boolean = false,
    hmppModuleName: String? = null,
) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, CangJieSourceRoot(path, isCommon, hmppModuleName))
}

/**
 * 向编译配置批量追加仓颉源码根。
 */
fun CompilerConfiguration.addCangJieSourceRoots(sources: List<String>) {
    sources.forEach { addCangJieSourceRoot(it) }
}

/**
 * 向编译配置追加一个 Java 源码根。
 */
@JvmOverloads
fun CompilerConfiguration.addJavaSourceRoot(
    path: String,
    packagePrefix: String? = null,
) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, JavaSourceRoot(path, packagePrefix))
}

/**
 * 向编译配置追加一个 classpath 根。
 */
fun CompilerConfiguration.addClasspathRoot(path: String) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, ClasspathRoot(path))
}

/**
 * 当前编译配置中所有仓颉源码根。
 */
val CompilerConfiguration.cangjieSourceRoots: List<CangJieSourceRoot>
    get() = get(CLIConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<CangJieSourceRoot>().orEmpty()

/**
 * 当前编译配置中所有 classpath 根。
 */
val CompilerConfiguration.classpathRoots: List<ClasspathRoot>
    get() = get(CLIConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<ClasspathRoot>().orEmpty()
