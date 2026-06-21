package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
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
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.declaredExtendTargetKey
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CfirExtendIndexStore : CfirSessionComponent {
    private var models: List<CfirExtendSemanticModel> = emptyList()
    private var modelsByTargetKey: Map<CfirExtendTargetKey, List<CfirExtendSemanticModel>> = emptyMap()
    private var modelsByPackage: Map<FqName, List<CfirExtendSemanticModel>> = emptyMap()
    private var modelsByOrigin: Map<CfirExtendSemanticOrigin, List<CfirExtendSemanticModel>> = emptyMap()
    private var modelByDeclaration: Map<CfirExtend, CfirExtendSemanticModel> = emptyMap()
    private var containingExtendByCallableSymbol: Map<CfirCallableSymbol<*>, CfirExtend> = emptyMap()
    private var defaultIndependentMembersByInterface: Map<ClassId, List<Name>> = emptyMap()
    private var interfaceClosureByClassId: Map<ClassId, Set<ClassId>> = emptyMap()
    private var targetClassOwnInterfacesByClassId: Map<ClassId, Set<ClassId>> = emptyMap()

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
        modelsByTargetKey = next.filter { it.targetKey != null }.groupBy { it.targetKey!! }
        modelsByPackage = next.groupBy { it.packageFqName }
        modelsByOrigin = next.groupBy { it.origin }
        containingExtendByCallableSymbol = buildContainingExtendIndex(next)
        defaultIndependentMembersByInterface = buildDefaultIndependentMembersMap(next, resolver)
        interfaceClosureByClassId = buildInterfaceClosureMap(next, resolver)
        targetClassOwnInterfacesByClassId = buildTargetClassOwnInterfacesMap(next, resolver)
    }

    fun allModels(): List<CfirExtendSemanticModel> = models

    fun modelForDeclaration(declaration: Any): CfirExtendSemanticModel? = modelByDeclaration[declaration]

    fun modelsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtendSemanticModel> =
        modelsByTargetKey[targetKey].orEmpty()

    fun modelsForClass(classId: ClassId): List<CfirExtendSemanticModel> =
        modelsForTarget(CfirExtendTargetKey.ClassLike(classId))

    fun modelsInPackage(packageFqName: FqName): List<CfirExtendSemanticModel> = modelsByPackage[packageFqName].orEmpty()

    fun modelsByOrigin(origin: CfirExtendSemanticOrigin): List<CfirExtendSemanticModel> = modelsByOrigin[origin].orEmpty()

    fun containingExtendOf(symbol: CfirCallableSymbol<*>): CfirExtend? = containingExtendByCallableSymbol[symbol]

    fun defaultIndependentMembersOfInterface(interfaceClassId: ClassId): List<Name> =
        defaultIndependentMembersByInterface[interfaceClassId].orEmpty()

    fun targetClassOwnInterfaceClassIds(targetClassId: ClassId): Set<ClassId> =
        targetClassOwnInterfacesByClassId[targetClassId].orEmpty()

    /**
     * 返回单条 extend 声明直接接口及其传递父接口组成的稳定闭包。
     *
     * orphan rule 必须与索引层缓存的闭包语义保持一致，不能在 checker 里重新递归 provider，
     * 否则会出现 duplicate/orphan 两套规则对同一声明看到不同接口集合的问题。
     */
    fun inheritedInterfaceClosureClassIdsOf(declaration: Any): Set<ClassId> {
        val model = modelForDeclaration(declaration) ?: return emptySet()
        return buildLinkedSet {
            for (interfaceClassId in model.inheritedInterfaceClassIds) {
                addAll(interfaceClosureByClassId[interfaceClassId].orEmpty())
            }
        }
    }

    /**
     * 对齐官方 `TypeManager::IsExtendInheritRelation` 在默认接口成员合并中的用途：
     * 两个 extend 只要存在“其中一个直接接口继承了另一个直接接口”的关系，就视为相关 extend。
     */
    fun areExtendsInInheritRelation(firstDeclaration: Any, secondDeclaration: Any): Boolean {
        val first = modelForDeclaration(firstDeclaration) ?: return false
        val second = modelForDeclaration(secondDeclaration) ?: return false
        if (first === second) return true
        if (first.targetKey == null || first.targetKey != second.targetKey) return false
        return hasDirectInterfaceInheritedFrom(first, second) ||
            hasDirectInterfaceInheritedFrom(second, first)
    }

    private fun hasDirectInterfaceInheritedFrom(
        child: CfirExtendSemanticModel,
        parent: CfirExtendSemanticModel,
    ): Boolean {
        if (child.inheritedInterfaceClassIds.isEmpty() || parent.inheritedInterfaceClassIds.isEmpty()) {
            return false
        }
        val parentInterfaces = parent.inheritedInterfaceClassIds.toSet()
        return child.inheritedInterfaceClassIds.any { childInterface ->
            interfaceClosureByClassId[childInterface].orEmpty().any { it in parentInterfaces }
        }
    }

    /**
     * 对齐官方 `DeterminingSkipExtendByInheritanceRelationship`：
     * 同一目标的两个 extend 如果在不同接口上同时要求“当前在前”和“当前在后”，
     * 或单个接口同时被对方的子接口与父接口夹住，就无法决定检查顺序。
     */
    fun hasUndecidableExtendCheckSequence(declaration: Any): Boolean {
        val current = modelForDeclaration(declaration) ?: return false
        val targetKey = current.targetKey ?: return false
        return modelsForTarget(targetKey).any { other ->
            other.declaration !== current.declaration &&
                hasUndecidableExtendCheckSequence(current, other)
        }
    }

    private fun hasUndecidableExtendCheckSequence(
        current: CfirExtendSemanticModel,
        other: CfirExtendSemanticModel,
    ): Boolean {
        var previousOrder: Boolean? = null
        for (currentInterface in current.inheritedInterfaceClassIds) {
            var hasSubImplementation = false
            var hasSuperImplementation = false
            for (otherInterface in other.inheritedInterfaceClassIds) {
                if (otherInterface == currentInterface) continue
                if (otherInterface.isStrictSubtypeOfInterface(currentInterface)) {
                    hasSubImplementation = true
                }
                if (currentInterface.isStrictSubtypeOfInterface(otherInterface)) {
                    hasSuperImplementation = true
                }
            }
            if (hasSubImplementation && hasSuperImplementation) return true
            if (!hasSubImplementation && !hasSuperImplementation) continue

            val currentOrder = hasSubImplementation
            if (previousOrder != null && previousOrder != currentOrder) return true
            previousOrder = currentOrder
        }
        return false
    }

    private fun ClassId.isStrictSubtypeOfInterface(superInterface: ClassId): Boolean =
        this != superInterface && superInterface in interfaceClosureByClassId[this].orEmpty()

    fun otherPackageExtendedInterfaceClassIds(targetClassId: ClassId, currentPackage: FqName): Set<ClassId> =
        otherPackageExtendedInterfaceClassIds(CfirExtendTargetKey.ClassLike(targetClassId), currentPackage)

    fun otherPackageExtendedInterfaceClassIds(targetKey: CfirExtendTargetKey, currentPackage: FqName): Set<ClassId> {
        return modelsForTarget(targetKey)
            .asSequence()
            .filter { it.packageFqName != currentPackage }
            .flatMap { model ->
                model.inheritedInterfaceClassIds.asSequence().flatMap { interfaceClassId ->
                    interfaceClosureByClassId[interfaceClassId].orEmpty().asSequence()
                }
            }
            .toSet()
    }

    fun isFirstExtendForTarget(declaration: Any, targetClassId: ClassId): Boolean {
        return isFirstExtendForTarget(declaration, CfirExtendTargetKey.ClassLike(targetClassId))
    }

    fun isFirstExtendForTarget(declaration: Any, targetKey: CfirExtendTargetKey): Boolean {
        val myModel = modelByDeclaration[declaration] ?: return true
        val allModels = modelsForTarget(targetKey)
        return allModels.firstOrNull() === myModel
    }

    private inline fun buildLinkedSet(build: LinkedHashSet<ClassId>.() -> Unit): Set<ClassId> =
        linkedSetOf<ClassId>().apply(build)

    /**
     * 建立 extend 成员到 owner extend 的稳定索引。
     *
     * substitution scope 必须通过 providers 直接拿到 owner extend，才能在 scope 层
     * 完成声明复制与类型实参替换，不能退化成运行期遍历所有 extend 做反查。
     */
    private fun buildContainingExtendIndex(
        models: List<CfirExtendSemanticModel>,
    ): Map<CfirCallableSymbol<*>, CfirExtend> {
        val result = linkedMapOf<CfirCallableSymbol<*>, CfirExtend>()
        for (model in models) {
            for (declaration in model.declaration.declarations) {
                val callableDeclaration = declaration as? CfirCallableDeclaration ?: continue
                result[callableDeclaration.symbol] = model.declaration
            }
        }
        return result
    }

    private fun CfirExtend.toSemanticModel(
        file: CfirFile,
        declarationIndexInFile: Int,
        resolver: CfirTypeResolver,
    ): CfirExtendSemanticModel {
        val semanticNormalizer = CfirExtendTypeSemanticNormalizer(this)
        val targetClass = resolver.resolveClass(extendedTypeRef)
        val inheritedInterfaces = superTypeRefs.map { superTypeRef ->
            CfirExtendInheritedInterfaceSemantic(
                classId = superTypeRef.toClassIdOrNull(resolver),
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
            targetKey = extendedTypeRef.toExtendTargetKeyOrNull(),
            targetClassId = extendedTypeRef.toClassIdOrNull(resolver),
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
        { it.targetKey?.toString() ?: "" },
        { it.inheritedInterfaceSemanticKeys.joinToString(separator = "|") },
    )

    /**
     * 构建接口闭包缓存。
     *
     * 键为接口 ClassId，值为“自身 + 所有父接口”的传递闭包。
     * orphan rule 与 duplicate 检查都依赖这里的统一语义，不能退化成仅看直接父接口。
     */
    private fun buildInterfaceClosureMap(
        models: List<CfirExtendSemanticModel>,
        resolver: CfirTypeResolver,
    ): Map<ClassId, Set<ClassId>> {
        val interfaceIds = models
            .asSequence()
            .flatMap { model -> model.inheritedInterfaceClassIds.asSequence() }
            .toSortedSet(compareBy(ClassId::asString))
        if (interfaceIds.isEmpty()) return emptyMap()

        val memo = linkedMapOf<ClassId, Set<ClassId>>()
        for (interfaceId in interfaceIds) {
            collectInterfaceClosure(interfaceId, resolver, memo, linkedSetOf())
        }
        return memo
    }

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

    /**
     * 构建目标类自身声明的接口集合映射。
     * 收集每个 extend 目标类的 inheritedTypes 中所有接口的 ClassId。
     */
    private fun buildTargetClassOwnInterfacesMap(
        models: List<CfirExtendSemanticModel>,
        resolver: CfirTypeResolver,
    ): Map<ClassId, Set<ClassId>> {
        val targetClassIds = models.mapNotNull { it.targetClassId }.toSet()
        val result = linkedMapOf<ClassId, Set<ClassId>>()
        for (targetClassId in targetClassIds) {
            val declaration = resolver.resolveClass(targetClassId) ?: continue
            val ownInterfaces = collectOwnInterfaceClassIds(declaration, resolver)
            if (ownInterfaces.isNotEmpty()) {
                result[targetClassId] = ownInterfaces
            }
        }
        return result
    }

    /**
     * 递归收集一个声明的所有超类型中的接口 ClassId（含传递）
     */
    private fun collectOwnInterfaceClassIds(
        declaration: CfirClassLikeDeclaration,
        resolver: CfirTypeResolver,
    ): Set<ClassId> {
        val result = linkedSetOf<ClassId>()
        val memo = linkedMapOf<ClassId, Set<ClassId>>()
        for (superTypeRef in declaration.superTypeRefsOrEmpty()) {
            val classId = superTypeRef.toClassIdOrNull(resolver) ?: continue
            result.addAll(collectInterfaceClosure(classId, resolver, memo, linkedSetOf()))
        }
        return result
    }

    private fun collectInterfaceClosure(
        classId: ClassId,
        resolver: CfirTypeResolver,
        memo: MutableMap<ClassId, Set<ClassId>>,
        visiting: MutableSet<ClassId>,
    ): Set<ClassId> {
        memo[classId]?.let { cached -> return cached }
        if (!visiting.add(classId)) return emptySet()

        val declaration = resolver.resolveClass(classId)
        val result = linkedSetOf<ClassId>()
        if (declaration?.classKindOrNull() == CfirClassKind.INTERFACE) {
            result += classId
        }

        for (superTypeRef in declaration.superTypeRefsOrEmpty()) {
            val superClassId = superTypeRef.toClassIdOrNull(resolver) ?: continue
            result += collectInterfaceClosure(superClassId, resolver, memo, visiting)
        }

        visiting.remove(classId)
        memo[classId] = result
        return result
    }

    private fun CfirTypeRef.toClassIdOrNull(resolver: CfirTypeResolver): ClassId? {
        val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
        val abbreviatedTypeAlias = coneType.abbreviatedType as? ConeTypeAliasType
        if (abbreviatedTypeAlias != null) return abbreviatedTypeAlias.classId
        if (coneType is ConeTypeAliasType) return coneType.classId
        return coneType.classIdOrPrimitiveClassId
    }

    private fun CfirTypeRef.toExtendTargetKeyOrNull(): CfirExtendTargetKey? {
        val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
        return coneType.declaredExtendTargetKey
    }

    private fun ConeCangJieType.expandedClassIdOrPrimitiveClassId(resolver: CfirTypeResolver): ClassId? {
        if (this !is ConeTypeAliasType) return classIdOrPrimitiveClassId

        expandedType?.expandedClassIdOrPrimitiveClassId(resolver)?.let { return it }
        val typeAlias = resolver.resolveClass(classId) as? CfirTypeAlias
        val expandedConeType = (typeAlias?.expandedTypeRef as? CfirResolvedTypeRef)?.coneType
        return expandedConeType?.expandedClassIdOrPrimitiveClassId(resolver)
            ?: expandedClassIdOrPrimitiveClassId
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
    is org.cangnova.cangjie.cfir.types.ConeFunctionType -> parameterTypes.any { it.containsAnyTypeParameter(parameterNames) } ||
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

private fun CfirClassLikeDeclaration?.superTypeRefsOrEmpty(): List<CfirTypeRef> = when (this) {
    is CfirClass -> superTypeRefs
    is CfirInterface -> superTypeRefs
    is CfirStruct -> superTypeRefs
    is CfirEnum -> superTypeRefs
    else -> emptyList()
}
