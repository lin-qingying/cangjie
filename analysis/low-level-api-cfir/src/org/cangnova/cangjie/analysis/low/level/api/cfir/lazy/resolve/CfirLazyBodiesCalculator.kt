/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirErrorPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjElement
import org.jetbrains.annotations.TestOnly

@LLCfirInternals
object CfirLazyBodiesCalculator {
    fun calculateBodies(designation: CfirDesignation) {
        designation.target.transformSingle(
            CfirTargetLazyBodiesCalculatorTransformer,
            designation.path.toPersistentList(),
        )
    }

    @TestOnly
    fun calculateAllLazyExpressionsInFile(cfirFile: CfirFile) {
        cfirFile.accept(RecursiveLazyAnnotationCalculatorVisitor, cfirFile.moduleData.session)
        cfirFile.transformSingle(CfirAllLazyBodiesCalculatorTransformer, persistentListOf())
    }

    fun calculateAnnotations(cfirElement: CfirElementWithResolveState) {
        cfirElement.accept(LazyAnnotationCalculatorVisitor, cfirElement.moduleData.session)
    }

    fun calculateAnnotation(annotationCall: CfirAnnotationCall, session: CfirSession) {
        calculateAnnotationCallIfNeeded(annotationCall, session)
    }

    fun createArgumentsForAnnotation(annotationCall: CfirAnnotationCall, session: CfirSession): CfirArgumentList {
        val annotationPsi = annotationCall.psi as? CjAnnotation
            ?: errorWithCfirSpecificEntries(
                "Annotation PSI is not found for lazy argument reconstruction",
                fir = annotationCall,
                psi = annotationCall.psi,
            )

        val rebuiltFile = PsiRawCfirBuilder(
            session,
            baseScopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFile(annotationPsi.containingCjFile)

        val rebuiltAnnotation = CfirElementFinder.findElementIn<CfirAnnotationCall>(rebuiltFile) { it.psi == annotationPsi }
            ?: errorWithCfirSpecificEntries(
                "Rebuilt annotation call was not found",
                fir = rebuiltFile,
                psi = annotationPsi,
            )

        return rebuiltAnnotation.argumentList
    }

    fun needCalculatingAnnotationCall(cfirAnnotationCall: CfirAnnotationCall): Boolean =
        cfirAnnotationCall.argumentList.arguments.any { it is CfirLazyExpression }
}

private inline fun <reified T : CfirDeclaration> revive(
    designation: CfirDesignation,
    psi: PsiElement? = designation.target.psi,
): T {
    val session = designation.target.moduleData.session
    val rootNonLocalDeclaration = psi as? CjElement
        ?: errorWithCfirSpecificEntries(
            "PSI is not available for lazy body reconstruction",
            fir = designation.target,
            psi = psi,
        )

    return RawCfirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
        session = session,
        scopeProvider = session.cangjieScopeProvider,
        designation = designation,
        rootNonLocalDeclaration = rootNonLocalDeclaration,
    ) as T
}

private fun replaceLazyValueParameters(target: CfirFunction, copy: CfirFunction) {
    val targetParameters = target.valueParameters
    val copyParameters = copy.valueParameters
    require(targetParameters.size == copyParameters.size)

    for ((valueParameter, newValueParameter) in targetParameters.zip(copyParameters)) {
        if (valueParameter.defaultValue is CfirLazyExpression) {
            valueParameter.replaceDefaultValue(newValueParameter.defaultValue)
        }
    }
}

private fun replaceLazyBody(target: CfirFunction, copy: CfirFunction) {
    if (target.body !is CfirLazyBlock) return
    target.replaceBody(copy.body)
}

private val CfirCallableDeclaration.originalPsi: PsiElement?
    get() = unwrapFakeOverridesOrDelegated().psi

private fun calculateLazyBodiesForFunction(designation: CfirDesignation) {
    val function = designation.target as CfirNamedFunction
    require(needCalculatingLazyBodyForFunction(function))

    val recreatedFunction = revive<CfirNamedFunction>(designation, function.originalPsi)
    replaceLazyBody(function, recreatedFunction)
    replaceLazyValueParameters(function, recreatedFunction)
}

private fun calculateLazyBodyForConstructor(designation: CfirDesignation) {
    val constructor = designation.target as CfirConstructor
    require(needCalculatingLazyBodyForConstructor(constructor))

    val recreatedConstructor = revive<CfirConstructor>(designation, constructor.originalPsi)
    replaceLazyBody(constructor, recreatedConstructor)
    replaceLazyValueParameters(constructor, recreatedConstructor)
}

private fun calculateLazyBodyForProperty(designation: CfirDesignation) {
    val property = designation.target as CfirProperty
    if (!needCalculatingLazyBodyForProperty(property)) return

    val recreatedProperty = revive<CfirProperty>(designation, property.originalPsi)

    property.getter?.let { getter ->
        val recreatedGetter = recreatedProperty.getter
            ?: errorWithCfirSpecificEntries("Recreated getter is missing", fir = recreatedProperty, psi = recreatedProperty.psi)
        replaceLazyBody(getter, recreatedGetter)
        replaceLazyValueParameters(getter, recreatedGetter)
    }

    property.setter?.let { setter ->
        val recreatedSetter = recreatedProperty.setter
            ?: errorWithCfirSpecificEntries("Recreated setter is missing", fir = recreatedProperty, psi = recreatedProperty.psi)
        replaceLazyBody(setter, recreatedSetter)
        replaceLazyValueParameters(setter, recreatedSetter)
    }
}

private fun needCalculatingLazyBodyForConstructor(constructor: CfirConstructor): Boolean =
    needCalculatingLazyBodyForFunction(constructor)

private fun needCalculatingLazyBodyForFunction(function: CfirFunction): Boolean =
    function.body is CfirLazyBlock || function.valueParameters.any { it.defaultValue is CfirLazyExpression }

private fun needCalculatingLazyBodyForProperty(property: CfirProperty): Boolean =
    property.getter?.let(::needCalculatingLazyBodyForFunction) == true ||
            property.setter?.let(::needCalculatingLazyBodyForFunction) == true

private fun calculateLazyBodyForCodeFragment(designation: CfirDesignation) {
    val codeFragment = designation.target as CfirCodeFragment
    require(codeFragment.block is CfirLazyBlock)

    val recreatedCodeFragment = revive<CfirCodeFragment>(designation, codeFragment.psi as? CjCodeFragment)
    codeFragment.replaceBlock(recreatedCodeFragment.block)
}

private object RecursiveLazyAnnotationCalculatorVisitor : RecursiveNonLocalAnnotationVisitor<CfirSession>() {
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

private object LazyAnnotationCalculatorVisitor : NonLocalAnnotationVisitor<CfirSession>() {
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

private fun calculateAnnotationCallIfNeeded(annotation: CfirAnnotation, session: CfirSession) {
    if (annotation !is CfirAnnotationCall || !CfirLazyBodiesCalculator.needCalculatingAnnotationCall(annotation)) return
    annotation.replaceArgumentList(CfirLazyBodiesCalculator.createArgumentsForAnnotation(annotation, session))
}

private object CfirAllLazyBodiesCalculatorTransformer : CfirLazyBodiesCalculatorTransformer() {
    override fun <E : CfirElement> transformElement(element: E, data: PersistentList<CfirDeclaration>): E {
        return recursiveTransformation(element, data)
    }
}

private object CfirTargetLazyBodiesCalculatorTransformer : CfirLazyBodiesCalculatorTransformer()

private sealed class CfirLazyBodiesCalculatorTransformer : CfirTransformer<PersistentList<CfirDeclaration>>() {
    override fun <E : CfirElement> transformElement(element: E, data: PersistentList<CfirDeclaration>): E = element

    override fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        data: PersistentList<CfirDeclaration>,
    ): CfirNamedFunction {
        if (needCalculatingLazyBodyForFunction(namedFunction)) {
            calculateLazyBodiesForFunction(CfirDesignation(data, namedFunction))
        }
        return namedFunction
    }

    override fun transformConstructor(
        constructor: CfirConstructor,
        data: PersistentList<CfirDeclaration>,
    ): CfirConstructor {
        if (needCalculatingLazyBodyForConstructor(constructor)) {
            calculateLazyBodyForConstructor(CfirDesignation(data, constructor))
        }
        return constructor
    }

    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: CfirErrorPrimaryConstructor,
        data: PersistentList<CfirDeclaration>,
    ): CfirErrorPrimaryConstructor {
        if (needCalculatingLazyBodyForConstructor(errorPrimaryConstructor)) {
            calculateLazyBodyForConstructor(CfirDesignation(data, errorPrimaryConstructor))
        }
        return errorPrimaryConstructor
    }

    override fun transformProperty(property: CfirProperty, data: PersistentList<CfirDeclaration>): CfirProperty {
        if (needCalculatingLazyBodyForProperty(property)) {
            calculateLazyBodyForProperty(CfirDesignation(data, property))
        }
        return property
    }

    override fun transformCodeFragment(
        codeFragment: CfirCodeFragment,
        data: PersistentList<CfirDeclaration>,
    ): CfirCodeFragment {
        if (codeFragment.block is CfirLazyBlock) {
            calculateLazyBodyForCodeFragment(CfirDesignation(data, codeFragment))
        }
        return codeFragment
    }
}

private fun <E : CfirElement> CfirTransformer<PersistentList<CfirDeclaration>>.recursiveTransformation(
    element: E,
    data: PersistentList<CfirDeclaration>,
): E {
    if (element is CfirFile || element is CfirClass || element is CfirExtend) {
        val newList = data.add(element as CfirDeclaration)
        element.transformChildren(this, newList)
    }

    return element
}
