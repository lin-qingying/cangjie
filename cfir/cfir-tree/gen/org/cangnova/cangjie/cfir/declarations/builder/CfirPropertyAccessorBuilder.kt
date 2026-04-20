

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
import org.cangnova.cangjie.cfir.declarations.impl.CfirPropertyAccessorImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirPropertyAccessorBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    var dispatchReceiverType: ConeSimpleCangJieType? = null
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    val valueParameters: MutableList<CfirValueParameter> = mutableListOf()
    var body: CfirBlock? = null
    lateinit var symbol: CfirPropertyAccessorSymbol
    lateinit var propertySymbol: CfirPropertySymbol
    var isGetter: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirPropertyAccessor {
        return CfirPropertyAccessorImpl(
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
            propertySymbol,
            isGetter,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessor(init: CfirPropertyAccessorBuilder.() -> Unit): CfirPropertyAccessor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirPropertyAccessorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessorCopy(original: CfirPropertyAccessor, init: CfirPropertyAccessorBuilder.() -> Unit): CfirPropertyAccessor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirPropertyAccessorBuilder()
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
    copyBuilder.propertySymbol = original.propertySymbol
    copyBuilder.isGetter = original.isGetter
    return copyBuilder.apply(init).build()
}
