

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirTypeParameterBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var name: Name
    val bounds: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypeParameter {
        return CfirTypeParameterImpl(
            source,
            moduleData,
            annotations,
            symbol,
            origin,
            attributes,
            name,
            bounds,
        ).also {
            it.initDefaultResolveState()
        }
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameter(init: CfirTypeParameterBuilder.() -> Unit): CfirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypeParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameterCopy(original: CfirTypeParameter, init: CfirTypeParameterBuilder.() -> Unit): CfirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypeParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes
    copyBuilder.name = original.name
    copyBuilder.bounds.addAll(original.bounds)
    return copyBuilder.apply(init).build()
}
