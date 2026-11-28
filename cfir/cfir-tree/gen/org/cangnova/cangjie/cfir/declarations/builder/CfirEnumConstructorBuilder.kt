

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
import org.cangnova.cangjie.cfir.declarations.impl.CfirEnumConstructorImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirEnumConstructorBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    var contentSource: CfirContentSource = CfirContentSource.Owned
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    val typeParameters: MutableList<CfirTypeParameterRef> = mutableListOf()
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var returnTypeRef: CfirTypeRef
    var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    var dispatchReceiverType: ConeSimpleCangJieType? = null
    val valueParameters: MutableList<CfirValueParameter> = mutableListOf()
    var body: CfirBlock? = null
    lateinit var symbol: CfirEnumConstructorSymbol
    lateinit var status: CfirDeclarationStatus
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirEnumConstructor {
        return CfirEnumConstructorImpl(
            source,
            moduleData,
            resolvePhase,
            contentSource,
            annotations.toMutableOrEmpty(),
            origin,
            attributes,
            typeParameters,
            isLocal,
            returnTypeRef,
            deprecationsProvider,
            dispatchReceiverType,
            valueParameters,
            body,
            symbol,
            status,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumConstructor(init: CfirEnumConstructorBuilder.() -> Unit): CfirEnumConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirEnumConstructorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumConstructorCopy(original: CfirEnumConstructor, init: CfirEnumConstructorBuilder.() -> Unit): CfirEnumConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirEnumConstructorBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.isLocal = original.isLocal
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.status = original.status
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
