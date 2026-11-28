

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedQualifier
import org.cangnova.cangjie.cfir.expressions.impl.CfirResolvedQualifierImpl
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirResolvedQualifierBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var packageFqName: FqName
    var relativeClassFqName: FqName? = null
    var qualifierSymbol: CfirClassLikeSymbol<*>? = null
    var explicitParent: CfirResolvedQualifier? = null
    val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()
    var organizationName: Name? = null
    lateinit var referenceName: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedQualifier {
        return CfirResolvedQualifierImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            packageFqName,
            relativeClassFqName,
            qualifierSymbol,
            explicitParent,
            nonFatalDiagnostics.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            organizationName,
            referenceName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedQualifier(init: CfirResolvedQualifierBuilder.() -> Unit): CfirResolvedQualifier {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedQualifierBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedQualifierCopy(original: CfirResolvedQualifier, init: CfirResolvedQualifierBuilder.() -> Unit): CfirResolvedQualifier {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirResolvedQualifierBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.packageFqName = original.packageFqName
    copyBuilder.relativeClassFqName = original.relativeClassFqName
    copyBuilder.qualifierSymbol = original.qualifierSymbol
    copyBuilder.explicitParent = original.explicitParent
    copyBuilder.nonFatalDiagnostics.addAll(original.nonFatalDiagnostics)
    copyBuilder.typeArguments.addAll(original.typeArguments)
    copyBuilder.organizationName = original.organizationName
    copyBuilder.referenceName = original.referenceName
    return copyBuilder.apply(init).build()
}
