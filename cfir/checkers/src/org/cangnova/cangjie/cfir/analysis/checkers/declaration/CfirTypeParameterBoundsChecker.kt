package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaredUpperBoundTypesInCurrentContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.typeConstraintDiagnosticData
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
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
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

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
        if (with(context.session) { declaration.findFirstGenericUpperBoundRecursionIssueInOwner() } != null) return

        val nonErrorBounds = declaration
            .declaredUpperBoundTypesInCurrentContext()
            .filterNot { it is ConeErrorType }
        if (nonErrorBounds.isEmpty()) return

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
        if (invalidBounds.isNotEmpty()) return

        val classBounds = uniqueBounds.values
            .filter { it.upperBoundKind() == UpperBoundKind.CLASS }
            .map { it.fullyExpandTypeAlias() }

        if (classBounds.size > 1 && !classBounds.areInOneInheritanceChain()) {
            reporter.reportOn(declaration.source, CfirErrors.CONFLICTING_UPPER_BOUNDS)
        }

        declaration.reportUpperBoundInheritedMemberTypeConsistency(uniqueBounds.values)
    }
}

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
 * 为 class/interface 上界创建声明侧成员 scope。
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
        scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
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
    for (containingDeclaration in context.containingDeclarations.asReversed()) {
        val owner = containingDeclaration as? CfirTypeParameterRefsOwner ?: continue
        if (owner.typeParameters.none { it.symbol == symbol }) continue
        val ownerDeclaration = containingDeclaration as? CfirDeclaration ?: continue
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
