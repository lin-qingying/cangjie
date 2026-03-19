

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirInvalidDeclarationImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirInvalidDeclarationBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirInvalidDeclaration {
        return CfirInvalidDeclarationImpl(
            source,
            moduleData,
            annotations,
            symbol,
            origin,
            attributes,
            reason,
        ).also {
            it.initDefaultResolveState()
        }
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildInvalidDeclaration(init: CfirInvalidDeclarationBuilder.() -> Unit): CfirInvalidDeclaration {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirInvalidDeclarationBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildInvalidDeclarationCopy(original: CfirInvalidDeclaration, init: CfirInvalidDeclarationBuilder.() -> Unit): CfirInvalidDeclaration {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirInvalidDeclarationBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
