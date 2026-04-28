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
