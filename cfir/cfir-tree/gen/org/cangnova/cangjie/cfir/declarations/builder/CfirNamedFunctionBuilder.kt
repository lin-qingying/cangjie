

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
import org.cangnova.cangjie.cfir.declarations.impl.CfirNamedFunctionImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNamedFunctionBuilder : CfirFunctionBuilder {
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
    override lateinit var status: CfirDeclarationStatus
    override val valueParameters: MutableList<CfirValueParameter> = mutableListOf()
    override var hasVariableLenArg: Boolean = false
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var symbol: CfirNamedFunctionSymbol
    lateinit var name: Name
    var isMut: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    override fun build(): CfirNamedFunction {
        return CfirNamedFunctionImpl(
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
            name,
            isMut,
        )
    }


}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedFunction(init: CfirNamedFunctionBuilder.() -> Unit): CfirNamedFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNamedFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedFunctionCopy(original: CfirNamedFunction, init: CfirNamedFunctionBuilder.() -> Unit): CfirNamedFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNamedFunctionBuilder()
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
    copyBuilder.name = original.name
    copyBuilder.isMut = original.isMut
    return copyBuilder.apply(init).build()
}
