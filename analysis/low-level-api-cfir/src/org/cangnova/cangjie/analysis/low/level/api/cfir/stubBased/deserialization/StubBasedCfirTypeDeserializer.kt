/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.CjRealPsiSourceElement
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.computeTypeAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.builder.CfirTypeParameterBuilder
import org.cangnova.cangjie.cfir.declarations.utils.addDefaultBoundIfNecessary
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotation
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationArgumentMapping
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.ConeClassLikeTypeImpl
import org.cangnova.cangjie.cfir.types.impl.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.utils.exceptions.withConeTypeEntry
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.impl.*
import org.cangnova.cangjie.types.ConstantValueKind
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.utils.addToStdlib.runIf
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

internal class StubBasedCfirTypeDeserializer(
    private val moduleData: CfirModuleData,
    private val annotationDeserializer: StubBasedAnnotationDeserializer,
    private val parent: StubBasedCfirTypeDeserializer?,
    private val containingSymbol: CfirBasedSymbol<*>?,
    owner: CjTypeParameterListOwner?,
    initialOrigin: CfirDeclarationOrigin
) {
    private val typeParametersByName: Map<String, CfirTypeParameterSymbol>

    val ownTypeParameters: List<CfirTypeParameterSymbol>
        get() = typeParametersByName.values.toList()

    init {
        val typeParameters = owner?.typeParameters
        if (!typeParameters.isNullOrEmpty()) {
            typeParametersByName = mutableMapOf()
            val builders = mutableListOf<CfirTypeParameterBuilder>()
            for (typeParameter in typeParameters) {
                val name = typeParameter.nameAsSafeName
                val symbol = CfirTypeParameterSymbol().also {
                    typeParametersByName[name.asString()] = it
                }

                builders += CfirTypeParameterBuilder().apply {
                    source = CjRealPsiSourceElement(typeParameter)
                    moduleData = this@StubBasedCfirTypeDeserializer.moduleData
                    resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = initialOrigin
                    this.name = name
                    this.symbol = symbol
                    this.containingDeclarationSymbol = containingSymbol ?: errorWithAttachment("Top-level type parameter ???") {
                        withPsiEntry("owner", owner)
                        withPsiEntry("parameter", typeParameter)
                    }
                    annotations += annotationDeserializer.loadAnnotations(
                        ktAnnotated = typeParameter,
                        useSiteTargetFilter = StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
                    )
                }
            }

            for ((index, typeParameter) in typeParameters.withIndex()) {
                val builder = builders[index]
                builder.apply {
                    typeParameter.extendsBound?.let { bounds.add(typeRef(it)) }
                    owner.typeConstraints
                        .filter { it.subjectTypeParameterName?.getReferencedNameAsName() == typeParameter.nameAsName }
                        .forEach { typeConstraint -> typeConstraint.boundTypeReference?.let { bounds += typeRef(it) } }
                    addDefaultBoundIfNecessary()
                }.build()
            }
        } else {
            typeParametersByName = emptyMap()
        }
    }

    fun typeRef(typeReference: CjTypeReference): CfirTypeRef = buildResolvedTypeRef {
        source = CjRealPsiSourceElement(typeReference)
        annotations += annotationDeserializer.loadAnnotations(
            ktAnnotated = typeReference,
            useSiteTargetFilter = StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
        )

        coneType = type(typeReference, annotations.computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false))
    }

    fun type(typeReference: CjTypeReference): ConeKotlinType {
        val annotations = annotationDeserializer.loadAnnotations(
            typeReference,
            StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
        ).toMutableList()

        val parentStub = typeReference.compiledStub.parentStub
        if (parentStub is KotlinParameterStubImpl) {
            parentStub.functionTypeParameterName?.let { paramName ->
                annotations += buildAnnotation {
                    annotationTypeRef = buildResolvedTypeRef {
                        coneType = StandardNames.FqNames.parameterNameClassId.toLookupTag()
                            .constructClassType()
                    }
                    this.argumentMapping = buildAnnotationArgumentMapping {
                        mapping[StandardNames.NAME] =
                            buildLiteralExpression(null, ConstantValueKind.String, paramName, setType = true)
                    }
                }
            }
        }
        return type(typeReference, annotations.computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false))
    }

    fun type(type: KotlinTypeBean): ConeKotlinType? {
        when (type) {
            is KotlinTypeParameterTypeBean -> {
                val lookupTag =
                    typeParametersByName[type.typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(type.typeParameterName)
                    ?: return null
                return ConeTypeParameterTypeImpl(lookupTag, isMarkedNullable = false)
            }
            is KotlinClassTypeBean -> {
                return deserializeClassType(type)
            }
            is KotlinFlexibleTypeBean -> {
                return type(type.lowerBound)
            }
        }
    }

    private val ConeKotlinType?.asRigidType: ConeRigidType
        get() = when (this) {
            is ConeRigidType -> this
            null, is ConeFlexibleType -> errorWithAttachment("Unexpected cone type ${this?.let { it::class.simpleName }}") {
                withConeTypeEntry("bound", this@asRigidType)
            }
        }

    private fun deserializeClassType(typeBean: KotlinClassTypeBean): ConeClassLikeType {
        val projections = typeBean.arguments.map { typeArgumentBean ->
            val argBean = typeArgumentBean.type!!
            val lowerBound = type(argBean)
                ?: errorWithAttachment("Broken type argument ${typeArgumentBean.type?.let { it::class }}") {
                    withEntry("type", typeArgumentBean.type) { it.toString() }
                }
            lowerBound.toTypeProjection(Variance.INVARIANT)
        }

        val abbreviatedTypeAttribute = typeBean.abbreviatedType?.let { AbbreviatedTypeAttribute(deserializeClassType(it)) }
        val attributes = ConeAttributes.create(listOfNotNull(abbreviatedTypeAttribute))

        return ConeClassLikeTypeImpl(
            typeBean.classId.toLookupTag(),
            projections.toTypedArray(),
            isMarkedNullable = false,
            attributes,
        )
    }

    private fun type(typeReference: CjTypeReference, attributes: ConeAttributes): ConeKotlinType {
        val typeElement = typeReference.typeElement
        return when (typeElement) {
            is CjFunctionType -> deserializeFunctionType(typeReference, typeElement, attributes)
            is CjUserType -> deserializeUserType(typeReference, typeElement, attributes)
            else -> simpleTypeOrError(typeReference, attributes)
        }
    }

    private fun deserializeFunctionType(typeReference: CjTypeReference, type: CjFunctionType, attributes: ConeAttributes): ConeKotlinType {
        val functionTypeStub: KotlinFunctionTypeStubImpl = type.compiledStub
        return simpleTypeOrError(typeReference, attributes.withAbbreviation(functionTypeStub.abbreviatedType))
    }

    private fun deserializeUserType(typeReference: CjTypeReference, type: CjUserType, attributes: ConeAttributes): ConeKotlinType {
        val userTypeStub: KotlinUserTypeStubImpl = type.compiledStub
        return simpleTypeOrError(typeReference, attributes.withAbbreviation(userTypeStub.abbreviatedType))
    }

    private fun ConeAttributes.withAbbreviation(abbreviatedType: KotlinClassTypeBean?): ConeAttributes {
        if (abbreviatedType == null) return this
        return add(AbbreviatedTypeAttribute(deserializeClassType(abbreviatedType)))
    }

    private fun typeParameterSymbol(typeParameterName: String): ConeTypeParameterLookupTag? =
        typeParametersByName[typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(typeParameterName)

    fun CfirClassLikeSymbol<*>.typeParameters(): List<CfirTypeParameterSymbol> =
        (fir as? CfirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol }.orEmpty()

    private fun simpleType(typeReference: CjTypeReference, attributes: ConeAttributes): ConeRigidType? {
        val constructor = typeSymbol(typeReference) ?: return null
        if (constructor is ConeTypeParameterLookupTag) {
            return ConeTypeParameterTypeImpl(constructor, isMarkedNullable = false, attributes)
        }
        if (constructor !is ConeClassLikeLookupTag) return null

        val typeElement = typeReference.typeElement
        val arguments = when (typeElement) {
            is CjUserType -> buildList {
                // The type for Outer<T>.Inner<S> needs to have type args <S, T>
                var current: CjUserType? = typeElement
                while (current != null) {
                    current.typeArguments.forEach { add(type(it.typeReference!!).toTypeProjection(Variance.INVARIANT)) }
                    current = current.qualifier
                }
            }.toTypedArray()
            is CjFunctionType -> buildList {
                typeElement.parameters.mapTo(this) { type(it.typeReference!!).toTypeProjection(Variance.INVARIANT) }
                add(type(typeElement.returnTypeReference!!).toTypeProjection(Variance.INVARIANT))
            }.toTypedArray()
            else -> errorWithAttachment("not supported ${typeElement?.let { it::class }}") {
                withPsiEntry("typeElement", typeElement)
            }
        }

        return ConeClassLikeTypeImpl(
            constructor,
            arguments,
            isMarkedNullable = false,
            attributes,
        )
    }

    private fun simpleTypeOrError(typeReference: CjTypeReference, attributes: ConeAttributes): ConeRigidType =
        simpleType(typeReference, attributes) ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))

    private fun typeSymbol(typeReference: CjTypeReference): ConeClassifierLookupTag? {
        val typeElement = typeReference.typeElement
        if (typeElement is CjFunctionType) {
            val arity = typeElement.totalParameterCount
            val functionClassId = when {
                /*
                 * Since 2.1 any `@Composable FunctionN` type is serialized to metadata as `ComposableFunctionN`, which is consistent with
                 * how composable functions are treated in sources (with compose plugin enabled). But there are old libraries compiled
                 * with 2.0 or less, which still have `@Composable FunctionN` types. To handle such libraries in the CLI compiler plugins
                 * are passed to the library session so they could be applied to deserialized classes.
                 * But it's impossible to do the same in the IDE, because there libraries don't know anything about source modules they
                 * will be used in. So to work around this issue this conversion for Composable functions is hardcoded
                 */
                typeReference.annotationEntries.any {
                    StubBasedAnnotationDeserializer.getAnnotationClassId(it) == composableClassId
                } -> getComposableFunctionClassId(arity)

                else -> StandardNames.getFunctionClassId(arity)
            }
            return functionClassId.toLookupTag()
        }
        if (typeElement is CjIntersectionType) {
            val leftTypeRef = typeElement.getLeftTypeRef() ?: return null
            //T&Any
            return typeSymbol(leftTypeRef)
        }
        val type = typeElement as CjUserType
        val referencedName = type.referencedName
        return runIf(type.qualifier == null) {
            // Things like Foo.T can never be resolved to type parameter T
            typeParameterSymbol(referencedName!!)
        } ?: type.classId().toLookupTag()
    }

    private fun getComposableFunctionClassId(arity: Int): ClassId {
        val name = Name.identifier("$composableFunctionPrefix$arity")
        return ClassId(internalComposePackageFqName, name)
    }

    companion object {
        private val internalComposePackageFqName = FqName("androidx.compose.runtime.internal")
        private val composableClassId = ClassId(
            FqName("androidx.compose.runtime"),
            Name.identifier("Composable"),
        )
        private val composableFunctionPrefix = "ComposableFunction"
    }
}

/**
 * Retrieves classId from [CjUserType] for compiled code only.
 *
 * It relies on [org.cangnova.cangjie.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl.isClassRef],
 * which is set during cls analysis only.
 */
internal fun CjUserType.classId(): ClassId {
    val packageFragments = mutableListOf<String>()
    val classFragments = mutableListOf<String>()

    fun collectFragments(type: CjUserType) {
        type.qualifier?.let(::collectFragments)

        val referenceExpression = type.referenceExpression as? CjNameReferenceExpression
        if (referenceExpression != null) {
            val referencedName = referenceExpression.getReferencedName()
            val referenceExpressionStub: KotlinNameReferenceExpressionStubImpl = referenceExpression.compiledStub
            if (referenceExpressionStub.isClassRef) {
                classFragments.add(referencedName)
            } else {
                packageFragments.add(referencedName)
            }
        }
    }
    collectFragments(this)
    return ClassId(
        FqName.fromSegments(packageFragments),
        FqName.fromSegments(classFragments),
        isLocal = false
    )
}
