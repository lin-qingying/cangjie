package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.source.CjSourceElement

open class CfirDeclarationRendererWithAttributes : CfirDeclarationRenderer() {
    override fun CfirDeclaration.renderDeclarationAttributes() {
        if (attributes.isNotEmpty()) {
            val attributes = getAttributesWithValues()
                .mapNotNull { (klass, value) ->
                    val unwrappedValue = when (value) {
                        is Lazy<*> -> value.value
                        else -> value
                    } ?: return@mapNotNull null
                    klass to unwrappedValue.renderAsDeclarationAttributeValue()
                }
                .ifEmpty { return }
                .joinToString { (name, value) -> "$name=$value" }
            printer.print("[$attributes] ")
        }
    }

    private fun CfirDeclaration.getAttributesWithValues(): List<Pair<String, Any?>> {
        return attributeTypesToIds()
            .sortedBy { it.first }
            .map { (klass, index) -> klass to attributes[index] }
    }

    protected open fun attributeTypesToIds(): List<Pair<String, Int>> {
        val attributeMap = CfirDeclarationDataRegistry.allValuesThreadUnsafeForRendering()
        return attributeMap.entries
            .map { it.key.substringAfterLast(".") to it.value }
    }

    private fun Any.renderAsDeclarationAttributeValue(): String = when (this) {
        is List<*> -> map { it?.renderAsDeclarationAttributeValue() }.toString()
        is Map<*, *> -> map { (key, value) ->
            key?.renderAsDeclarationAttributeValue() to value?.renderAsDeclarationAttributeValue()
        }.toMap().toString()
        is CfirCallableSymbol<*> -> callableIdAsString()
        is CfirClassLikeSymbol<*> -> classId.asString()
        is CfirCallableDeclaration -> symbol.callableIdAsString()
        is CjSourceElement -> "KtSourceElement"
        else -> toString()
    }
}
