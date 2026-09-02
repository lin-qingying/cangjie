package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaredUpperBoundConeTypeInCurrentContextOrNull
import org.cangnova.cangjie.cfir.analysis.checkers.declaredUpperBoundTypesInCurrentContext
import org.cangnova.cangjie.cfir.analysis.checkers.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.typeConstraintDiagnosticData
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.types.declaredUpperBoundConeTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredUpperBoundRefsAfterTypeResolve
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 类型参数上界合法性检查器。
 *
 * 该检查器去重后检查上界是否为 class/interface，可忽略 Any/C 类型上界，并报告多个 class 上界
 * 不在同一继承链中的冲突。
 */
object CfirTypeParameterBoundsChecker : CfirTypeParameterChecker() {
    /**
     * 检查单个类型参数的所有已解析上界。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeParameter) {
        val owner = declaration.containingDeclarationSymbol.cfir as? CfirTypeParameterRefsOwner
        if (owner?.typeParameters?.firstOrNull()?.symbol == declaration.symbol) {
            owner.findFirstInvalidNestedGenericUpperBoundInstantiation()?.let { violation ->
                reporter.reportOn(
                    source = owner.source?.firstCharacterDiagnosticSource(),
                    factory = CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT,
                    a = violation.actualType,
                    b = violation.upperBound,
                    c = violation.genericType,
                )
            }
        }

        if (with(context.session) { declaration.findFirstGenericUpperBoundRecursionIssueInOwner() } != null) return

        val nonErrorBounds = declaration
            .declaredUpperBoundTypesInCurrentContext()
            .filterNot { it is ConeErrorType }

        val uniqueBounds = linkedMapOf<String, ConeCangJieType>()
        nonErrorBounds.forEach { bound ->
            uniqueBounds.putIfAbsent(bound.stableBoundKey(), bound)
        }

        val invalidBounds = uniqueBounds.values
            .mapNotNull { bound -> bound.takeIf { it.upperBoundKind() == UpperBoundKind.INVALID } }
        invalidBounds.firstOrNull()?.let { bound ->
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE,
                a = bound.fullyExpandTypeAlias(),
                b = declaration.name,
            )
        }
        val boundsWithExposedClassConstraints =
            (declaration.containingDeclarationSymbol.cfir as? CfirTypeParameterRefsOwner)
                ?.collectAssumptionUpperBounds()
                ?.get(declaration.symbol)
                ?: uniqueBounds.values.toList()
        if (boundsWithExposedClassConstraints.isEmpty()) return

        val classBounds = boundsWithExposedClassConstraints
            .filter { it.upperBoundKind() == UpperBoundKind.CLASS }
            .map { it.fullyExpandTypeAlias() }

        if (classBounds.size > 1 && !classBounds.areInOneInheritanceChain()) {
            reporter.reportOn(declaration.source, CfirErrors.MULTIPLE_CLASS_UPPER_BOUNDS)
        }

        declaration.reportUpperBoundInheritedMemberTypeConsistency(boundsWithExposedClassConstraints)
    }
}

/**
 * 对齐官方 `CheckUpperBoundsLegality`：检查声明上界中递归出现的泛型实例化。
 *
 * 该检查必须在声明级执行。类型引用 checker 只能把错误落到嵌套实参，无法补上
 * 官方同时落在所属 class/function 声明头部的约束错误。
 */
context(context: CheckerContext)
private fun CfirTypeParameterRefsOwner.findFirstInvalidNestedGenericUpperBoundInstantiation():
    GenericUpperBoundInstantiationViolation? {
    val visited = linkedSetOf<String>()
    for (typeParameter in typeParameters) {
        for (upperBound in typeParameter.declaredUpperBoundTypesForInstantiation()) {
            upperBound.findFirstInvalidNestedGenericUpperBoundInstantiation(visited)?.let { return it }
        }
    }
    return null
}

/** 在一个上界类型树中查找第一个不满足目标泛型声明约束的实例化。 */
context(context: CheckerContext)
private fun ConeCangJieType.findFirstInvalidNestedGenericUpperBoundInstantiation(
    visited: MutableSet<String>,
): GenericUpperBoundInstantiationViolation? {
    val expandedType = fullyExpandTypeAlias()
    val visitKey = expandedType.renderForDebugging()
    if (!visited.add(visitKey)) return null

    val classifierType = expandedType as? ConeClassifierType
    if (classifierType != null && classifierType.typeArguments.isNotEmpty()) {
        val targetDeclaration = expandedType.toResolvedClassLikeDeclaration()
        val lookupType = expandedType as? ConeLookupTagBasedType
        if (targetDeclaration != null && lookupType != null &&
            targetDeclaration.typeParameters.size == classifierType.typeArguments.size
        ) {
            val substitutor = targetDeclaration.createDeclarationTypeSubstitutor(lookupType)
            for ((index, targetParameter) in targetDeclaration.typeParameters.withIndex()) {
                val targetBounds = targetParameter.declaredUpperBoundTypesForInstantiation()
                if (targetBounds.isEmpty()) continue

                val actualType = classifierType.typeArguments[index].type
                if (actualType is ConeErrorType) continue

                val substitutedBounds = targetBounds.map { bound ->
                    substitutor.substituteOrSelf(bound)
                }.filterNot { it is ConeErrorType }
                if (substitutedBounds.isEmpty()) continue

                if (!actualType.satisfiesGenericUpperBounds(substitutedBounds)) {
                    return GenericUpperBoundInstantiationViolation(
                        genericType = expandedType,
                        actualType = actualType,
                        upperBound = context.session.typeContext.intersectTypes(substitutedBounds) as ConeCangJieType,
                    )
                }
            }
        }
    }

    classifierType?.typeArguments?.forEach { argument ->
        argument.type.findFirstInvalidNestedGenericUpperBoundInstantiation(visited)?.let { return it }
    }
    return null
}

/**
 * 泛型实参满足声明约束的判断。
 *
 * 对具体类型要求其满足全部上界；对泛型实参则沿官方 `Assumption` 暴露的上界逐条
 * 寻找满足关系，避免把 `X <: A<X>` 错误判成不满足 `A<T>` 自身的约束。
 */
context(context: CheckerContext)
private fun ConeCangJieType.satisfiesGenericUpperBounds(
    upperBounds: List<ConeCangJieType>,
): Boolean {
    val typeParameterType = this as? ConeTypeParameterType
    if (typeParameterType == null) {
        return upperBounds.all { upperBound ->
            AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                context.session.typeContext,
                this,
                upperBound,
            )
        }
    }

    val typeParameter = typeParameterType.lookupTag.typeParameterSymbol.cfir
    val owner = typeParameter.containingDeclarationSymbol.cfir as? CfirTypeParameterRefsOwner
    val exposedBounds = owner
        ?.collectAssumptionUpperBounds()
        ?.get(typeParameter.symbol)
        .orEmpty()
        .ifEmpty { typeParameter.declaredUpperBoundTypesForInstantiation() }

    return upperBounds.all { upperBound ->
        exposedBounds.any { exposedBound ->
            AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                context.session.typeContext,
                exposedBound,
                upperBound,
            )
        }
    }
}

/** 读取目标泛型声明参数的已解析上界，并跳过错误中间态。 */
context(context: CheckerContext)
private fun CfirTypeParameterRef.declaredUpperBoundTypesForInstantiation(): List<ConeCangJieType> =
    symbol.toLookupTag()
        .declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { bound ->
            bound.declaredUpperBoundConeTypeInCurrentContextOrNull()
                ?: bound.declaredUpperBoundConeTypeOrNull()
        }
        .filterNot { it is ConeErrorType }

/** 记录官方递归泛型实例化检查所需的三类语义类型。 */
private data class GenericUpperBoundInstantiationViolation(
    /** 发生约束检查的泛型实例化类型。 */
    val genericType: ConeCangJieType,
    /** 实际写出的泛型实参。 */
    val actualType: ConeCangJieType,
    /** 该实参必须满足的上界。 */
    val upperBound: ConeCangJieType,
)

/**
 * 按官方 `Assumption` 阶段构建当前声明的泛型约束环境。
 *
 * class/interface 上界声明中的约束必须归属到代换后的泛型实参：
 * `X <: A<X>` 会把 `A.T <: C1` 归入 `X`，而 `X <: A<Y>` 会归入 `Y`。
 * 因此该计算以整个 owner 为单位，不能从单个待检查参数局部展开。
 */
context(context: CheckerContext)
private fun CfirTypeParameterRefsOwner.collectAssumptionUpperBounds(): Map<CfirTypeParameterSymbol, List<ConeCangJieType>> {
    val ownerParameters = typeParameters.mapTo(linkedSetOf()) { it.symbol }
    val result = ownerParameters.associateWithTo(linkedMapOf()) { linkedMapOf<String, ConeCangJieType>() }
    val queue = ArrayDeque<Pair<CfirTypeParameterSymbol, ConeCangJieType>>()

    for (typeParameter in typeParameters) {
        typeParameter.symbol.cfir
            .declaredUpperBoundTypesInCurrentContext()
            .forEach { queue += typeParameter.symbol to it }
    }

    while (queue.isNotEmpty()) {
        val (targetParameter, rawBound) = queue.removeFirst()
        val current = rawBound.fullyExpandTypeAlias()
        if (current is ConeErrorType) continue
        val targetBounds = result.getValue(targetParameter)
        if (targetBounds.putIfAbsent(current.stableBoundKey(), current) != null) continue

        current.assumptionConstraints(targetParameter, ownerParameters).forEach(queue::addLast)
    }

    return result.mapValues { (_, bounds) -> bounds.values.toList() }
}

/**
 * 将 class/interface 实例声明侧的泛型约束代换为当前 owner 的约束边。
 * 只有代换后的约束左侧仍是 owner 类型参数时，才对应官方 `AddConstraint` 可写入的关系。
 */
context(context: CheckerContext)
private fun ConeCangJieType.assumptionConstraints(
    targetParameter: CfirTypeParameterSymbol,
    ownerParameters: Set<CfirTypeParameterSymbol>,
): List<Pair<CfirTypeParameterSymbol, ConeCangJieType>> {
    val referencedTypeParameter = (this as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol
    if (referencedTypeParameter != null) {
        return referencedTypeParameter.cfir
            .declaredUpperBoundTypesInCurrentContext()
            .filterNot { it is ConeErrorType }
            .map { bound -> targetParameter to bound }
    }

    val lookupType = fullyExpandTypeAlias() as? ConeLookupTagBasedType ?: return emptyList()
    val declaration = lookupType.toResolvedClassLikeDeclaration() as? CfirTypeParameterRefsOwner ?: return emptyList()
    if (declaration.typeParameters.isEmpty() || declaration.typeParameters.size != lookupType.typeArguments.size) {
        return emptyList()
    }

    val substitutor = declaration.createDeclarationTypeSubstitutor(lookupType)
    return buildList {
        for (typeParameter in declaration.typeParameters) {
            val substitutedSubject = substitutor.substituteOrSelf(typeParameter.symbol.constructType())
            val targetParameter = (substitutedSubject as? ConeTypeParameterType)
                ?.lookupTag
                ?.typeParameterSymbol
                ?.takeIf { it in ownerParameters }
                ?: continue

            for (bound in typeParameter.declaredUpperBoundTypesForExposure()) {
                val substitutedBound = substitutor.substituteOrSelf(bound)
                if (substitutedBound !is ConeErrorType) {
                    add(targetParameter to substitutedBound)
                }
            }
        }
    }
}

/**
 * 创建 class/interface 声明类型参数到当前使用点实参的替换器。
 */
context(context: CheckerContext)
private fun CfirTypeParameterRefsOwner.createDeclarationTypeSubstitutor(
    type: ConeLookupTagBasedType,
): ConeSubstitutor {
    val substitutions = typeParameters.zip(type.typeArguments).associate { (typeParameter, argument) ->
        typeParameter.symbol.toLookupTag() as TypeConstructorMarker to argument.type
    }
    return createTypeSubstitutorByTypeConstructor(
        map = substitutions,
        context = context.session.typeContext,
        approximateIntegerLiterals = false,
    )
}

/**
 * 读取类型参数声明侧上界，用于泛型 class/interface 上界暴露。
 */
context(context: CheckerContext)
private fun CfirTypeParameterRef.declaredUpperBoundTypesForExposure(): List<ConeCangJieType> =
    symbol.toLookupTag()
        .declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { it.declaredUpperBoundConeTypeInCurrentContextOrNull() }

/**
 * 检查泛型上界交集继承到的同签名成员返回类型/属性类型是否一致。
 *
 * 对齐官方 `GenericInheritanceChecker::CheckUpperBoundsConfliction`：接口上界先合并，
 * 再用最具体 class 上界的 inherited member 表更新，冲突诊断落在整条 where 约束上。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun CfirTypeParameter.reportUpperBoundInheritedMemberTypeConsistency(
    bounds: Collection<ConeCangJieType>,
) {
    val interfaceBounds = bounds
        .filter { it.upperBoundKind() == UpperBoundKind.INTERFACE }
        .map { it.fullyExpandTypeAlias() }
    val classBound = bounds
        .filter { it.upperBoundKind() == UpperBoundKind.CLASS }
        .map { it.fullyExpandTypeAlias() }
        .smallestClassUpperBoundOrNull()

    val memberTypes = buildList {
        for (interfaceBound in interfaceBounds) {
            interfaceBound.upperBoundMemberScope()?.collectUpperBoundMemberTypes()?.let(::addAll)
        }
        classBound?.upperBoundMemberScope()?.collectUpperBoundMemberTypes()?.let(::addAll)
    }
    if (memberTypes.size < 2) return

    val source = upperBoundConstraintDiagnosticSource()
    val reported = mutableSetOf<String>()
    for ((key, members) in memberTypes.groupBy { it.conflictKey }) {
        if (!reported.add(key)) continue
        val types = members.map { it.type }.filterNot { it is ConeErrorType }
        if (types.size < 2 || !types.hasInconsistentUpperBoundTypes()) continue
        val first = members.first()
        reporter.reportOn(
            source = source,
            factory = CfirErrors.INHERIT_MEMBER_TYPE_INCONSISTENT,
            a = if (first.kind == UpperBoundMemberKind.PROPERTY) "types" else "return types",
            b = if (first.kind == UpperBoundMemberKind.PROPERTY) "property" else "function",
            c = first.name,
        )
    }
}

/**
 * 选择官方用于 generic upper-bound 合并的最具体 class 上界。
 */
context(context: CheckerContext)
private fun List<ConeCangJieType>.smallestClassUpperBoundOrNull(): ConeCangJieType? {
    var smallest: ConeCangJieType? = null
    for (bound in this) {
        val current = smallest
        if (current == null || AbstractTypeChecker.isSubtypeOf(context.session.typeContext, bound, current)) {
            smallest = bound
        }
    }
    return smallest
}

/**
 * 为 class/interface 上界创建使用点成员 scope。
 *
 * 泛型上界冲突比较消费的是该类型在当前使用点可见的完整成员集合，因此必须包含
 * 可访问 extend 成员；声明点 scope 会有意排除 extend，只适合声明自身继承检查。
 */
context(context: CheckerContext)
private fun ConeCangJieType.upperBoundMemberScope(): CfirTypeScope? {
    val expandedType = fullyExpandTypeAlias()
    val classId = expandedType.classIdOrPrimitiveClassId ?: return null
    val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
    val rawScope = CfirClassUseSiteMemberScope(
        session = context.session,
        classSymbol = symbol,
        symbolProvider = context.session.symbolProvider,
        extendProvider = context.session.extendProvider,
        directSupertypeProvider = context.session.directSupertypeProviderOrNull,
        ownerType = expandedType,
        dispatchReceiverType = expandedType,
        scopeKind = CfirClassMemberScopeKind.USE_SITE,
    )
    return CfirClassSubstitutionScope(
        session = context.session,
        useSiteMemberScope = rawScope,
        dispatchReceiverType = expandedType,
        substitutionOwnerType = expandedType,
    )
}

/**
 * 收集上界成员 scope 中用于类型一致性比较的函数与属性类型。
 */
context(context: CheckerContext)
private fun CfirTypeScope.collectUpperBoundMemberTypes(): List<UpperBoundMemberType> = buildList {
    for (name in getCallableNames()) {
        processFunctionsByName(name) { symbol ->
            symbol.upperBoundFunctionMemberTypeOrNull()?.let(::add)
        }
        processPropertiesByName(name) { symbol ->
            symbol.upperBoundPropertyMemberTypeOrNull()?.let(::add)
        }
    }
}

/**
 * 转换函数成员为上界一致性比较项。
 */
context(context: CheckerContext)
private fun CfirNamedFunctionSymbol.upperBoundFunctionMemberTypeOrNull(): UpperBoundMemberType? {
    if (!isBound) return null
    val returnType = resolvedReturnTypeOrNull() ?: return null
    if (returnType is ConeErrorType) return null
    return UpperBoundMemberType(
        name = name,
        kind = UpperBoundMemberKind.FUNCTION,
        isStatic = cfir.status.isStatic,
        signature = overrideSignatureKey(),
        type = returnType,
    )
}

/**
 * 转换属性成员为上界一致性比较项。
 */
context(context: CheckerContext)
private fun CfirPropertySymbol.upperBoundPropertyMemberTypeOrNull(): UpperBoundMemberType? {
    if (!isBound) return null
    val propertyType = resolvedReturnTypeOrNull() ?: return null
    if (propertyType is ConeErrorType) return null
    return UpperBoundMemberType(
        name = name,
        kind = UpperBoundMemberKind.PROPERTY,
        isStatic = cfir.status.isStatic,
        signature = overrideSignatureKey(),
        type = propertyType,
    )
}

/**
 * 计算 callable 的返回/属性类型。
 */
context(context: CheckerContext)
private fun CfirCallableSymbol<*>.resolvedReturnTypeOrNull(): ConeCangJieType? {
    if (!isBound) return null
    return context.returnTypeCalculator.tryCalculateReturnType(cfir).coneType
}

/**
 * 判断一组成员类型是否存在既不相等也不存在子类型关系的冲突。
 */
context(context: CheckerContext)
private fun List<ConeCangJieType>.hasInconsistentUpperBoundTypes(): Boolean {
    for (i in indices) {
        for (j in i + 1 until size) {
            val first = this[i]
            val second = this[j]
            if (AbstractTypeChecker.equalTypes(context.session.typeContext, first, second)) continue
            val related = AbstractTypeChecker.isSubtypeOf(context.session.typeContext, first, second) ||
                    AbstractTypeChecker.isSubtypeOf(context.session.typeContext, second, first)
            if (!related) return true
        }
    }
    return false
}

/**
 * 返回该类型参数对应 where 约束的整条 source，找不到时退回类型参数 source。
 */
context(context: CheckerContext)
private fun CfirTypeParameter.upperBoundConstraintDiagnosticSource(): CjSourceElement? {
    for (containingSymbol in context.containingDeclarations.asReversed()) {
        val ownerDeclaration = containingSymbol.cfir
        val owner = ownerDeclaration as? CfirTypeParameterRefsOwner ?: continue
        if (owner.typeParameters.none { it.symbol == symbol }) continue
        return ownerDeclaration.attributes.typeConstraintDiagnosticData
            ?.typeConstraints
            ?.firstOrNull { it.parameterName == name }
            ?.constraintSource
            ?: source
    }
    return source
}

/**
 * 上界成员类型一致性比较项。
 */
private data class UpperBoundMemberType(
    /** 成员名。 */
    val name: Name,
    /** 成员种类。 */
    val kind: UpperBoundMemberKind,
    /** 成员是否为 static。 */
    val isStatic: Boolean,
    /** override 签名，不包含返回类型。 */
    val signature: String,
    /** 用于一致性比较的返回类型或属性类型。 */
    val type: ConeCangJieType,
) {
    /** 合并同签名成员的稳定 key。 */
    val conflictKey: String = "${kind.name}:$isStatic:$signature"
}

/**
 * 上界成员种类。
 */
private enum class UpperBoundMemberKind {
    /** 函数成员。 */
    FUNCTION,
    /** 属性成员。 */
    PROPERTY,
}

/**
 * 生成上界去重使用的稳定 key。
 */
private fun ConeCangJieType.stableBoundKey(): String = this
    .fullyExpandTypeAlias()
    .renderForDebugging()

/**
 * 分类类型参数上界在声明规则中的角色。
 */
context(context: CheckerContext)
private fun ConeCangJieType.upperBoundKind(): UpperBoundKind {
    val expandedType = fullyExpandTypeAlias()
    return when (expandedType) {
        ConeAnyType -> UpperBoundKind.IGNORED_TOP_OR_CTYPE
        is ConeClassLikeType -> {
            val classId = expandedType.classId
            when {
                classId == StdlibClassIds.Any || CfirExtendSemantics.isCType(classId) ->
                    UpperBoundKind.IGNORED_TOP_OR_CTYPE
                expandedType.toResolvedClassLikeDeclaration() is CfirInterface ->
                    UpperBoundKind.INTERFACE
                expandedType.toResolvedClassLikeDeclaration() is CfirClass ->
                    UpperBoundKind.CLASS
                expandedType.isInterface ->
                    UpperBoundKind.INTERFACE
                else ->
                    UpperBoundKind.CLASS
            }
        }
        else -> {
            val classId = expandedType.classIdOrPrimitiveClassId
            if (classId == StdlibClassIds.Any || CfirExtendSemantics.isCType(classId)) {
                UpperBoundKind.IGNORED_TOP_OR_CTYPE
            } else {
                UpperBoundKind.INVALID
            }
        }
    }
}

/**
 * 将类型解析为对应 class-like 声明。
 */
context(context: CheckerContext)
private fun ConeCangJieType.toResolvedClassLikeDeclaration(): CfirClassLikeDeclaration? =
    when (this) {
        is ConeClassLikeType -> context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        is ConeTypeAliasType -> context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        else -> null
    }

/**
 * 完全展开 typealias 类型。
 */
private fun ConeCangJieType.fullyExpandTypeAlias(): ConeCangJieType {
    var current = this
    while (current is ConeTypeAliasType && current.expandedType != null) {
        current = current.expandedType ?: break
    }
    return current
}

/**
 * 判断多个 class 上界是否位于同一继承链。
 */
context(context: CheckerContext)
private fun List<ConeCangJieType>.areInOneInheritanceChain(): Boolean {
    for (leftIndex in indices) {
        for (rightIndex in leftIndex + 1 until size) {
            if (!this[leftIndex].isRelatedTo(this[rightIndex])) return false
        }
    }
    return true
}

/**
 * 判断两个类型是否存在任一方向的子类型关系。
 */
context(context: CheckerContext)
private fun ConeCangJieType.isRelatedTo(other: ConeCangJieType): Boolean =
    AbstractTypeChecker.isSubtypeOf(context.session.typeContext, this, other) ||
            AbstractTypeChecker.isSubtypeOf(context.session.typeContext, other, this)

/**
 * 上界分类结果。
 */
private enum class UpperBoundKind {
    /**
     * Any 或 C 类型上界，当前规则忽略。
     */
    IGNORED_TOP_OR_CTYPE,

    /**
     * class 上界。
     */
    CLASS,

    /**
     * interface 上界。
     */
    INTERFACE,

    /**
     * 非 class/interface 且不可忽略的非法上界。
     */
    INVALID,
}
