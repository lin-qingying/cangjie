/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.builder.CfirTypeParameterBuilder
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.withConeTypeEntry
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl
import org.cangnova.cangjie.source.CjRealPsiSourceElement
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
    private val psiFactory: CjPsiFactory? = owner?.project?.let { CjPsiFactory(it) }
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
                        annotated = typeParameter,
                        containingDeclarationSymbol = this.containingDeclarationSymbol,
                        useSiteTargetFilter = StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
                    )
                }
            }

            for ((index, typeParameter) in typeParameters.withIndex()) {
                val builder = builders[index]
                builder.apply {
                    typeParameter.extendsBound?.let { bounds.add(typeRef(it)) }
                    owner.typeConstraints
                        .filter { it.subjectTypeParameterName?.referencedNameAsName == typeParameter.nameAsName }
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
            annotated = typeReference,
            containingDeclarationSymbol = containingSymbol,
            useSiteTargetFilter = StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
        )

        coneType = type(typeReference, ConeAttributes.Empty)
    }

    fun type(typeReference: CjTypeReference): ConeCangJieType {
        val annotations = annotationDeserializer.loadAnnotations(
            annotated = typeReference,
            containingDeclarationSymbol = containingSymbol,
            useSiteTargetFilter = StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
        )
        return type(typeReference, if (annotations.isEmpty()) ConeAttributes.Empty else ConeAttributes.Empty)
    }

    private val ConeCangJieType?.asRigidType: ConeRigidType
        get() = when (this) {
            is ConeRigidType -> this
            null -> errorWithAttachment("Unexpected cone type ${this?.let { it::class.simpleName }}") {
                withConeTypeEntry("bound", this@asRigidType)
            }
        }

    private fun type(typeReference: CjTypeReference, attributes: ConeAttributes): ConeCangJieType {
        val typeElement = typeReference.typeElement
        return when (typeElement) {
            is CjFunctionType -> deserializeFunctionType(typeReference, typeElement, attributes)
            is CjOptionType -> deserializeOptionType(typeReference, typeElement, attributes)
            is CjUserType -> deserializeUserType(typeReference, typeElement, attributes)
            else -> simpleTypeOrError(typeReference, attributes)
        }
    }

    private fun deserializeFunctionType(typeReference: CjTypeReference, type: CjFunctionType, attributes: ConeAttributes): ConeCangJieType {
        return simpleTypeOrError(typeReference, attributes)
    }

    private fun deserializeUserType(typeReference: CjTypeReference, type: CjUserType, attributes: ConeAttributes): ConeCangJieType {
        return simpleTypeOrError(typeReference, attributes)
    }

    private fun deserializeOptionType(
        typeReference: CjTypeReference,
        type: CjOptionType,
        attributes: ConeAttributes,
    ): ConeCangJieType {
        val innerTypeElement = type.getInnerType()
            ?: return ConeErrorType(ConeSimpleDiagnostic("Malformed option type", DiagnosticKind.DeserializationError))
        val innerTypeReference = psiFactory?.createTypeIfPossible(innerTypeElement.text)
            ?: return ConeErrorType(ConeSimpleDiagnostic("Malformed option type", DiagnosticKind.DeserializationError))
        val componentType = type(innerTypeReference, attributes)
        return ConeClassLikeType(
            StdlibClassIds.Option.toLookupTag(),
            typeArguments = listOf(componentType),
            attributes = attributes,
        )
    }

    private fun typeParameterSymbol(typeParameterName: String): ConeTypeParameterLookupTag? =
        typeParametersByName[typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(typeParameterName)

    fun CfirClassLikeSymbol<*>.typeParameters(): List<CfirTypeParameterSymbol> =
        (cfir as? CfirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol }.orEmpty()

    private fun simpleType(typeReference: CjTypeReference, attributes: ConeAttributes): ConeRigidType? {
        val constructor = typeSymbol(typeReference) ?: return null
        if (constructor is ConeTypeParameterLookupTag) {
            return ConeTypeParameterTypeImpl(constructor, attributes)
        }
        if (constructor !is ConeClassLikeLookupTag) return null

        val typeElement = typeReference.typeElement
        val arguments: List<ConeTypeProjection> = when (typeElement) {
            is CjUserType -> buildList<ConeTypeProjection> {
                var current: CjUserType? = typeElement
                while (current != null) {
                    current.typeArguments.forEach { projection ->
                        projection.typeReference?.let { add(type(it)) }
                    }
                    current = current.qualifier
                }
            }
            is CjFunctionType -> buildList<ConeTypeProjection> {
                typeElement.parameters.mapTo(this) { parameter ->
                    type(parameter.typeReference ?: errorWithAttachment("Function type parameter lacks type reference") {
                        withPsiEntry("parameter", parameter)
                    })
                }
                val returnTypeReference = typeElement.returnTypeReference
                    ?: errorWithAttachment("Function type lacks return type reference") {
                        withPsiEntry("typeReference", typeReference)
                    }
                add(type(returnTypeReference))
            }
            else -> errorWithAttachment("not supported ${typeElement?.let { it::class }}") {
                withPsiEntry("typeElement", typeElement)
            }
        }

        return ConeClassLikeType(constructor, arguments, attributes)
    }

    private fun simpleTypeOrError(typeReference: CjTypeReference, attributes: ConeAttributes): ConeRigidType =
        simpleType(typeReference, attributes) ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))

    private fun typeSymbol(typeReference: CjTypeReference): ConeClassifierLookupTag? {
        val typeElement = typeReference.typeElement
        if (typeElement is CjFunctionType) {
            val arity = typeElement.parameters.size
            val functionClassId = StandardNames.getFunctionClassId(arity)
            return functionClassId.toLookupTag()
        }
        val type = typeElement as? CjUserType ?: return null
        val referencedName = type.referencedName ?: return null
        if (type.qualifier == null) {
            typeParameterSymbol(referencedName)?.let { return it }
        }
        return type.classId().toLookupTag()
    }
}

/**
 * 对齐 Kotlin `FirTypeParameterBuilder.addDefaultBoundIfNecessary` 的职责：
 * 当反序列化出的类型参数没有显式上界时，为其补上仓颉主干的默认顶层约束 `std.core.Any`。
 */
private fun CfirTypeParameterBuilder.addDefaultBoundIfNecessary() {
    if (bounds.isNotEmpty()) return

    val defaultBound = ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true)
    bounds += defaultBound.toCfirResolvedTypeRef()
}

/**
 * Retrieves classId from [CjUserType] for compiled code only.
 *
 * It relies on [org.cangnova.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl.isClassRef],
 * which is set during cls analysis only.
 */
internal fun CjUserType.classId(): ClassId {
    val packageFragments = mutableListOf<String>()
    val classFragments = mutableListOf<String>()

    fun collectFragments(type: CjUserType) {
        type.qualifier?.let(::collectFragments)

        val referenceExpression = type.referenceExpression as? CjNameReferenceExpression
        if (referenceExpression != null) {
            val referencedName = referenceExpression.referencedName
            val referenceExpressionStub: CangJieNameReferenceExpressionStubImpl = referenceExpression.compiledStub
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
    )
}
