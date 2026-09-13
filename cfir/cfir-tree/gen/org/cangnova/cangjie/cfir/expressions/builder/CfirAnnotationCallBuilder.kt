

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationCallImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAnnotationCallBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var typeRef: CfirTypeRef
    val arguments: MutableList<CfirElement> = mutableListOf()
    var annotationKind: CangjieAnnotationKind? = null
    var annotationOrigin: CangjieAnnotationOrigin? = null
    var isCompileTimeVisible: Boolean? = null
    lateinit var argumentList: CfirArgumentList
    lateinit var calleeReference: CfirReference
    var argumentView: CfirAnnotationArgumentView? = null
    var annotationResolveState: CfirAnnotationResolveState = CfirAnnotationResolveState.UNRESOLVED
    lateinit var containingDeclarationSymbol: CfirBasedSymbol<*>

    fun build(): CfirAnnotationCall {
        return CfirAnnotationCallImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            typeRef,
            arguments,
            annotationKind,
            annotationOrigin,
            isCompileTimeVisible,
            argumentList,
            calleeReference,
            argumentView,
            annotationResolveState,
            containingDeclarationSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCall(init: CfirAnnotationCallBuilder.() -> Unit): CfirAnnotationCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAnnotationCallBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCallCopy(original: CfirAnnotationCall, init: CfirAnnotationCallBuilder.() -> Unit): CfirAnnotationCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirAnnotationCallBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.typeRef = original.typeRef
    copyBuilder.arguments.addAll(original.arguments)
    copyBuilder.annotationKind = original.annotationKind
    copyBuilder.annotationOrigin = original.annotationOrigin
    copyBuilder.isCompileTimeVisible = original.isCompileTimeVisible
    copyBuilder.argumentList = original.argumentList
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.argumentView = original.argumentView
    copyBuilder.annotationResolveState = original.annotationResolveState
    copyBuilder.containingDeclarationSymbol = original.containingDeclarationSymbol
    return copyBuilder.apply(init).build()
}
