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
    private val options: JvmCodegenOptions,
) {
    fun moduleInternalName(chirPackage: ChirPackage, module: ChirModule): String {
        val packagePath = packageInternalPath(chirPackage.name)
        val simpleName = sanitizeClassSimpleName(module.name.substringAfterLast('.')) + options.moduleFacadeSuffix
        return if (packagePath.isEmpty()) simpleName else "$packagePath/$simpleName"
    }

    fun typeInternalName(chirPackage: ChirPackage, declaration: ChirTypeDeclaration): String =
        typeInternalName(chirPackage.name, declaration.name)

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

    fun functionJvmName(function: ChirFunctionDeclaration): String = sanitizeMethodName(function.name)

    fun functionJvmName(rawName: String): String = sanitizeMethodName(rawName)

    fun fieldJvmName(name: String): String = sanitizeJavaIdentifier(name, "field")

    private fun packageInternalPath(packageName: String): String {
        return packageName
            .split('.')
            .filter { it.isNotBlank() }
            .joinToString("/") { sanitizeJavaIdentifier(it, "pkg") }
    }

    private fun sanitizeClassSimpleName(raw: String): String {
        val sanitized = sanitizeJavaIdentifier(raw, "Module")
        return sanitized.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }
    }

    private fun sanitizeMethodName(raw: String): String = sanitizeJavaIdentifier(raw, "function")

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
