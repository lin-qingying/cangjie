

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirMacroExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirMacroExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var name: Name? = null
    var inputText: String? = null
    var attrText: String? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirMacroExpression {
        return CfirMacroExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            name,
            inputText,
            attrText,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildMacroExpression(init: CfirMacroExpressionBuilder.() -> Unit = {}): CfirMacroExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirMacroExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildMacroExpressionCopy(original: CfirMacroExpression, init: CfirMacroExpressionBuilder.() -> Unit = {}): CfirMacroExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirMacroExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.name = original.name
    copyBuilder.inputText = original.inputText
    copyBuilder.attrText = original.attrText
    return copyBuilder.apply(init).build()
}
