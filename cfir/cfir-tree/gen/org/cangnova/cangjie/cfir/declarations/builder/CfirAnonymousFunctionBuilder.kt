

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
import org.cangnova.cangjie.cfir.declarations.impl.CfirAnonymousFunctionImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAnonymousFunctionBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var dispatchReceiverType: ConeSimpleCangJieType? = null
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    val valueParameters: MutableList<CfirValueParameter> = mutableListOf()
    var body: CfirBlock? = null
    lateinit var symbol: CfirAnonymousFunctionSymbol
    var hasExplicitParameterList: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var typeRef: CfirTypeRef
    var matchingParameterFunctionType: ConeCangJieType? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirAnonymousFunction {
        return CfirAnonymousFunctionImpl(
            source,
            moduleData,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            origin,
            attributes,
            isLocal,
            dispatchReceiverType,
            status,
            typeParameters,
            returnTypeRef,
            valueParameters,
            body,
            symbol,
            hasExplicitParameterList,
            isLambda,
            typeRef,
            matchingParameterFunctionType,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunction(init: CfirAnonymousFunctionBuilder.() -> Unit): CfirAnonymousFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAnonymousFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunctionCopy(original: CfirAnonymousFunction, init: CfirAnonymousFunctionBuilder.() -> Unit): CfirAnonymousFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirAnonymousFunctionBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.isLocal = original.isLocal
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.hasExplicitParameterList = original.hasExplicitParameterList
    copyBuilder.isLambda = original.isLambda
    copyBuilder.typeRef = original.typeRef
    copyBuilder.matchingParameterFunctionType = original.matchingParameterFunctionType
    return copyBuilder.apply(init).build()
}
