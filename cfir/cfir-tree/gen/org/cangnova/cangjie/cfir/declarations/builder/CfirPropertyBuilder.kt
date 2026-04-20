

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
import org.cangnova.cangjie.cfir.declarations.impl.CfirPropertyImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirPropertyBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    var dispatchReceiverType: ConeSimpleCangJieType? = null
    lateinit var symbol: CfirPropertySymbol
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    lateinit var name: Name
    var getter: CfirPropertyAccessor? = null
    var setter: CfirPropertyAccessor? = null
    var bodyResolveState: CfirPropertyBodyResolveState = CfirPropertyBodyResolveState.NOTHING_RESOLVED

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirProperty {
        return CfirPropertyImpl(
            source,
            moduleData,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            origin,
            attributes,
            isLocal,
            deprecationsProvider,
            dispatchReceiverType,
            symbol,
            status,
            typeParameters,
            returnTypeRef,
            name,
            getter,
            setter,
            bodyResolveState,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildProperty(init: CfirPropertyBuilder.() -> Unit): CfirProperty {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirPropertyBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyCopy(original: CfirProperty, init: CfirPropertyBuilder.() -> Unit): CfirProperty {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirPropertyBuilder()
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
    copyBuilder.name = original.name
    copyBuilder.getter = original.getter
    copyBuilder.setter = original.setter
    copyBuilder.bodyResolveState = original.bodyResolveState
    return copyBuilder.apply(init).build()
}
