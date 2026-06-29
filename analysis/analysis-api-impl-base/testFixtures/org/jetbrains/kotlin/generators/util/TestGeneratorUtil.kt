package org.jetbrains.kotlin.generators.util

import com.intellij.openapi.util.io.FileUtil
import java.io.File

/**
 * 测试源码生成器共享的命名、正则和路径工具。
 */
object TestGeneratorUtil {
    /**
     * 匹配 `.kt` 或 `.kts` 测试数据文件并捕获主文件名的正则。
     */
    const val KT_OR_KTS = """^(.+)\.(kt|kts)$"""

    /**
     * 匹配 `.kt` 测试数据文件并捕获主文件名的正则。
     */
    const val KT = """^(.+)\.(kt)$"""

    /**
     * 匹配 `.kts` 测试数据文件并捕获主文件名的正则。
     */
    const val KTS = """^(.+)\.(kts)$"""

    /**
     * 匹配 REPL 脚本测试数据文件的正则。
     */
    const val REPL_KTS = """^(.+)\.repl\.kts$"""

    /**
     * 匹配文件名主体不含点号的 `.kt` 或 `.kts` 文件正则。
     */
    const val KT_OR_KTS_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.(kt|kts)$"""

    /**
     * 匹配文件名主体不含点号的 `.kt` 文件正则。
     */
    const val KT_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.kt$"""

    /**
     * 匹配带 FIR 标记的 `.kt` 或 `.kts` 测试数据文件正则。
     */
    const val KT_OR_KTS_WITH_FIR_PREFIX = """^(.+)\.fir\.kts?$"""

    /**
     * 为文件名正则追加可选的 `.can-freeze-ide` 后缀。
     */
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

    /**
     * 将文件名转换为首字母大写的 Java 标识符。
     */
    @JvmStatic
    fun fileNameToJavaIdentifier(file: File): String {
        return escapeForJavaIdentifier(file.name).replaceFirstChar(Char::uppercaseChar)
    }

    /**
     * 根据当前调用栈推断触发测试生成的主类名。
     */
    fun getMainClassName(): String? =
        Throwable().stackTrace.lastOrNull()?.className
}

/**
 * Java 生成源码中无需显式 import 的默认包列表。
 */
private val defaultPackages = listOf(
    "java.lang",
    "kotlin",
    "kotlin.annotations",
    "kotlin.collections",
)

/**
 * 判断类型是否位于 Java/Kotlin 默认导入包中。
 */
fun Class<*>.isDefaultImportedClass(): Boolean {
    val outerName = canonicalName.removeSuffix(".$simpleName")
    return outerName in defaultPackages
}

/**
 * 将文件路径转换为使用 `/` 的系统无关路径。
 */
internal fun File.getFilePath(): String {
    return FileUtil.toSystemIndependentName(path)
}
