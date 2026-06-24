package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
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
        val nonErrorBounds = declaration.symbol.resolvedBounds.filterNot { it.coneType is ConeErrorType }
        if (nonErrorBounds.isEmpty()) return

        val uniqueBounds = linkedMapOf<String, CfirResolvedTypeRef>()
        nonErrorBounds.forEach { bound ->
            uniqueBounds.putIfAbsent(bound.stableBoundKey(), bound)
        }

        if (uniqueBounds.values.any { it.hasRecursiveBoundFailure(declaration.name) }) return

        val invalidBounds = uniqueBounds.values
            .mapNotNull { bound -> bound.takeIf { it.upperBoundKind() == UpperBoundKind.INVALID } }
        invalidBounds.forEach { bound ->
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE,
                a = bound.coneType.fullyExpandTypeAlias(),
                b = declaration.name,
            )
        }
        if (invalidBounds.isNotEmpty()) return

        val classBounds = uniqueBounds.values
            .filter { it.upperBoundKind() == UpperBoundKind.CLASS }
            .map { it.coneType.fullyExpandTypeAlias() }

        if (classBounds.size > 1 && !classBounds.areInOneInheritanceChain()) {
            reporter.reportOn(declaration.source, CfirErrors.CONFLICTING_UPPER_BOUNDS)
        }
    }
}

/**
 * 生成上界去重使用的稳定 key。
 */
private fun CfirResolvedTypeRef.stableBoundKey(): String = coneType
    .fullyExpandTypeAlias()
    .renderForDebugging()

/**
 * 判断上界是否已经属于递归上界错误场景。
 *
 * 递归上界由 generic deep 检查器报告，本检查器不重复报告 class/interface 约束错误。
 */
context(context: CheckerContext)
private fun CfirResolvedTypeRef.hasRecursiveBoundFailure(parameterName: Name): Boolean {
    val expandedType = coneType.fullyExpandTypeAlias()
    if (expandedType is ConeTypeParameterType && expandedType.lookupTag.name == parameterName) return true
    if (expandedType.isClassLikeUpperBound()) return false
    return expandedType.containsTypeParameterInArguments(parameterName)
}

/**
 * 判断类型是否在展开后为 class-like 上界。
 */
context(context: CheckerContext)
private fun ConeCangJieType.isClassLikeUpperBound(): Boolean =
    fullyExpandedType(context.session) is ConeClassLikeType

/**
 * 判断类型实参树中是否包含指定类型参数名称。
 */
private fun ConeCangJieType.containsTypeParameterInArguments(parameterName: Name): Boolean {
    for (argument in typeArguments) {
        val argumentType = argument.type ?: continue
        if (argumentType is ConeTypeParameterType && argumentType.lookupTag.name == parameterName) return true
        if (argumentType.containsTypeParameterInArguments(parameterName)) return true
    }
    return false
}

/**
 * 分类类型参数上界在声明规则中的角色。
 */
context(context: CheckerContext)
private fun CfirResolvedTypeRef.upperBoundKind(): UpperBoundKind {
    val expandedType = coneType.fullyExpandTypeAlias()
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
