package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 会渲染声明 attributes 的声明渲染器。
 */
open class CfirDeclarationRendererWithAttributes : CfirDeclarationRenderer() {
    /**
     * 渲染声明属性键值列表。
     */
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

    /**
     * 读取当前声明上所有带值的属性。
     */
    private fun CfirDeclaration.getAttributesWithValues(): List<Pair<String, Any?>> {
        return attributeTypesToIds()
            .sortedBy { it.first }
            .map { (klass, index) -> klass to attributes[index] }
    }

    /**
     * 返回属性类型名到内部槽位 id 的映射。
     */
    protected open fun attributeTypesToIds(): List<Pair<String, Int>> {
        val attributeMap = CfirDeclarationDataRegistry.allValuesThreadUnsafeForRendering()
        return attributeMap.entries
            .map { it.key.substringAfterLast(".") to it.value }
    }

    /**
     * 将属性值渲染成声明属性文本。
     */
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
