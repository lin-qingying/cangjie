package org.jetbrains.kotlin.generators.util

import com.intellij.openapi.util.io.FileUtil
import java.io.File

object TestGeneratorUtil {
    const val KT_OR_KTS = """^(.+)\.(kt|kts)$"""
    const val KT = """^(.+)\.(kt)$"""
    const val KTS = """^(.+)\.(kts)$"""
    const val REPL_KTS = """^(.+)\.repl\.kts$"""
    const val KT_OR_KTS_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.(kt|kts)$"""
    const val KT_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.kt$"""
    const val KT_OR_KTS_WITH_FIR_PREFIX = """^(.+)\.fir\.kts?$"""

    @JvmStatic
    val String.canFreezeIDE: String
        get() = """${substringBeforeLast('$')}(\.can-freeze-ide)?$"""

    /**
     * 将测试数据文件名转成 Java 源码可直接使用的标识符。
     *
     * `.cj` 测试文件可能来自官方数据集，文件名允许以数字开头；生成 Java 类名或方法名时必须补齐合法首字符。
     */
    @JvmStatic
    fun escapeForJavaIdentifier(fileName: String): String {
        val result = StringBuilder()
        for (c in fileName) {
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c)
            } else {
                result.append("_")
            }
        }
        if (result.isEmpty() || !Character.isJavaIdentifierStart(result[0])) {
            result.insert(0, "_")
        }
        return result.toString()
    }

    @JvmStatic
    fun fileNameToJavaIdentifier(file: File): String {
        return escapeForJavaIdentifier(file.name).replaceFirstChar(Char::uppercaseChar)
    }

    fun getMainClassName(): String? =
        Throwable().stackTrace.lastOrNull()?.className
}

private val defaultPackages = listOf(
    "java.lang",
    "kotlin",
    "kotlin.annotations",
    "kotlin.collections",
)

fun Class<*>.isDefaultImportedClass(): Boolean {
    val outerName = canonicalName.removeSuffix(".$simpleName")
    return outerName in defaultPackages
}

internal fun File.getFilePath(): String {
    return FileUtil.toSystemIndependentName(path)
}
