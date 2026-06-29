package org.cangnova.cangjie.jvm.codegen.naming

import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.jvm.codegen.api.JvmCodegenOptions

/**
 * JVM 命名策略。所有 JVM internal name / method name 都从这里产生，后续支持注解 ABI 时只扩展这里。
 */
class JvmNamePolicy(
    /**
     * 当前 JVM codegen 选项，主要用于 module facade 后缀。
     */
    private val options: JvmCodegenOptions,
) {
    /**
     * 生成 CHIR module facade class 的 JVM internal name。
     */
    fun moduleInternalName(chirPackage: ChirPackage, module: ChirModule): String {
        val packagePath = packageInternalPath(chirPackage.name)
        val simpleName = sanitizeClassSimpleName(module.name.substringAfterLast('.')) + options.moduleFacadeSuffix
        return if (packagePath.isEmpty()) simpleName else "$packagePath/$simpleName"
    }

    /**
     * 生成 CHIR 类型声明的 JVM internal name。
     */
    fun typeInternalName(chirPackage: ChirPackage, declaration: ChirTypeDeclaration): String =
        typeInternalName(chirPackage.name, declaration.name)

    /**
     * 根据默认包名和类型名生成 JVM internal name。
     */
    fun typeInternalName(defaultPackageName: String, typeName: String): String {
        val segments = typeName
            .replace("::", ".")
            .split('.')
            .filter { it.isNotBlank() }
        if (segments.size > 1) {
            return segments.joinToString("/") { sanitizeJavaIdentifier(it, "Type") }
        }

        val packagePath = packageInternalPath(defaultPackageName)
        val simpleName = sanitizeClassSimpleName(segments.singleOrNull() ?: typeName)
        return if (packagePath.isEmpty()) simpleName else "$packagePath/$simpleName"
    }

    /**
     * 生成 CHIR 函数声明对应的 JVM 方法名。
     */
    fun functionJvmName(function: ChirFunctionDeclaration): String = sanitizeMethodName(function.name)

    /**
     * 生成原始函数名对应的 JVM 方法名。
     */
    fun functionJvmName(rawName: String): String = sanitizeMethodName(rawName)

    /**
     * 生成字段名对应的 JVM 字段名。
     */
    fun fieldJvmName(name: String): String = sanitizeJavaIdentifier(name, "field")

    /**
     * 将包名转换为 JVM internal package path。
     */
    private fun packageInternalPath(packageName: String): String {
        return packageName
            .split('.')
            .filter { it.isNotBlank() }
            .joinToString("/") { sanitizeJavaIdentifier(it, "pkg") }
    }

    /**
     * 将原始 class 简名转换为 Java class identifier。
     */
    private fun sanitizeClassSimpleName(raw: String): String {
        val sanitized = sanitizeJavaIdentifier(raw, "Module")
        return sanitized.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }
    }

    /**
     * 将原始方法名转换为 Java method identifier。
     */
    private fun sanitizeMethodName(raw: String): String = sanitizeJavaIdentifier(raw, "function")

    /**
     * 将任意字符串规整为合法 Java identifier，空结果使用 fallback。
     */
    private fun sanitizeJavaIdentifier(raw: String, fallback: String): String {
        val trimmed = raw.trim()
        val builder = StringBuilder()
        trimmed.forEachIndexed { index, char ->
            val valid = if (index == 0) {
                Character.isJavaIdentifierStart(char)
            } else {
                Character.isJavaIdentifierPart(char)
            }
            builder.append(if (valid) char else '_')
        }
        val result = builder.toString().trim('_')
        return when {
            result.isBlank() -> fallback
            Character.isJavaIdentifierStart(result.first()) -> result
            else -> "_$result"
        }
    }
}
