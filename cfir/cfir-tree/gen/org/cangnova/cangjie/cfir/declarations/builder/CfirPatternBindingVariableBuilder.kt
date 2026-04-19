

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
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternBindingVariableImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirPatternBindingVariableBuilder {
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
    var initializer: CfirExpression? = null
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var symbol: CfirPatternBindingSymbol
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirPatternBindingVariable {
        return CfirPatternBindingVariableImpl(
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
            initializer,
            isVar,
            symbol,
            typeParameters,
            returnTypeRef,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPatternBindingVariable(init: CfirPatternBindingVariableBuilder.() -> Unit): CfirPatternBindingVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirPatternBindingVariableBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPatternBindingVariableCopy(original: CfirPatternBindingVariable, init: CfirPatternBindingVariableBuilder.() -> Unit): CfirPatternBindingVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirPatternBindingVariableBuilder()
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
    copyBuilder.initializer = original.initializer
    copyBuilder.isVar = original.isVar
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
