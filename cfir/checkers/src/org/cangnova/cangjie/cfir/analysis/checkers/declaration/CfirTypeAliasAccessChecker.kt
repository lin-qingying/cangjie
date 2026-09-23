package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.ClassId

/**
 * 检查非 private type alias 是否引用了访问级别更低的类型。
 *
 * 对齐官方 `TypeCheckerImpl::CheckTypeAliasAccess`（`Sema/TypeCheckDecl.cpp:619`）：
 * - private 别名直接跳过；
 * - RHS 类型不正确（未声明类型、别名环等）时跳过，避免与内层错误重复；
 * - 按官方 `GetTypeArgsOfType`（`Sema/Utils.cpp:390`）的**前序**顺序枚举 RHS 类型树中的类型节点，
 *   只比对节点在语法位置上命名的声明本身，**不穿透 type alias 展开**：
 *   `public type A = B` 在 `B` 为 public 时不报，即使 `B` 展开后引用了 internal 类型；
 *   `public type D = C` 在 `C` 为 internal 时报 `C`，而不是 `C` 展开后的目标。
 * - 官方同一别名上最多保留一条该诊断（DiagEngine 按同类同范围去重），因此遇到首个违规即返回。
 */
object CfirTypeAliasAccessChecker : CfirTypeAliasChecker() {
    /**
     * 检查别名声明的访问级别是否被 RHS 类型树中访问级别更低的声明暴露。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeAlias) {
        val aliasVisibility = declaration.accessLevelVisibility() ?: return
        if (Visibilities.isPrivate(aliasVisibility)) return

        val expandedTypeRef = declaration.expandedTypeRef as? CfirResolvedTypeRef ?: return
        val expandedType = expandedTypeRef.coneType
        if (expandedType.containsErrorType()) return

        val target = expandedType.firstExposureTarget(aliasVisibility, linkedSetOf()) ?: return
        val targetVisibility = target.accessLevelVisibility() ?: return
        val targetName = target.declarationName() ?: return

        reporter.reportOn(
            source = declaration.typeAliasDeclarationHeaderDiagnosticSource(),
            factory = CfirErrors.TYPEALIAS_EXTERNAL_REFER_INTERNAL,
            a = aliasVisibility,
            b = declaration.name,
            c = targetVisibility,
            d = targetName,
        )
    }
}

/**
 * 按官方 `GetTypeArgsOfType` 的前序顺序查找第一个访问级别不足的引用目标声明。
 *
 * 只比对节点自身命名的声明（class/struct/enum/interface/typealias）：类型参数与基础类型没有
 * classId 因而自动跳过，与官方跳过 `GENERIC_PARAM_DECL` 一致。
 * `visitedTypes` 用于避免递归类型结构导致无限遍历。
 */
context(context: CheckerContext)
private fun ConeCangJieType.firstExposureTarget(
    declarationVisibility: Visibility,
    visitedTypes: MutableSet<ConeCangJieType>,
): CfirDeclaration? {
    if (!visitedTypes.add(this)) return null

    classIdOrNullForSyntaxTarget()?.let { classId ->
        val referencedDeclaration = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        val referencedVisibility = referencedDeclaration?.accessLevelVisibility()
        if (referencedDeclaration != null &&
            referencedVisibility != null &&
            !declarationVisibility.canExpose(referencedVisibility)
        ) {
            return referencedDeclaration
        }
    }

    for (projection in typeArguments) {
        projection.type.firstExposureTarget(declarationVisibility, visitedTypes)?.let { return it }
    }

    when (this) {
        is ConeFunctionType -> {
            for (parameterType in parameterTypes) {
                parameterType.firstExposureTarget(declarationVisibility, visitedTypes)?.let { return it }
            }
            returnType.firstExposureTarget(declarationVisibility, visitedTypes)?.let { return it }
        }

        is ConeTupleType -> {
            for (elementType in elementTypes) {
                elementType.firstExposureTarget(declarationVisibility, visitedTypes)?.let { return it }
            }
        }

        is ConeVArrayType -> elementType.firstExposureTarget(declarationVisibility, visitedTypes)?.let { return it }
        is ConePointerType -> pointeeType.firstExposureTarget(declarationVisibility, visitedTypes)?.let { return it }
        else -> {}
    }

    return null
}

/**
 * 取当前节点在官方语义下「语法位置上命名的声明」的 classId。
 *
 * 官方 `CheckTypeAliasAccess` 比对的是 RHS 类型树里 `REF_TYPE` 的 `ref.target`，即语法上写下的
 * 那个声明，**不穿透别名**：`public type A = B` 在 `B` 为 public 时不报，即使 `B` 展开后引用
 * internal 类型。CFIR 在 `expandTypeAliasesInTypeResolution` 打开时会就地展开别名
 * （`fullyExpandedTypeNoCache`），展开结果通过 abbreviation 属性保留别名身份，
 * 因此优先读 abbreviation，只在没有缩写时才退回节点自身的 classId。
 */
private fun ConeCangJieType.classIdOrNullForSyntaxTarget(): ClassId? {
    val abbreviated = abbreviatedType
    if (abbreviated != null) return abbreviated.classIdOrNull()
    return classIdOrNull()
}
