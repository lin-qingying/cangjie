

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirMainFunctionImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirMainFunctionBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var deprecationsProvider: DeprecationsProvider
    var dispatchReceiverType: ConeSimpleCangJieType? = null
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    val valueParameters: MutableList<CfirValueParameter> = mutableListOf()
    var body: CfirBlock? = null
    lateinit var symbol: CfirMainFunctionSymbol

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirMainFunction {
        return CfirMainFunctionImpl(
            source,
            moduleData,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            origin,
            attributes,
            isLocal,
            deprecationsProvider,
            dispatchReceiverType,
            status,
            typeParameters,
            returnTypeRef,
            valueParameters,
            body,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildMainFunction(init: CfirMainFunctionBuilder.() -> Unit): CfirMainFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirMainFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildMainFunctionCopy(original: CfirMainFunction, init: CfirMainFunctionBuilder.() -> Unit): CfirMainFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirMainFunctionBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.isLocal = original.isLocal
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
