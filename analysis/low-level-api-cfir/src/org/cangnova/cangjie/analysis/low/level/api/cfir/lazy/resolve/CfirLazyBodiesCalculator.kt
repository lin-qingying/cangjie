/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.TestOnly
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.withCfirDesignationEntry
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.getExplicitBackingField
import org.cangnova.cangjie.cfir.declarations.utils.hasGeneratedDelegateBody
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirLazyDelegatedConstructorCall
import org.cangnova.cangjie.cfir.expressions.impl.CfirSingleExpressionBlock
import org.cangnova.cangjie.cfir.references.CfirDelegateFieldReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildDelegateFieldReference
import org.cangnova.cangjie.cfir.references.builder.buildImplicitThisReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.scopes.kotlinScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirReceiverParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildTypeProjectionWithVariance
import org.cangnova.cangjie.cfir.types.impl.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjAnnotationEntry
import org.cangnova.cangjie.psi.CjClassOrObject
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

@LLCfirInternals
object CfirLazyBodiesCalculator {
    fun calculateBodies(designation: CfirDesignation) {
        designation.target.transformSingle(
            CfirTargetLazyBodiesCalculatorTransformer,
            designation.path.toPersistentList(),
        )
    }

    @TestOnly
    fun calculateAllLazyExpressionsInFile(firFile: CfirFile) {
        firFile.accept(RecursiveLazyAnnotationCalculatorVisitor, firFile.moduleData.session)
        firFile.transformSingle(CfirAllLazyBodiesCalculatorTransformer, persistentListOf())
    }

    fun calculateAnnotations(firElement: CfirElementWithResolveState) {
        firElement.accept(LazyAnnotationCalculatorVisitor, firElement.moduleData.session)
    }

    fun calculateAnnotation(annotationCall: CfirAnnotationCall, session: CfirSession) {
        calculateAnnotationCallIfNeeded(annotationCall, session)
    }

    fun createArgumentsForAnnotation(annotationCall: CfirAnnotationCall, session: CfirSession): CfirArgumentList {
        val builder = PsiRawCfirBuilder(session, baseScopeProvider = session.kotlinScopeProvider)
        val ktAnnotationEntry = annotationCall.psi as CjAnnotationEntry
        builder.context.packageFqName = ktAnnotationEntry.containingCjFile.packageFqName
        val newAnnotationCall = builder.buildAnnotationCall(ktAnnotationEntry, annotationCall.containingDeclarationSymbol)
        return newAnnotationCall.argumentList
    }

    fun needCalculatingAnnotationCall(firAnnotationCall: CfirAnnotationCall): Boolean =
        firAnnotationCall.argumentList.arguments.any { it is CfirLazyExpression }
}

private inline fun <reified T : CfirDeclaration> revive(
    designation: CfirDesignation,
    psi: PsiElement? = designation.target.psi,
): T {
    val session = designation.target.moduleData.session

    return RawCfirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
        session = session,
        scopeProvider = session.kotlinScopeProvider,
        designation = designation,
        rootNonLocalDeclaration = psi as CjAnnotated,
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

/**
 * @param isContractResolved is **false** during [CfirResolvePhase.CONTRACTS]
 * and **true** for the following phases.
 * **true** flag assumes that the declaration already passes the [CfirResolvePhase.CONTRACTS] phase,
 * so it is possible to depend on [CfirContractDescriptionOwner.contractDescription].
 *
 * Raw body may have false-positive contracts, so the final decision will be made only during the [CfirResolvePhase.CONTRACTS] phase.
 * In the case of a false positive the redundant [CfirContractCallBlock] should be unwrapped to allow the body be processed
 * correctly by other transformers and checkers.
 */
private fun replaceLazyBody(target: CfirFunction, copy: CfirFunction) {
    if (target.body !is CfirLazyBlock) return

    val newBody = copy.body
    target.replaceBody(newBody)
}

private fun replaceLazyDelegatedConstructor(target: CfirConstructor, copy: CfirConstructor) {
    val targetCall = target.delegatedConstructor
    val copyCall = copy.delegatedConstructor

    when (targetCall) {
        is CfirLazyDelegatedConstructorCall -> {
            require(copyCall !is CfirMultiDelegatedConstructorCall)
            target.replaceDelegatedConstructor(copyCall)
        }
        is CfirMultiDelegatedConstructorCall -> {
            require(copyCall is CfirMultiDelegatedConstructorCall)
            require(targetCall.delegatedConstructorCalls.size == copyCall.delegatedConstructorCalls.size)

            val newCalls = targetCall.delegatedConstructorCalls.zip(copyCall.delegatedConstructorCalls)
                .map { (target, copy) -> target.takeUnless { it is CfirLazyDelegatedConstructorCall } ?: copy }

            targetCall.replaceDelegatedConstructorCalls(newCalls)
        }
    }
}

private fun replaceLazyInitializer(target: CfirVariable, copy: CfirVariable) {
    if (target.initializer is CfirLazyExpression) {
        target.replaceInitializer(copy.initializer)
    }
}

private fun replaceLazyDelegate(target: CfirVariable, copy: CfirVariable) {
    if (target.delegate is CfirLazyExpression) {
        target.replaceDelegate(copy.delegate)
    }
}

private val CfirCallableDeclaration.originalPsi: PsiElement? get() = unwrapFakeOverridesOrDelegated().psi

private fun calculateLazyBodiesForFunction(designation: CfirDesignation) {
    val simpleFunction = designation.target as CfirNamedFunction
    require(needCalculatingLazyBodyForFunction(simpleFunction))

    val newSimpleFunction = revive<CfirNamedFunction>(designation, simpleFunction.originalPsi)

    replaceLazyBody(simpleFunction, newSimpleFunction)
    replaceLazyValueParameters(simpleFunction, newSimpleFunction)
}

private fun calculateLazyBodyForConstructor(designation: CfirDesignation) {
    val constructor = designation.target as CfirConstructor
    require(needCalculatingLazyBodyForConstructor(constructor))

    // TODO A temporary hack to avoid problems with lazy resolve of typealiased constructors; see KT-73481
    val constructorPsi = (constructor.typeAliasConstructorInfo?.originalConstructor ?: constructor).psi

    val newConstructor = revive<CfirConstructor>(designation, constructorPsi)

    replaceLazyBody(constructor, newConstructor)
    replaceLazyDelegatedConstructor(constructor, newConstructor)
    replaceLazyValueParameters(constructor, newConstructor)
}

private fun calculateLazyBodyForProperty(designation: CfirDesignation) {
    val firProperty = designation.target as CfirProperty
    if (!needCalculatingLazyBodyForProperty(firProperty)) return
    if (firProperty.origin == CfirDeclarationOrigin.ScriptCustomization.ResultProperty) {
        calculateLazyBodyForResultProperty(firProperty, designation)
        return
    }

    val recreatedProperty = revive<CfirProperty>(designation, firProperty.originalPsi)

    firProperty.getter?.let { getter ->
        val recreatedGetter = recreatedProperty.getter!!
        replaceLazyBody(getter, recreatedGetter)
        rebindDelegatedAccessorBody(newTarget = getter, oldTarget = recreatedGetter)
    }

    firProperty.setter?.let { setter ->
        val recreatedSetter = recreatedProperty.setter!!
        replaceLazyBody(setter, recreatedSetter)
        rebindDelegatedAccessorBody(newTarget = setter, oldTarget = recreatedSetter)
    }

    replaceLazyInitializer(firProperty, recreatedProperty)
    replaceLazyDelegate(firProperty, recreatedProperty)
    rebindDelegate(newTarget = firProperty, oldTarget = recreatedProperty)

    firProperty.getExplicitBackingField()?.let { backingField ->
        val newBackingField = recreatedProperty.getExplicitBackingField()!!
        replaceLazyInitializer(backingField, newBackingField)
    }
}

private fun calculateLazyBodyForResultProperty(firProperty: CfirProperty, designation: CfirDesignation) {
    val newInitializer = revive<CfirAnonymousInitializer>(designation)
    val body = newInitializer.body
    requireWithAttachment(body != null, { "${CfirAnonymousInitializer::class.simpleName} without body" }) {
        withCfirDesignationEntry("designation", designation)
        withCfirEntry("initializer", newInitializer)
    }

    val singleStatement = body.statements.singleOrNull()
    requireWithAttachment(singleStatement is CfirExpression, { "Unexpected body content" }) {
        withCfirDesignationEntry("designation", designation)
        withCfirEntry("initializer", newInitializer)
        singleStatement?.let {
            withCfirEntry("statement", it)
        }
    }

    firProperty.replaceInitializer(singleStatement)
}

/**
 * This function is required to correctly rebind symbols
 * after [generateAccessorsByDelegate][org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate]
 * for correct work
 *
 * @see org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate
 */
private fun rebindDelegate(newTarget: CfirProperty, oldTarget: CfirProperty) {
    val delegate = newTarget.delegate ?: return
    requireWithAttachment(
        delegate is CfirWrappedDelegateExpression,
        { "Unexpected delegate type: ${delegate::class.simpleName}" },
    ) {
        withCfirEntry("newTarget", newTarget)
        withCfirEntry("oldTarget", oldTarget)
        withCfirEntry("delegate", delegate)
    }

    val delegateProvider = delegate.provideDelegateCall
    rebindArgumentList(
        delegateProvider.argumentList,
        newTarget = newTarget.symbol,
        oldTarget = oldTarget.symbol,
        isSetter = false,
        canHavePropertySymbolAsThisReference = false,
    )
}

/**
 * This function is required to correctly rebind symbols
 * after [generateAccessorsByDelegate][org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate]
 * for correct work
 *
 * @see org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate
 * @see rebindDelegate
 */
private fun rebindDelegatedAccessorBody(newTarget: CfirPropertyAccessor, oldTarget: CfirPropertyAccessor) {
    if (!newTarget.hasGeneratedDelegateBody()) return
    val body = newTarget.body
    requireWithAttachment(
        body is CfirSingleExpressionBlock,
        { "Unexpected body for generated accessor ${body?.let { it::class.simpleName }}" },
    ) {
        withCfirSymbolEntry("newTarget", newTarget.propertySymbol)
        withCfirSymbolEntry("oldTarget", oldTarget.propertySymbol)
        body?.let { withCfirEntry("body", it) } ?: withEntry("body", "null")
    }

    val returnExpression = body.statement
    rebindReturnExpression(returnExpression = returnExpression, newTarget = newTarget, oldTarget = oldTarget)
}

private fun rebindReturnExpression(returnExpression: CfirStatement, newTarget: CfirPropertyAccessor, oldTarget: CfirPropertyAccessor) {
    requireWithAttachment(returnExpression is CfirReturnExpression, { "Unexpected single statement" }) {
        withCfirSymbolEntry("newTarget", newTarget.propertySymbol)
        withCfirSymbolEntry("oldTarget", oldTarget.propertySymbol)
        withCfirEntry("expression", returnExpression)
    }

    val functionCall = returnExpression.result
    rebindFunctionCall(functionCall, newTarget, oldTarget)
}

private fun rebindFunctionCall(functionCall: CfirExpression, newTarget: CfirPropertyAccessor, oldTarget: CfirPropertyAccessor) {
    requireWithAttachment(functionCall is CfirFunctionCall, { "Unexpected result expression ${functionCall::class.simpleName}" }) {
        withCfirSymbolEntry("newTarget", newTarget.propertySymbol)
        withCfirSymbolEntry("oldTarget", oldTarget.propertySymbol)
        withCfirEntry("functionCall", functionCall)
    }

    rebindDelegateAccess(
        expression = functionCall.explicitReceiver,
        newPropertySymbol = newTarget.propertySymbol,
        oldPropertySymbol = oldTarget.propertySymbol,
    )

    rebindArgumentList(
        argumentList = functionCall.argumentList,
        newTarget = newTarget.propertySymbol,
        oldTarget = oldTarget.propertySymbol,
        isSetter = newTarget.isSetter,
        canHavePropertySymbolAsThisReference = true,
    )
}

/**
 * To cover `thisRef` function
 *
 * @see org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate
 */
private fun rebindThisRef(
    expression: CfirExpression,
    newTarget: CfirPropertySymbol,
    oldTarget: CfirPropertySymbol,
    canHavePropertySymbolAsThisReference: Boolean,
) {
    if (expression is CfirLiteralExpression) return

    requireWithAttachment(
        expression is CfirThisReceiverExpression,
        { "Unexpected this reference expression: ${expression::class.simpleName}" },
    ) {
        withCfirSymbolEntry("newTarget", newTarget)
        withCfirSymbolEntry("oldTarget", oldTarget)
        withCfirEntry("expression", expression)
    }

    val boundSymbol = expression.calleeReference.boundSymbol
    if (boundSymbol is CfirClassSymbol<*>) return
    requireWithAttachment(
        canHavePropertySymbolAsThisReference,
        { "Class bound symbol is not found: ${boundSymbol?.let { it::class.simpleName }}" },
    ) {
        withCfirSymbolEntry("newTarget", newTarget)
        withCfirSymbolEntry("oldTarget", oldTarget)
        boundSymbol?.let { withCfirSymbolEntry("boundSymbol", boundSymbol as CfirBasedSymbol<*>) }
    }

    requireWithAttachment(
        boundSymbol is CfirReceiverParameterSymbol && boundSymbol.containingDeclarationSymbol == oldTarget,
        {
            "Unexpected bound symbol: ${boundSymbol?.let { it::class.simpleName }}"
        }
    ) {
        withCfirSymbolEntry("newTarget", newTarget)
        withCfirSymbolEntry("oldTarget", oldTarget)
        boundSymbol?.let { withCfirSymbolEntry("boundSymbol", boundSymbol as CfirBasedSymbol<*>) }
    }

    expression.replaceCalleeReference(buildImplicitThisReference {
        this.boundSymbol = newTarget.receiverParameterSymbol!!
    })
}

private fun rebindArgumentList(
    argumentList: CfirArgumentList,
    newTarget: CfirPropertySymbol,
    oldTarget: CfirPropertySymbol,
    isSetter: Boolean,
    canHavePropertySymbolAsThisReference: Boolean,
) {
    val arguments = argumentList.arguments
    val expectedSize = 2 + if (isSetter) 1 else 0
    requireWithAttachment(
        arguments.size == expectedSize,
        { "Unexpected arguments size. Expected: $expectedSize, actual: ${arguments.size}" },
    ) {
        withCfirSymbolEntry("newTarget", newTarget)
        withCfirSymbolEntry("oldTarget", oldTarget)
        withCfirEntry("expression", argumentList)
    }

    rebindThisRef(
        expression = arguments[0],
        newTarget = newTarget,
        oldTarget = oldTarget,
        canHavePropertySymbolAsThisReference = canHavePropertySymbolAsThisReference,
    )

    rebindPropertyRef(expression = arguments[1], newPropertySymbol = newTarget, oldPropertySymbol = oldTarget)

    if (isSetter) {
        rebindSetterParameter(expression = arguments[2], newPropertySymbol = newTarget, oldPropertySymbol = oldTarget)
    }
}

/**
 * To cover third argument in setter body
 *
 * @see org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate
 */
private fun rebindSetterParameter(expression: CfirExpression, newPropertySymbol: CfirPropertySymbol, oldPropertySymbol: CfirPropertySymbol) {
    requireWithAttachment(
        expression is CfirPropertyAccessExpression,
        { "Unexpected third argument: ${expression::class.simpleName}" }) {
        withCfirSymbolEntry("newTarget", newPropertySymbol)
        withCfirSymbolEntry("oldTarget", oldPropertySymbol)
        withCfirEntry("expression", expression)
    }

    val calleeReference = expression.resolvedCalleeReference(newPropertySymbol = newPropertySymbol, oldPropertySymbol = oldPropertySymbol)
    val resolvedParameterSymbol = calleeReference.resolvedSymbol
    val oldValueParameterSymbol = oldPropertySymbol.setterSymbol?.valueParameterSymbols?.first()
    requireWithAttachment(
        resolvedParameterSymbol == oldValueParameterSymbol,
        { "Unexpected symbol: ${resolvedParameterSymbol::class.simpleName}" },
    ) {
        withCfirEntry("expression", expression)
        withCfirSymbolEntry("actualOldParameter", resolvedParameterSymbol)
        oldValueParameterSymbol?.let { withCfirSymbolEntry("expectedOldParameter", it) }
        withCfirSymbolEntry("oldProperty", oldPropertySymbol)
        withCfirSymbolEntry("newProperty", newPropertySymbol)
    }

    expression.replaceCalleeReference(buildResolvedNamedReference {
        source = calleeReference.source
        name = calleeReference.name
        resolvedSymbol = newPropertySymbol.setterSymbol?.valueParameterSymbols?.first() ?: errorWithAttachment("Parameter is not found") {
            withCfirSymbolEntry("oldProperty", oldPropertySymbol)
            withCfirSymbolEntry("newProperty", newPropertySymbol)
        }
    })
}

private fun CfirQualifiedAccessExpression.resolvedCalleeReference(
    newPropertySymbol: CfirPropertySymbol,
    oldPropertySymbol: CfirPropertySymbol,
): CfirResolvedNamedReference {
    val calleeReference = calleeReference
    requireWithAttachment(
        calleeReference is CfirResolvedNamedReference,
        { "Unexpected callee reference: ${calleeReference::class.simpleName}" },
    ) {
        withCfirSymbolEntry("oldProperty", oldPropertySymbol)
        withCfirSymbolEntry("newProperty", newPropertySymbol)
        withCfirEntry("calleeReference", calleeReference)
    }

    return calleeReference
}

/**
 * To cover `propertyRef` function
 *
 * @see org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate
 */
private fun rebindPropertyRef(
    expression: CfirExpression,
    newPropertySymbol: CfirPropertySymbol,
    oldPropertySymbol: CfirPropertySymbol,
) {
    requireWithAttachment(
        expression is CfirCallableReferenceAccess,
        { "Unexpected second argument: ${expression::class.simpleName}" },
    ) {
        withCfirSymbolEntry("newTarget", newPropertySymbol)
        withCfirSymbolEntry("oldTarget", oldPropertySymbol)
        withCfirEntry("expression", expression)
    }

    val calleeReference = expression.resolvedCalleeReference(newPropertySymbol = newPropertySymbol, oldPropertySymbol = oldPropertySymbol)
    val resolvedPropertySymbol = calleeReference.resolvedSymbol
    requireWithAttachment(
        resolvedPropertySymbol == oldPropertySymbol,
        { "Unexpected symbol: ${resolvedPropertySymbol::class.simpleName}" },
    ) {
        withCfirEntry("expression", expression)
        withCfirSymbolEntry("actualOldProperty", resolvedPropertySymbol)
        withCfirSymbolEntry("expectedOldProperty", oldPropertySymbol)
        withCfirSymbolEntry("newProperty", newPropertySymbol)
    }

    expression.replaceCalleeReference(buildResolvedNamedReference {
        source = calleeReference.source
        name = calleeReference.name
        resolvedSymbol = newPropertySymbol
    })

    expression.replaceTypeArguments(newPropertySymbol.cfir.typeParameters.map {
        buildTypeProjectionWithVariance {
            source = expression.source
            variance = Variance.INVARIANT
            typeRef = buildResolvedTypeRef {
                coneType = ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
            }
        }
    })
}

/**
 * To cover `delegateAccess` function
 *
 * @see org.cangnova.cangjie.cfir.builder.generateAccessorsByDelegate
 */
private fun rebindDelegateAccess(expression: CfirExpression?, newPropertySymbol: CfirPropertySymbol, oldPropertySymbol: CfirPropertySymbol) {
    requireWithAttachment(
        expression is CfirPropertyAccessExpression,
        { "Unexpected delegate accessor expression: ${expression?.let { it::class.simpleName }}" },
    ) {
        withCfirSymbolEntry("newTarget", newPropertySymbol)
        withCfirSymbolEntry("oldTarget", oldPropertySymbol)
        expression?.let { withCfirEntry("expression", it) }
    }

    val delegateFieldReference = expression.calleeReference
    requireWithAttachment(
        delegateFieldReference is CfirDelegateFieldReference,
        { "Unexpected callee reference: ${delegateFieldReference::class.simpleName}" },
    ) {
        withCfirSymbolEntry("newTarget", newPropertySymbol)
        withCfirSymbolEntry("oldTarget", oldPropertySymbol)
        withCfirEntry("delegateFieldReference", delegateFieldReference)
    }

    requireWithAttachment(
        delegateFieldReference.resolvedSymbol == oldPropertySymbol.delegateFieldSymbol,
        { "Unexpected delegate field symbol" }
    ) {
        withCfirSymbolEntry("newTarget", newPropertySymbol)
        withCfirSymbolEntry("oldTarget", oldPropertySymbol)
        withCfirSymbolEntry("field", delegateFieldReference.resolvedSymbol)
    }

    expression.replaceCalleeReference(buildDelegateFieldReference {
        source = delegateFieldReference.source
        resolvedSymbol = newPropertySymbol.delegateFieldSymbol ?: errorWithAttachment("Delegate field is missing") {
            withCfirSymbolEntry("newTarget", newPropertySymbol)
            withCfirSymbolEntry("oldTarget", oldPropertySymbol)
        }
    })

    expression.dispatchReceiver?.let {
        rebindThisRef(
            expression = it,
            newTarget = newPropertySymbol,
            oldTarget = oldPropertySymbol,
            canHavePropertySymbolAsThisReference = false,
        )
    }
}

private fun calculateLazyInitializerForEnumEntry(designation: CfirDesignation) {
    val enumEntry = designation.target as CfirEnumEntry
    require(enumEntry.initializer is CfirLazyExpression)

    val newEnumEntry = revive<CfirEnumEntry>(designation)
    enumEntry.replaceInitializer(newEnumEntry.initializer)
}

private fun calculateLazyBodyForAnonymousInitializer(designation: CfirDesignation) {
    val initializer = designation.target as CfirAnonymousInitializer
    require(initializer.body is CfirLazyBlock)

    val newInitializer = revive<CfirAnonymousInitializer>(designation)
    initializer.replaceBody(newInitializer.body)
}

private fun needCalculatingLazyBodyForConstructor(firConstructor: CfirConstructor): Boolean {
    if (needCalculatingLazyBodyForFunction(firConstructor) || firConstructor.delegatedConstructor is CfirLazyDelegatedConstructorCall) {
        return true
    }
    val delegatedConstructor = firConstructor.delegatedConstructor
    if (delegatedConstructor is CfirMultiDelegatedConstructorCall) {
        for (delegated in delegatedConstructor.delegatedConstructorCalls) {
            if (delegated is CfirLazyDelegatedConstructorCall) {
                return true
            }
        }
    }
    return false
}

private fun calculateLazyBodiesForField(designation: CfirDesignation) {
    val field = designation.target as CfirField
    require(field.initializer is CfirLazyExpression)

    // 'designation.path.last()' cannot be used here, as for dangling files designation target may be in a different file
    val psi = field.psi?.getStrictParentOfType<CjClassOrObject>()

    val newField = revive<CfirField>(designation, psi)
    field.replaceInitializer(newField.initializer)
}

private fun needCalculatingLazyBodyForFunction(firFunction: CfirFunction): Boolean {
    return firFunction.body is CfirLazyBlock || firFunction.valueParameters.any { it.defaultValue is CfirLazyExpression }
}

private fun needCalculatingLazyBodyForProperty(firProperty: CfirProperty): Boolean =
    firProperty.getter?.let { needCalculatingLazyBodyForFunction(it) } == true
            || firProperty.setter?.let { needCalculatingLazyBodyForFunction(it) } == true
            || firProperty.initializer is CfirLazyExpression
            || firProperty.delegate is CfirLazyExpression
            || firProperty.getExplicitBackingField()?.initializer is CfirLazyExpression

private fun calculateLazyBodyForCodeFragment(designation: CfirDesignation) {
    val codeFragment = designation.target as CfirCodeFragment
    require(codeFragment.block is CfirLazyBlock)

    val newCodeFragment = revive<CfirCodeFragment>(designation)
    codeFragment.replaceBlock(newCodeFragment.block)
}

/**
 * This object is supposed to be used only for tests.
 *
 * @see LazyAnnotationCalculatorVisitor
 */
private object RecursiveLazyAnnotationCalculatorVisitor : RecursiveNonLocalAnnotationVisitor<CfirSession>() {
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

/**
 * Calculates all non-local lazy annotations on a provided declaration.
 */
private object LazyAnnotationCalculatorVisitor : NonLocalAnnotationVisitor<CfirSession>() {
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

private fun calculateAnnotationCallIfNeeded(annotation: CfirAnnotation, session: CfirSession) {
    if (annotation !is CfirAnnotationCall || !CfirLazyBodiesCalculator.needCalculatingAnnotationCall(annotation)) return

    val newArgumentList = CfirLazyBodiesCalculator.createArgumentsForAnnotation(annotation, session)
    annotation.replaceArgumentList(newArgumentList)
}

private object CfirAllLazyBodiesCalculatorTransformer : CfirLazyBodiesCalculatorTransformer() {
    override fun <E : CfirElement> transformElement(element: E, data: PersistentList<CfirDeclaration>): E {
        return recursiveTransformation(element, data)
    }
}

private object CfirTargetLazyBodiesCalculatorTransformer : CfirLazyBodiesCalculatorTransformer()

private sealed class CfirLazyBodiesCalculatorTransformer : CfirTransformer<PersistentList<CfirDeclaration>>() {
    override fun <E : CfirElement> transformElement(element: E, data: PersistentList<CfirDeclaration>): E = element

    override fun transformField(field: CfirField, data: PersistentList<CfirDeclaration>): CfirStatement {
        if (field.initializer is CfirLazyExpression) {
            val designation = CfirDesignation(data, field)
            calculateLazyBodiesForField(designation)
        }

        return field
    }

    override fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        data: PersistentList<CfirDeclaration>,
    ): CfirNamedFunction {
        if (needCalculatingLazyBodyForFunction(namedFunction)) {
            val designation = CfirDesignation(data, namedFunction)
            calculateLazyBodiesForFunction(designation)
        }

        return namedFunction
    }

    override fun transformConstructor(
        constructor: CfirConstructor,
        data: PersistentList<CfirDeclaration>,
    ): CfirConstructor {
        if (needCalculatingLazyBodyForConstructor(constructor)) {
            val designation = CfirDesignation(data, constructor)
            calculateLazyBodyForConstructor(designation)
        }

        return constructor
    }

    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: CfirErrorPrimaryConstructor,
        data: PersistentList<CfirDeclaration>,
    ) = transformConstructor(errorPrimaryConstructor, data)

    override fun transformProperty(property: CfirProperty, data: PersistentList<CfirDeclaration>): CfirProperty {
        if (needCalculatingLazyBodyForProperty(property)) {
            val designation = CfirDesignation(data, property)
            calculateLazyBodyForProperty(designation)
        }

        return property
    }

    override fun transformErrorProperty(errorProperty: CfirErrorProperty, data: PersistentList<CfirDeclaration>): CfirStatement {
        return transformProperty(errorProperty, data)
    }

    override fun transformEnumEntry(enumEntry: CfirEnumEntry, data: PersistentList<CfirDeclaration>): CfirStatement {
        if (enumEntry.initializer is CfirLazyExpression) {
            val designation = CfirDesignation(data, enumEntry)
            calculateLazyInitializerForEnumEntry(designation)
        }

        return enumEntry
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: CfirAnonymousInitializer,
        data: PersistentList<CfirDeclaration>,
    ): CfirAnonymousInitializer {
        if (anonymousInitializer.body is CfirLazyBlock) {
            val designation = CfirDesignation(data, anonymousInitializer)
            calculateLazyBodyForAnonymousInitializer(designation)
        }

        return anonymousInitializer
    }

    override fun transformCodeFragment(codeFragment: CfirCodeFragment, data: PersistentList<CfirDeclaration>): CfirCodeFragment {
        if (codeFragment.block is CfirLazyBlock) {
            val designation = CfirDesignation(data, codeFragment)
            calculateLazyBodyForCodeFragment(designation)
        }

        return codeFragment
    }
}

private fun <E : CfirElement> CfirTransformer<PersistentList<CfirDeclaration>>.recursiveTransformation(
    element: E,
    data: PersistentList<CfirDeclaration>,
): E {
    if (element is CfirFile || element is CfirRegularClass) {
        val newList = data.add(element as CfirDeclaration)
        element.transformChildren(this, newList)
    }

    return element
}
