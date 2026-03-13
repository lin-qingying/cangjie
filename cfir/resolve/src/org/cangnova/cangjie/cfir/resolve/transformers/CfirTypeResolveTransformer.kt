/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.KtFakeSourceElementKind
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.config.AnalysisFlags
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget.*
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.fromPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.utils.isFromVararg
import org.cangnova.cangjie.cfir.declarations.utils.isInner
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCallCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildExpressionStub
import org.cangnova.cangjie.cfir.extensions.extensionService
import org.cangnova.cangjie.cfir.extensions.replSnippetResolveExtensions
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.diagnostics.ConeAmbiguouslyResolvedAnnotationFromPlugin
import org.cangnova.cangjie.cfir.resolve.diagnostics.ConeCyclicTypeBound
import org.cangnova.cangjie.cfir.resolve.lookupSuperTypes
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.createImportingScopes
import org.cangnova.cangjie.cfir.scopes.getNestedClassifierScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirMemberTypeParameterScope
import org.cangnova.cangjie.cfir.scopes.impl.nestedClassifierScope
import org.cangnova.cangjie.cfir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.StandardClassIds
import org.cangnova.cangjie.util.PrivateForInline
import org.cangnova.cangjie.utils.addToStdlib.shouldNotBeCalled

class CfirTypeResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession
) : CfirTransformerBasedResolveProcessor(session, scopeSession, CfirResolvePhase.TYPES) {
    override val transformer: CfirTypeResolveTransformer = CfirTypeResolveTransformer(session, scopeSession)
}

fun <F : CfirClassLikeDeclaration> F.runTypeResolvePhaseForLocalClass(
    session: CfirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<CfirScope>,
    useSiteFile: CfirFile,
    containingDeclarations: List<CfirDeclaration>,
): F {
    val transformer = CfirTypeResolveTransformer(
        session,
        scopeSession,
        currentScopeList,
        initialCurrentFile = useSiteFile,
        classDeclarationsStack = containingDeclarations.filterIsInstanceTo(ArrayDeque())
    )

    return this.transform(transformer, null)
}

@OptIn(PrivateForInline::class)
open class CfirTypeResolveTransformer(
    final override val session: CfirSession,
    @property:PrivateForInline val scopeSession: ScopeSession,
    initialScopes: List<CfirScope> = emptyList(),
    initialCurrentFile: CfirFile? = null,
    @property:PrivateForInline val classDeclarationsStack: ArrayDeque<CfirClass> = ArrayDeque()
) : CfirAbstractTreeTransformer<Any?>(CfirResolvePhase.TYPES) {
    /**
     * All current scopes sorted from outermost to innermost.
     */
    @PrivateForInline
    var scopes: PersistentList<CfirScope> = initialScopes.asReversed().toPersistentList()

    /**
     * Scopes that are accessible statically, i.e. [scopes] minus type parameter scopes.
     */
    @PrivateForInline
    var staticScopes: PersistentList<CfirScope> = scopes

    @set:PrivateForInline
    var scopesBefore: PersistentList<CfirScope>? = null

    @set:PrivateForInline
    var staticScopesBefore: PersistentList<CfirScope>? = null

    private var currentDeclaration: CfirDeclaration? = null

    private inline fun <T> withDeclaration(declaration: CfirDeclaration, crossinline action: () -> T): T {
        val oldDeclaration = currentDeclaration
        return try {
            currentDeclaration = declaration
            action()
        } finally {
            currentDeclaration = oldDeclaration
        }
    }

    private val typeResolverTransformer: CfirSpecificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session, expandTypeAliases = true)

    @PrivateForInline
    var currentFile: CfirFile? = initialCurrentFile

    override fun transformFile(file: CfirFile, data: Any?): CfirFile {
        checkSessionConsistency(file)
        return withFileScope(file) {
            super.transformFile(file, data)
        }
    }

    inline fun <R> withFileScope(file: CfirFile, crossinline action: () -> R): R {
        currentFile = file
        return withScopeCleanup {
            addScopes(createImportingScopes(file, session, scopeSession))
            action()
        }
    }

    override fun transformReplSnippet(replSnippet: CfirReplSnippet, data: Any?): CfirReplSnippet {
        whileAnalysing(session, replSnippet) {
            return withReplSnippetScope(replSnippet) {
                transformElement(replSnippet, data)
            }
        }
    }

    inline fun <R> withReplSnippetScope(replSnippet: CfirReplSnippet, crossinline action: () -> R): R {
        return withScopeCleanup {
            addScopes(buildList {
                // TODO: robuster matching and error reporting on no extension (KT-72969)
                for (resolveExt in session.extensionService.replSnippetResolveExtensions) {
                    val scope = resolveExt.getSnippetScope(replSnippet, session)
                    if (scope != null) add(scope)
                }
            })
            action()
        }
    }

    override fun transformRegularClass(regularClass: CfirRegularClass, data: Any?): CfirStatement {
        whileAnalysing(session, regularClass) {
            withClassDeclarationCleanup(regularClass) {
                transformClassTypeParameters(regularClass, data)
                return resolveClassContent(regularClass, data)
            }
        }
    }

    fun transformClassTypeParameters(regularClass: CfirRegularClass, data: Any?) {
        withScopeCleanup {
            // Remove type parameter scopes for classes that are neither inner nor local
            if (removeOuterTypeParameterScope(regularClass)) {
                this.scopes = staticScopes
            }
            addTypeParametersScope(regularClass)
            regularClass.typeParameters.forEach {
                it.accept(this, data)
            }
            unboundCyclesInTypeParametersSupertypes(regularClass)
        }
    }

    inline fun <R> withClassDeclarationCleanup(regularClass: CfirRegularClass, action: () -> R): R {
        return withClassDeclarationCleanup(classDeclarationsStack, regularClass, action)
    }

    override fun transformAnonymousObject(anonymousObject: CfirAnonymousObject, data: Any?): CfirStatement {
        withClassDeclarationCleanup(classDeclarationsStack, anonymousObject) {
            return resolveClassContent(anonymousObject, data)
        }
    }

    override fun transformConstructor(constructor: CfirConstructor, data: Any?): CfirConstructor = whileAnalysing(session, constructor) {
        return withScopeCleanup {
            addTypeParametersScope(constructor)
            val result = transformDeclaration(constructor, data) as CfirConstructor

            if (result.isPrimary) {
                val shouldAddDefaultStubs = session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) &&
                        session.moduleData.isCommon &&
                        constructor.returnTypeRef.coneType.classId == StandardClassIds.Enum
                for (valueParameter in result.valueParameters) {
                    if (valueParameter.correspondingProperty != null) {
                        valueParameter.moveOrDeleteIrrelevantAnnotations()
                    }
                    if (shouldAddDefaultStubs) {
                        valueParameter.replaceDefaultValue(buildExpressionStub()) // TODO: Remove when KT-67381 is implemented
                    }
                }
            }

            result
        }
    }

    override fun transformAnonymousInitializer(anonymousInitializer: CfirAnonymousInitializer, data: Any?): CfirAnonymousInitializer {
        return withScopeCleanup {
            transformDeclaration(anonymousInitializer, data) as CfirAnonymousInitializer
        }
    }

    override fun transformErrorPrimaryConstructor(errorPrimaryConstructor: CfirErrorPrimaryConstructor, data: Any?): CfirConstructor =
        transformConstructor(errorPrimaryConstructor, data)

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Any?): CfirTypeAlias = whileAnalysing(session, typeAlias) {
        withScopeCleanup {
            addTypeParametersScope(typeAlias)
            transformDeclaration(typeAlias, data)
        } as CfirTypeAlias
    }

    override fun transformEnumEntry(enumEntry: CfirEnumEntry, data: Any?): CfirEnumEntry = whileAnalysing(session, enumEntry) {
        enumEntry.transformReturnTypeRef(this, data)
        enumEntry.transformTypeParameters(this, data)
        enumEntry.transformAnnotations(this, data)
        enumEntry
    }

    override fun transformReceiverParameter(receiverParameter: CfirReceiverParameter, data: Any?): CfirReceiverParameter {
        return receiverParameter.transformAnnotations(this, data).transformTypeRef(this, data)
    }

    override fun transformProperty(property: CfirProperty, data: Any?): CfirProperty = whileAnalysing(session, property) {
        withScopeCleanup {
            withDeclaration(property) {
                addTypeParametersScope(property)
                property.transformTypeParameters(this, data)
                    .transformReturnTypeRef(this, data)
                    .transformReceiverParameter(this, data)
                    .transformContextParameters(this, data)
                    .transformGetter(this, data)
                    .transformSetter(this, data)
                    .transformBackingField(this, data)
                    .transformAnnotations(this, data)

                if (property.isFromVararg == true) {
                    property.transformTypeToArrayType(session)
                    property.backingField?.transformTypeToArrayType(session)
                    setAccessorTypesByPropertyType(property)
                }

                when {
                    property.returnTypeRef is CfirResolvedTypeRef && property.delegate != null -> {
                        setAccessorTypesByPropertyType(property)
                    }
                    property.returnTypeRef !is CfirResolvedTypeRef && property.initializer == null &&
                            property.getter?.returnTypeRef is CfirResolvedTypeRef -> {
                        val returnTypeRef = property.getter!!.returnTypeRef

                        property.replaceReturnTypeRef(returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyTypeFromGetterReturnType))
                        property.backingField?.replaceReturnTypeRef(
                            returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyTypeFromGetterReturnType)
                        )

                        property.setter?.valueParameters?.forEach {
                            it.replaceReturnTypeRef(
                                returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyTypeFromGetterReturnType)
                            )
                        }
                    }
                }

                unboundCyclesInTypeParametersSupertypes(property)

                property.moveOrDeleteIrrelevantAnnotations()
                property
            }
        }
    }

    private fun setAccessorTypesByPropertyType(property: CfirProperty) {
        property.getter?.replaceReturnTypeRef(property.returnTypeRef)
        property.setter?.valueParameters?.map { it.replaceReturnTypeRef(property.returnTypeRef) }
    }

    override fun transformField(field: CfirField, data: Any?): CfirField = whileAnalysing(session, field) {
        withScopeCleanup {
            field.transformReturnTypeRef(this, data).transformAnnotations(this, data)
            field
        }
    }

    override fun transformBackingField(backingField: CfirBackingField, data: Any?): CfirStatement = whileAnalysing(session, backingField) {
        backingField.transformAnnotations(this, data)
        super.transformBackingField(backingField, data)
    }

    override fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        data: Any?,
    ): CfirNamedFunction = whileAnalysing(session, namedFunction) {
        withScopeCleanup {
            withDeclaration(namedFunction) {
                addTypeParametersScope(namedFunction)
                val result = transformDeclaration(namedFunction, data).also {
                    unboundCyclesInTypeParametersSupertypes(it as CfirTypeParametersOwner)
                }

                if (result.source?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers &&
                    result is CfirNamedFunction &&
                    result.name == StandardNames.DATA_CLASS_COPY
                ) {
                    for (valueParameter in result.valueParameters) {
                        valueParameter.moveOrDeleteIrrelevantAnnotations()
                    }
                }

                result
            }
        } as CfirNamedFunction
    }

    private fun unboundCyclesInTypeParametersSupertypes(typeParametersOwner: CfirTypeParameterRefsOwner) {
        for (typeParameter in typeParametersOwner.typeParameters) {
            if (typeParameter !is CfirTypeParameter) continue
            if (hasSupertypePathToParameter(typeParameter, typeParameter, mutableSetOf())) {
                val errorType = buildErrorTypeRef {
                    diagnostic = ConeCyclicTypeBound(typeParameter.symbol, typeParameter.bounds.toImmutableList())
                    source = typeParameter.bounds.first().source
                }
                typeParameter.replaceBounds(
                    listOf(errorType)
                )
            }
        }
    }

    private fun hasSupertypePathToParameter(
        currentTypeParameter: CfirTypeParameter,
        typeParameter: CfirTypeParameter,
        visited: MutableSet<CfirTypeParameter>
    ): Boolean {
        if (visited.isNotEmpty() && currentTypeParameter == typeParameter) return true
        if (!visited.add(currentTypeParameter)) return false

        fun ConeKotlinType.toNextTypeParameter(): CfirTypeParameter? = when (this) {
            is ConeTypeParameterType -> lookupTag.typeParameterSymbol.fir
            is ConeDefinitelyNotNullType -> original.toNextTypeParameter()
            else -> null
        }

        return currentTypeParameter.bounds.any {
            val nextTypeParameter = it.coneTypeOrNull?.toNextTypeParameter() ?: return@any false

            hasSupertypePathToParameter(nextTypeParameter, typeParameter, visited)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: Any?): CfirTypeRef {
        return implicitTypeRef
    }

    override fun transformTypeRef(typeRef: CfirTypeRef, data: Any?): CfirResolvedTypeRef {
        return typeRef.transform(
            typeResolverTransformer,
            TypeResolutionConfiguration(scopes.asReversed(), classDeclarationsStack, currentFile)
        )
    }

    override fun transformValueParameter(
        valueParameter: CfirValueParameter,
        data: Any?,
    ): CfirStatement = whileAnalysing(session, valueParameter) {
        withDeclaration(valueParameter) {
            valueParameter.transformReturnTypeRef(this, data)
            valueParameter.transformAnnotations(this, data)
            valueParameter.transformVarargTypeToArrayType(session)
            valueParameter
        }
    }

    override fun transformBlock(block: CfirBlock, data: Any?): CfirStatement {
        return block
    }

    override fun transformArgumentList(argumentList: CfirArgumentList, data: Any?): CfirArgumentList {
        return argumentList
    }

    override fun transformAnnotation(annotation: CfirAnnotation, data: Any?): CfirStatement {
        shouldNotBeCalled()
    }

    override fun transformAnnotationCall(
        annotationCall: CfirAnnotationCall,
        data: Any?
    ): CfirStatement = whileAnalysing(session, annotationCall) {
        when (val originalTypeRef = annotationCall.annotationTypeRef) {
            is CfirResolvedTypeRef -> {
                when (annotationCall.annotationResolvePhase) {
                    CfirAnnotationResolvePhase.Unresolved -> when (originalTypeRef) {
                        is CfirErrorTypeRef -> return annotationCall.also { it.replaceAnnotationResolvePhase(CfirAnnotationResolvePhase.Types) }
                        else -> shouldNotBeCalled()
                    }
                    CfirAnnotationResolvePhase.CompilerRequiredAnnotations -> {
                        annotationCall.transformTypeArguments(this, data)
                        annotationCall.replaceAnnotationResolvePhase(CfirAnnotationResolvePhase.Types)
                        val alternativeResolvedTypeRef =
                            originalTypeRef.delegatedTypeRef?.transformSingle(this, data) ?: return annotationCall
                        val coneTypeFromCompilerRequiredPhase = originalTypeRef.coneType
                        val coneTypeFromTypesPhase = alternativeResolvedTypeRef.coneType
                        if (coneTypeFromTypesPhase != coneTypeFromCompilerRequiredPhase) {
                            val errorTypeRef = buildErrorTypeRef {
                                source = originalTypeRef.source
                                coneType = coneTypeFromCompilerRequiredPhase
                                annotations += originalTypeRef.annotations
                                delegatedTypeRef = originalTypeRef.delegatedTypeRef
                                diagnostic = ConeAmbiguouslyResolvedAnnotationFromPlugin(
                                    coneTypeFromCompilerRequiredPhase,
                                    coneTypeFromTypesPhase
                                )
                            }
                            annotationCall.replaceAnnotationTypeRef(errorTypeRef)
                        }
                    }
                    CfirAnnotationResolvePhase.Types -> {}
                }
            }
            else -> {
                val transformedTypeRef = originalTypeRef.transformSingle(this, data)
                annotationCall.transformTypeArguments(this, data)
                annotationCall.replaceAnnotationResolvePhase(CfirAnnotationResolvePhase.Types)
                annotationCall.replaceAnnotationTypeRef(transformedTypeRef)
            }
        }

        return annotationCall
    }

    inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val scopesBeforeSnapshot = scopes
        val scopesBeforeBeforeSnapshot = scopesBefore
        scopesBefore = scopesBeforeSnapshot

        val staticScopesBeforeSnapshot = staticScopes
        val staticScopesBeforeBeforeSnapshot = staticScopesBefore
        staticScopesBefore = staticScopesBeforeSnapshot

        return try {
            l()
        } finally {
            scopes = scopesBeforeSnapshot
            scopesBefore = scopesBeforeBeforeSnapshot
            staticScopes = staticScopesBeforeSnapshot
            staticScopesBefore = staticScopesBeforeBeforeSnapshot
        }
    }

    private fun resolveClassContent(
        firClass: CfirClass,
        data: Any?
    ): CfirStatement = withClassScopes(
        firClass,
        actionInsideStaticScope = {
            withScopeCleanup {
                firClass.transformAnnotations(this, null)

                if (firClass is CfirRegularClass) {
                    addTypeParametersScope(firClass)
                }

                // ConstructedTypeRef should be resolved only with type parameters, but not with nested classes and classes from supertypes
                for (declaration in firClass.declarations) {
                    when (declaration) {
                        is CfirConstructor -> transformDelegatedConstructorCall(declaration)
                        is CfirField -> {
                            if (declaration.origin == CfirDeclarationOrigin.Synthetic.DelegateField) {
                                transformDelegateField(declaration)
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    ) {
        // Note that annotations are still visited here
        // again, although there's no need in it
        transformElement(firClass, data)
    }

    fun transformDelegatedConstructorCall(constructor: CfirConstructor) {
        constructor.delegatedConstructor?.let(this::resolveConstructedTypeRefForDelegatedConstructorCall)
    }

    fun transformDelegateField(field: CfirField) {
        field.transformReturnTypeRef(this, null)
    }

    fun removeOuterTypeParameterScope(firClass: CfirClass): Boolean = !firClass.isInner && !firClass.isLocal

    /**
     * Changes to the order of scopes should also be reflected in
     * [org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext.withScopesForClass].
     * Otherwise, we get different behavior between type resolve and body resolve phases.
     */
    inline fun <R> withClassScopes(
        firClass: CfirClass,
        crossinline actionInsideStaticScope: () -> Unit = {},
        crossinline action: () -> R,
    ): R = withScopeCleanup {
        // Remove type parameter scopes for classes that are neither inner nor local
        if (removeOuterTypeParameterScope(firClass)) {
            this.scopes = staticScopes
        }

        actionInsideStaticScope()

        // ? Is it Ok to use original file session here ?
        val superTypes = lookupSuperTypes(
            firClass,
            lookupInterfaces = false,
            deep = true,
            substituteTypes = true,
            useSiteSession = session
        ).asReversed()

        val scopesToAdd = mutableListOf<CfirScope>()

        for (superType in superTypes) {
            superType.lookupTag.getNestedClassifierScope(session, scopeSession)?.let { nestedClassifierScope ->
                val scope = nestedClassifierScope.wrapNestedClassifierScopeWithSubstitutionForSuperType(superType, session)
                scopesToAdd.add(scope)
            }
        }

        if (firClass is CfirRegularClass) {
            // Companion scope is added before static scope,
            // i.e., static scope is checked first during resolution (scopes are in reverse order).
            // This is because we can qualify companion scope using `Companion.` if we want to explicitly refer to a declaration in the
            // companion.
            firClass.companionObjectSymbol?.fir
                ?.let(session::nestedClassifierScope)
                ?.let(scopesToAdd::add)

            session.nestedClassifierScope(firClass)?.let(scopesToAdd::add)

            addScopes(scopesToAdd)
            addTypeParametersScope(firClass)
        } else {
            session.nestedClassifierScope(firClass)?.let(scopesToAdd::add)
            addScopes(scopesToAdd)
        }

        action()
    }

    private fun resolveConstructedTypeRefForDelegatedConstructorCall(
        delegatedConstructorCall: CfirDelegatedConstructorCall
    ) {
        delegatedConstructorCall.replaceConstructedTypeRef(delegatedConstructorCall.constructedTypeRef.transformSingle(this, null))
        delegatedConstructorCall.transformCalleeReference(this, null)
    }

    fun addTypeParametersScope(firMemberDeclaration: CfirMemberDeclaration) {
        if (firMemberDeclaration.typeParameters.isNotEmpty()) {
            scopes = scopes.add(CfirMemberTypeParameterScope(firMemberDeclaration))
        }
    }

    fun addScopes(list: List<CfirScope>) {
        // small optimization to skip unnecessary allocations
        val scopesAreTheSame = scopes === staticScopes

        scopes = scopes.addAll(list)
        staticScopes = if (scopesAreTheSame) scopes else staticScopes.addAll(list)
    }

    /**
     * Filters annotations by target.
     * For example, in the following snippet the annotation may apply to the constructor value parameter, the property or the underlying field:
     * ```
     * class Foo(@Ann val x: String)
     * ```
     * This ambiguity may be resolved by specifying the use-site explicitly, i.e. `@field:Ann` or by analysing the allowed targets from
     * the [kotlin.annotation.Target] meta-annotation.
     * In latter case, the method will ensure that the annotation is moved to the correct element (field or parameter) or left at the property.
     */
    private fun CfirVariable.moveOrDeleteIrrelevantAnnotations() {
        if (annotations.isEmpty()) return
        val languageVersionSettings = session.languageVersionSettings
        replaceAnnotations(annotations.filter { annotation ->
            when (annotation.useSiteTarget) {
                null -> annotation.multiplexWithoutUseSiteTarget(this, languageVersionSettings)
                ALL -> annotation.multiplexWithAllUseSiteTarget(this, languageVersionSettings)
                else -> true
            }
        })
    }

    private fun CfirAnnotation.multiplexWithoutUseSiteTarget(
        annotated: CfirDeclaration,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        val allowedTargets = useSiteTargetsFromMetaAnnotation(session)
        return when (annotated) {
            // If parameter is allowed, we apply annotation to it in the first turn, independent of the targeting mode
            is CfirValueParameter -> {
                CONSTRUCTOR_PARAMETER in allowedTargets
            }
            is CfirProperty if annotated.fromPrimaryConstructor == true && CONSTRUCTOR_PARAMETER in allowedTargets -> {
                when {
                    !languageVersionSettings.supportsFeature(LanguageFeature.PropertyParamAnnotationDefaultTargetMode) -> {
                        false
                    }
                    // In the property-param mode,
                    // we should apply annotation also to the property (or to the field) if it's allowed
                    PROPERTY in allowedTargets -> true
                    annotated.backingField != null && propertyAnnotationShouldBeMovedToField(allowedTargets) -> {
                        if (classDeclarationsStack.lastOrNull()?.classKind != ClassKind.ANNOTATION_CLASS) {
                            val backingField = annotated.backingField!!
                            backingField.replaceAnnotations(backingField.annotations + this)
                        }
                        false
                    }
                    else -> false
                }
            }
            // Otherwise (for a regular property or for a constructor property if annotation isn't applicable to parameter),
            // we simply choose between a property and a field
            is CfirProperty if annotated.backingField != null && propertyAnnotationShouldBeMovedToField(allowedTargets) -> {
                val backingField = annotated.backingField!!
                backingField.replaceAnnotations(backingField.annotations + this)
                false
            }
            // Here we can come with a regular (non-constructor) property without a backing field,
            // or with some other non-parameter variable
            else -> {
                true
            }
        }
    }

    private fun CfirAnnotation.multiplexWithAllUseSiteTarget(
        annotated: CfirDeclaration,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.AnnotationAllUseSiteTarget)) {
            return true
        }
        val allowedTargets = useSiteTargetsFromMetaAnnotation(session)
        return when (annotated) {
            is CfirValueParameter -> {
                CONSTRUCTOR_PARAMETER in allowedTargets
            }
            is CfirProperty -> {
                var addedSomewhere = false

                fun CfirCallableDeclaration.addAnnotationWithoutUseSiteTarget(annotation: CfirAnnotation) {
                    val copy = if (annotation is CfirAnnotationCall) {
                        buildAnnotationCallCopy(annotation) {
                            useSiteTarget = null
                        }
                    } else {
                        buildAnnotationCopy(annotation) {
                            useSiteTarget = null
                        }
                    }
                    replaceAnnotations(annotations + copy)
                    addedSomewhere = true
                }

                if (FIELD in allowedTargets && annotated.delegate == null) {
                    annotated.backingField?.addAnnotationWithoutUseSiteTarget(this)
                }
                if (PROPERTY_GETTER in allowedTargets) {
                    annotated.getter?.addAnnotationWithoutUseSiteTarget(this)
                }
                if (annotated.isVar && SETTER_PARAMETER in allowedTargets) {
                    annotated.setter?.valueParameters?.firstOrNull()?.addAnnotationWithoutUseSiteTarget(this)
                }
                if (CONSTRUCTOR_PARAMETER in allowedTargets && annotated.fromPrimaryConstructor == true) {
                    // It's already on a constructor parameter, but we set the flag to prevent reporting an error
                    addedSomewhere = true
                }
                // If annotation isn't applicable anywhere or the property is delegated, we keep it at property to report an error later
                PROPERTY in allowedTargets || !addedSomewhere || annotated.delegate != null
            }
            else -> {
                true
            }
        }
    }

    /**
     * @param allowedTargets allowed use-site targets of a given property annotation
     * @return true if the given annotation on a property (initially placed there during raw FIR building)
     * is in fact inapplicable to properties, but applicable to fields.
     */
    private fun propertyAnnotationShouldBeMovedToField(allowedTargets: Set<AnnotationUseSiteTarget>): Boolean =
        (FIELD in allowedTargets || PROPERTY_DELEGATE_FIELD in allowedTargets) && PROPERTY !in allowedTargets
}
