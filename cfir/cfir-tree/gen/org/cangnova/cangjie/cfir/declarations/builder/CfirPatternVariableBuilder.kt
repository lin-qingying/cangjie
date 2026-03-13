

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternVariableImpl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef

@CfirBuilderDsl
class CfirPatternVariableBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    lateinit var pattern: CfirPattern
    var initializer: CfirExpression? = null
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirPatternVariable {
        return CfirPatternVariableImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
            attributes,
            status,
            typeParameters,
            returnTypeRef,
            pattern,
            initializer,
            isVar,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPatternVariable(init: CfirPatternVariableBuilder.() -> Unit): CfirPatternVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirPatternVariableBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPatternVariableCopy(original: CfirPatternVariable, init: CfirPatternVariableBuilder.() -> Unit): CfirPatternVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirPatternVariableBuilder()
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.pattern = original.pattern
    copyBuilder.initializer = original.initializer
    copyBuilder.isVar = original.isVar
    return copyBuilder.apply(init).build()
}
