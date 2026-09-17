

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.annotations.CangjieAnnotationTarget
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirAnnotationCallImpl(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var typeRef: CfirTypeRef,
    override var argumentMapping: CfirAnnotationArgumentMapping,
    override var annotationClassId: ClassId?,
    override var annotationTarget: CangjieAnnotationTarget?,
    override var forcedCustom: Boolean,
    override var annotationSourceName: String?,
    override var sourceModuleName: String,
    override var annotationKind: BuiltInAnnotationKind?,
    override var annotationIdentity: CangjieAnnotationIdentity?,
    override var annotationOrigin: CangjieAnnotationOrigin?,
    override var isCompileTimeVisible: Boolean?,
    override var argumentList: CfirArgumentList,
    override var calleeReference: CfirReference,
    override var argumentView: CfirAnnotationArgumentView?,
    override var annotationResolveState: CfirAnnotationResolveState,
    override val containingDeclarationSymbol: CfirBasedSymbol<*>,
) : CfirAnnotationCall() {
    override val arguments: List<CfirExpression>
        get() = argumentList.arguments

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        transformAnnotations(transformer, data)
        transformTypeRef(transformer, data)
        argumentList = argumentList.transform(transformer, data)
        transformCalleeReference(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        typeRef = typeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: CfirTransformer<D>, data: D): CfirAnnotationCallImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceTypeRef(newTypeRef: CfirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceArgumentMapping(newArgumentMapping: CfirAnnotationArgumentMapping) {
        argumentMapping = newArgumentMapping
    }

    override fun replaceAnnotationClassId(newAnnotationClassId: ClassId?) {
        annotationClassId = newAnnotationClassId
    }

    override fun replaceAnnotationTarget(newAnnotationTarget: CangjieAnnotationTarget?) {
        annotationTarget = newAnnotationTarget
    }

    override fun replaceForcedCustom(newForcedCustom: Boolean) {
        forcedCustom = newForcedCustom
    }

    override fun replaceAnnotationSourceName(newAnnotationSourceName: String?) {
        annotationSourceName = newAnnotationSourceName
    }

    override fun replaceSourceModuleName(newSourceModuleName: String) {
        sourceModuleName = newSourceModuleName
    }

    override fun replaceAnnotationKind(newAnnotationKind: BuiltInAnnotationKind?) {
        annotationKind = newAnnotationKind
    }

    override fun replaceAnnotationIdentity(newAnnotationIdentity: CangjieAnnotationIdentity?) {
        annotationIdentity = newAnnotationIdentity
    }

    override fun replaceAnnotationOrigin(newAnnotationOrigin: CangjieAnnotationOrigin?) {
        annotationOrigin = newAnnotationOrigin
    }

    override fun replaceIsCompileTimeVisible(newIsCompileTimeVisible: Boolean?) {
        isCompileTimeVisible = newIsCompileTimeVisible
    }

    override fun replaceArgumentList(newArgumentList: CfirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: CfirReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceArgumentView(newArgumentView: CfirAnnotationArgumentView?) {
        argumentView = newArgumentView
    }

    override fun replaceAnnotationResolveState(newAnnotationResolveState: CfirAnnotationResolveState) {
        annotationResolveState = newAnnotationResolveState
    }
}
