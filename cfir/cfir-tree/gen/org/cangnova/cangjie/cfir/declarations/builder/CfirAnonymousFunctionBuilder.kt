

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirContentSource
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
class CfirAnonymousFunctionBuilder : CfirFunctionBuilder {
    override var source: CjSourceElement? = null
    override lateinit var moduleData: CfirModuleData
    override lateinit var resolvePhase: CfirResolvePhase
    override var contentSource: CfirContentSource = CfirContentSource.Owned
    override val annotations: MutableList<CfirAnnotation> = mutableListOf()
    override lateinit var origin: CfirDeclarationOrigin
    override lateinit var attributes: CfirDeclarationAttributes
    override var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override lateinit var returnTypeRef: CfirTypeRef
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var dispatchReceiverType: ConeSimpleCangJieType? = null
    override var body: CfirBlock? = null
    override var status: CfirDeclarationStatus = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
    override val valueParameters: MutableList<CfirValueParameter> = mutableListOf()
    override var hasVariableLenArg: Boolean = false
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var symbol: CfirAnonymousFunctionSymbol
    var hasExplicitParameterList: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var typeRef: CfirTypeRef
    var matchingParameterFunctionType: ConeCangJieType? = null

    @OptIn(CfirImplementationDetail::class)
    override fun build(): CfirAnonymousFunction {
        return CfirAnonymousFunctionImpl(
            source,
            moduleData,
            resolvePhase,
            contentSource,
            annotations.toMutableOrEmpty(),
            origin,
            attributes,
            isLocal,
            returnTypeRef,
            deprecationsProvider,
            dispatchReceiverType,
            body,
            status,
            valueParameters,
            hasVariableLenArg,
            typeParameters,
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
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.body = original.body
    copyBuilder.status = original.status
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.hasVariableLenArg = original.hasVariableLenArg
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.hasExplicitParameterList = original.hasExplicitParameterList
    copyBuilder.isLambda = original.isLambda
    copyBuilder.typeRef = original.typeRef
    copyBuilder.matchingParameterFunctionType = original.matchingParameterFunctionType
    return copyBuilder.apply(init).build()
}
