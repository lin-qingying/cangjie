package org.cangnova.cangjie.config

interface ContentRoot

/**
 * Mirrors KotlinSourceRoot while keeping Cangjie naming.
 */
data class CangJieSourceRoot(
    val path: String,
    val isCommon: Boolean,
    val hmppModuleName: String?,
) : ContentRoot

data class JavaSourceRoot(
    val path: String,
    val packagePrefix: String?,
) : ContentRoot

data class ClasspathRoot(
    val path: String,
) : ContentRoot

@JvmOverloads
fun CompilerConfiguration.addCangJieSourceRoot(
    path: String,
    isCommon: Boolean = false,
    hmppModuleName: String? = null,
) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, CangJieSourceRoot(path, isCommon, hmppModuleName))
}

fun CompilerConfiguration.addCangJieSourceRoots(sources: List<String>) {
    sources.forEach { addCangJieSourceRoot(it) }
}

@JvmOverloads
fun CompilerConfiguration.addJavaSourceRoot(
    path: String,
    packagePrefix: String? = null,
) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, JavaSourceRoot(path, packagePrefix))
}

fun CompilerConfiguration.addClasspathRoot(path: String) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, ClasspathRoot(path))
}

val CompilerConfiguration.cangjieSourceRoots: List<CangJieSourceRoot>
    get() = get(CLIConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<CangJieSourceRoot>().orEmpty()

val CompilerConfiguration.classpathRoots: List<ClasspathRoot>
    get() = get(CLIConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<ClasspathRoot>().orEmpty()
