package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CfirExtendIndexStore : CfirSessionComponent {
    private var models: List<CfirExtendSemanticModel> = emptyList()
    private var modelsByTargetClassId: Map<ClassId, List<CfirExtendSemanticModel>> = emptyMap()
    private var modelsByPackage: Map<FqName, List<CfirExtendSemanticModel>> = emptyMap()
    private var modelsByOrigin: Map<CfirExtendSemanticOrigin, List<CfirExtendSemanticModel>> = emptyMap()
    private var modelByDeclaration: Map<CfirExtend, CfirExtendSemanticModel> = emptyMap()
    private var defaultIndependentMembersByInterface: Map<ClassId, List<Name>> = emptyMap()

    @Synchronized
    fun rebuild(files: List<CfirFile>, resolver: CfirTypeResolver) {
        val collected = buildList {
            for (file in files) {
                for ((declarationIndex, declaration) in file.declarations.withIndex()) {
                    if (declaration !is CfirExtend) continue
                    add(declaration.toSemanticModel(file, declarationIndex, resolver))
                }
            }
        }
        val next = collected.sortedWith(semanticModelComparator)

        models = next
        modelByDeclaration = next.associateBy { it.declaration }
        modelsByTargetClassId = next.filter { it.targetClassId != null }.groupBy { it.targetClassId!! }
        modelsByPackage = next.groupBy { it.packageFqName }
        modelsByOrigin = next.groupBy { it.origin }
        defaultIndependentMembersByInterface = buildDefaultIndependentMembersMap(next, resolver)
    }

    fun allModels(): List<CfirExtendSemanticModel> = models

    fun modelForDeclaration(declaration: Any): CfirExtendSemanticModel? = modelByDeclaration[declaration]

    fun modelsForClass(classId: ClassId): List<CfirExtendSemanticModel> = modelsByTargetClassId[classId].orEmpty()

    fun modelsInPackage(packageFqName: FqName): List<CfirExtendSemanticModel> = modelsByPackage[packageFqName].orEmpty()

    fun modelsByOrigin(origin: CfirExtendSemanticOrigin): List<CfirExtendSemanticModel> = modelsByOrigin[origin].orEmpty()

    fun defaultIndependentMembersOfInterface(interfaceClassId: ClassId): List<Name> =
        defaultIndependentMembersByInterface[interfaceClassId].orEmpty()

    private fun CfirExtend.toSemanticModel(
        file: CfirFile,
        declarationIndexInFile: Int,
        resolver: CfirTypeResolver,
    ): CfirExtendSemanticModel {
        val semanticNormalizer = CfirExtendTypeSemanticNormalizer(this)
        val targetClass = resolver.resolveClass(extendedTypeRef)
        val inheritedInterfaces = superTypeRefs.map { superTypeRef ->
            CfirExtendInheritedInterfaceSemantic(
                classId = superTypeRef.toClassIdOrNull(),
                semanticKey = semanticNormalizer.semanticKeyOrNull(superTypeRef) ?: superTypeRef.toString(),
            )
        }
        val inheritedInterfaceClassIds = inheritedInterfaces.mapNotNull { it.classId }
        val inheritedInterfaceSemanticKeys = inheritedInterfaces.map { it.semanticKey }
        return CfirExtendSemanticModel(
            declaration = this,
            packageFqName = file.packageDirective.packageFqName,
            fileName = file.name,
            declarationIndexInFile = declarationIndexInFile,
            targetClassId = extendedTypeRef.toClassIdOrNull(),
            targetClassKind = targetClass?.classKindOrNull(),
            inheritedInterfaces = inheritedInterfaces,
            inheritedInterfaceClassIds = inheritedInterfaceClassIds,
            inheritedInterfaceSemanticKeys = inheritedInterfaceSemanticKeys,
            origin = origin.toExtendSemanticOrigin(),
        )
    }

    private val semanticModelComparator = compareBy<CfirExtendSemanticModel>(
        { it.packageFqName.asString() },
        { it.fileName },
        { it.declarationIndexInFile },
        { it.targetClassId?.asString() ?: "" },
        { it.inheritedInterfaceSemanticKeys.joinToString(separator = "|") },
    )

    private fun buildDefaultIndependentMembersMap(
        models: List<CfirExtendSemanticModel>,
        resolver: CfirTypeResolver,
    ): Map<ClassId, List<Name>> {
        val interfaceIds = models
            .asSequence()
            .flatMap { model -> model.inheritedInterfaces.asSequence() }
            .mapNotNull { inherited -> inherited.classId }
            .toSortedSet(compareBy(ClassId::asString))

        val result = linkedMapOf<ClassId, List<Name>>()
        for (interfaceId in interfaceIds) {
            val interfaceDeclaration = resolver.resolveClass(interfaceId) ?: continue
            if (interfaceDeclaration.classKindOrNull() != CfirClassKind.INTERFACE) continue
            val typeParameters = interfaceDeclaration.typeParametersOrEmpty()
            if (typeParameters.isEmpty()) continue

            val members = interfaceDeclaration.declarations
                .asSequence()
                .filter { declaration -> declaration.hasDefaultImplementation() }
                .filter { declaration -> declaration.doesNotDependOnTypeParameters(typeParameters) }
                .mapNotNull { declaration -> declaration.memberNameOrNull() }
                .toList()
            if (members.isNotEmpty()) {
                result[interfaceId] = members
            }
        }
        return result
    }

    private fun CfirTypeRef.toClassIdOrNull(): ClassId? {
        val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
        return coneType.classIdOrPrimitiveClassId
    }
}

private fun org.cangnova.cangjie.cfir.declarations.CfirDeclaration.hasDefaultImplementation(): Boolean = when (this) {
    is CfirFunction -> !status.isAbstract
    is CfirProperty -> !status.isAbstract
    else -> false
}

private fun org.cangnova.cangjie.cfir.declarations.CfirDeclaration.memberNameOrNull(): Name? = when (this) {
    is CfirFunction -> callableNameOrNull()
    is CfirProperty -> name
    else -> null
}

private fun org.cangnova.cangjie.cfir.declarations.CfirDeclaration.doesNotDependOnTypeParameters(
    typeParameters: List<CfirTypeParameter>,
): Boolean {
    val typeParameterNames = typeParameters.mapTo(linkedSetOf()) { it.name.asString() }
    if (typeParameterNames.isEmpty()) return true
    val depends = when (this) {
        is CfirFunction -> returnTypeRef.containsAnyTypeParameter(typeParameterNames) ||
            valueParameters.any { parameter -> parameter.returnTypeRef.containsAnyTypeParameter(typeParameterNames) }
        is CfirProperty -> {
            if (returnTypeRef.containsAnyTypeParameter(typeParameterNames)) {
                true
            } else {
                val setterValueParameter = setter?.valueParameters?.firstOrNull()
                setterValueParameter?.returnTypeRef?.containsAnyTypeParameter(typeParameterNames) == true
            }
        }
        else -> false
    }
    return !depends
}

private fun CfirTypeRef.containsAnyTypeParameter(parameterNames: Set<String>): Boolean {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return false
    return coneType.containsAnyTypeParameter(parameterNames)
}

private fun org.cangnova.cangjie.cfir.types.ConeCangJieType.containsAnyTypeParameter(parameterNames: Set<String>): Boolean = when (this) {
    is ConeTypeParameterType -> lookupTag.name.asString() in parameterNames
    is ConeClassLikeType -> typeArguments.any { it.containsAnyTypeParameter(parameterNames) }
    is ConeStructType -> typeArguments.any { it.containsAnyTypeParameter(parameterNames) }
    is ConeEnumType -> typeArguments.any { it.containsAnyTypeParameter(parameterNames) }
    is org.cangnova.cangjie.cfir.types.ConeTypeAliasType -> typeArguments.any { it.containsAnyTypeParameter(parameterNames) } ||
        (expandedType?.containsAnyTypeParameter(parameterNames) == true)
    is org.cangnova.cangjie.cfir.types.ConeFuncType -> parameterTypes.any { it.containsAnyTypeParameter(parameterNames) } ||
        returnType.containsAnyTypeParameter(parameterNames)
    is org.cangnova.cangjie.cfir.types.ConeTupleType -> elementTypes.any { it.containsAnyTypeParameter(parameterNames) }
    is org.cangnova.cangjie.cfir.types.ConeVArrayType -> elementType.containsAnyTypeParameter(parameterNames)
    is org.cangnova.cangjie.cfir.types.ConePointerType -> pointeeType.containsAnyTypeParameter(parameterNames)
    is org.cangnova.cangjie.cfir.types.ConeIntersectionType -> intersectedTypes.any { it.containsAnyTypeParameter(parameterNames) }
    is org.cangnova.cangjie.cfir.types.ConeUnionType -> unionTypes.any { it.containsAnyTypeParameter(parameterNames) }
    else -> arrayElementType?.containsAnyTypeParameter(parameterNames) == true
}

private fun ConeTypeProjection.containsAnyTypeParameter(parameterNames: Set<String>): Boolean {
    return type.containsAnyTypeParameter(parameterNames)
}

private fun CfirClassLikeDeclaration.classKindOrNull(): CfirClassKind? = when (this) {
    is CfirPrimitiveTypeDeclaration -> CfirClassKind.CLASS
    is CfirClass -> CfirClassKind.CLASS
    is CfirInterface -> CfirClassKind.INTERFACE
    is CfirStruct -> CfirClassKind.STRUCT
    is CfirEnum -> CfirClassKind.ENUM
    else -> null
}

private fun CfirClassLikeDeclaration.typeParametersOrEmpty(): List<CfirTypeParameter> = when (this) {
    is CfirPrimitiveTypeDeclaration -> emptyList()
    is CfirClass -> typeParameters
    is CfirInterface -> typeParameters
    is CfirStruct -> typeParameters
    is CfirEnum -> typeParameters
    else -> emptyList()
}
