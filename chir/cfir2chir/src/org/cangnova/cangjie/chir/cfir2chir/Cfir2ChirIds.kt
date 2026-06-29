package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * CFIR 到 CHIR 转换阶段生成稳定语义 ID 和 JVM 安全名称的工具。
 */
internal object Cfir2ChirIds {
    /**
     * 根据包名生成 CHIR package ID。
     */
    fun packageId(packageName: String): ChirSemanticId = ChirSemanticId("cfir:package:${packageName.sanitizeId()}")

    /**
     * 根据 CFIR 文件生成 CHIR module ID。
     */
    fun moduleId(file: CfirFile): ChirSemanticId = ChirSemanticId("cfir:file:${file.name.sanitizeId()}")

    /**
     * 根据 CFIR symbol debug 名称生成声明 ID。
     */
    fun declarationId(symbol: CfirBasedSymbol<*>): ChirSemanticId =
        ChirSemanticId("cfir:declaration:${symbol.debugName.sanitizeId()}")

    /**
     * 根据可调用声明 symbol 生成可调用 ID。
     */
    fun callableId(declaration: CfirCallableDeclaration): ChirSemanticId =
        ChirSemanticId("cfir:callable:${declaration.symbol.debugName.sanitizeId()}")

    /**
     * 根据 CFIR 值参数 symbol 生成参数 ID。
     */
    fun parameterId(parameter: CfirValueParameter): ChirSemanticId =
        ChirSemanticId("cfir:parameter:${parameter.symbol.debugName.sanitizeId()}")

    /**
     * 在所属 ID 下生成按顺序编号的合成 ID。
     */
    fun generatedId(kind: String, ownerId: ChirSemanticId, index: Int): ChirSemanticId =
        ChirSemanticId("${ownerId.value}:$kind:$index")

    /**
     * 根据 CFIR 元素源码位置生成稳定元素 ID，缺少源码位置时回退到顺序编号。
     */
    fun elementId(kind: String, element: CfirElement, ownerId: ChirSemanticId, index: Int): ChirSemanticId {
        val stablePart = element.source?.toString()?.sanitizeId()?.takeIf { it.isNotBlank() }
        return if (stablePart != null) {
            ChirSemanticId("${ownerId.value}:$kind:$stablePart:$index")
        } else {
            generatedId(kind, ownerId, index)
        }
    }

    /**
     * 将 CFIR 文件名转换为 JVM 可接受的 module 名称。
     */
    fun moduleName(file: CfirFile): String {
        val baseName = file.name.substringBeforeLast('.').ifBlank { "module" }
        return baseName.sanitizeJvmIdentifier()
    }

    /**
     * 将任意字符串规整为可放入 CHIR 语义 ID 的片段。
     */
    private fun String.sanitizeId(): String =
        replace(Regex("[^A-Za-z0-9_.:$-]"), "_").trim('_').ifBlank { "anonymous" }

    /**
     * 将任意字符串规整为 JVM 标识符安全的名称。
     */
    private fun String.sanitizeJvmIdentifier(): String {
        val sanitized = replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
        if (sanitized.isBlank()) return "module"
        return if (sanitized.first().isDigit()) "module_$sanitized" else sanitized
    }
}
