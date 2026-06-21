package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

internal object Cfir2ChirIds {
    fun packageId(packageName: String): ChirSemanticId = ChirSemanticId("cfir:package:${packageName.sanitizeId()}")

    fun moduleId(file: CfirFile): ChirSemanticId = ChirSemanticId("cfir:file:${file.name.sanitizeId()}")

    fun declarationId(symbol: CfirBasedSymbol<*>): ChirSemanticId =
        ChirSemanticId("cfir:declaration:${symbol.debugName.sanitizeId()}")

    fun callableId(declaration: CfirCallableDeclaration): ChirSemanticId =
        ChirSemanticId("cfir:callable:${declaration.symbol.debugName.sanitizeId()}")

    fun parameterId(parameter: CfirValueParameter): ChirSemanticId =
        ChirSemanticId("cfir:parameter:${parameter.symbol.debugName.sanitizeId()}")

    fun generatedId(kind: String, ownerId: ChirSemanticId, index: Int): ChirSemanticId =
        ChirSemanticId("${ownerId.value}:$kind:$index")

    fun elementId(kind: String, element: CfirElement, ownerId: ChirSemanticId, index: Int): ChirSemanticId {
        val stablePart = element.source?.toString()?.sanitizeId()?.takeIf { it.isNotBlank() }
        return if (stablePart != null) {
            ChirSemanticId("${ownerId.value}:$kind:$stablePart:$index")
        } else {
            generatedId(kind, ownerId, index)
        }
    }

    fun moduleName(file: CfirFile): String {
        val baseName = file.name.substringBeforeLast('.').ifBlank { "module" }
        return baseName.sanitizeJvmIdentifier()
    }

    private fun String.sanitizeId(): String =
        replace(Regex("[^A-Za-z0-9_.:$-]"), "_").trim('_').ifBlank { "anonymous" }

    private fun String.sanitizeJvmIdentifier(): String {
        val sanitized = replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
        if (sanitized.isBlank()) return "module"
        return if (sanitized.first().isDigit()) "module_$sanitized" else sanitized
    }
}
