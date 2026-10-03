package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.findBuiltinAnnotations
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.hasBuiltinAnnotation
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.expressions.booleanArgument
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 类型不匹配诊断专用的子类型判断。
 *
 * 对齐 Kotlin `FirHelpers.isSubtypeForTypeMismatch`：诊断阶段先展开类型别名，
 * 再以“错误类型可匹配任意类型、stub 类型不可匹配任意类型”的状态调用类型检查器。
 */
fun isSubtypeForTypeMismatch(
    session: CfirSession,
    context: ConeInferenceContext,
    subtype: ConeCangJieType,
    supertype: ConeCangJieType,
): Boolean {
    val subtypeFullyExpanded = subtype.fullyExpandedType(session)
    val supertypeFullyExpanded = supertype.fullyExpandedType(session)
    // `This` 返回类型要求实际返回值也保持 `This` 视图；普通 class 实例即使 ClassId 相同也不能替代。
    if (
        supertypeFullyExpanded is ConeClassLikeType &&
        supertypeFullyExpanded.isThisType &&
        (subtypeFullyExpanded !is ConeClassLikeType || !subtypeFullyExpanded.isThisType)
    ) {
        return false
    }
    return AbstractTypeChecker.isSubtypeOf(
        context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = false,
        ),
        subtypeFullyExpanded,
        supertypeFullyExpanded,
    )
}

/**
 * 根据期望返回类型选择返回值类型不匹配诊断工厂。
 *
 * 普通返回类型使用 [CfirErrors.RETURN_TYPE_MISMATCH]；当期望类型是 `This`
 * 时，官方语义要求使用通用 [CfirErrors.TYPE_MISMATCH]，避免把 `This`
 * 约束降级为普通类实例返回类型不匹配。
 */
fun diagnosticFactoryForReturnTypeMismatch(
    session: CfirSession,
    expectedType: ConeCangJieType,
): CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> {
    val expandedExpectedType = expectedType.fullyExpandedType(session)
    // 官方 `This` 返回约束按通用类型不匹配报告，而不是普通 return-type mismatch。
    return if (expandedExpectedType is ConeClassLikeType && expandedExpectedType.isThisType) {
        CfirErrors.TYPE_MISMATCH
    } else {
        CfirErrors.RETURN_TYPE_MISMATCH
    }
}

/**
 * 判断表达式是否是仓颉 flow 表达式。
 *
 * flow 在官方语义中先作为二元表达式完成解糖调用，再由 flow 节点本身检查目标类型；
 * 因此它作为隐式尾值或显式 `return` 值时都应使用同一个通用类型不匹配诊断。
 */
internal fun CfirExpression.isFlowExpression(): Boolean = when (this) {
    is CfirBinaryOp -> kind == CfirBinaryOpKind.PIPELINE || kind == CfirBinaryOpKind.COMPOSITION
    is CfirFunctionCall -> origin == CfirFunctionCallOrigin.Pipeline ||
            origin == CfirFunctionCallOrigin.CompilerCoreIntrinsic
    is CfirWrappedExpression -> expression.isFlowExpression()
    else -> false
}

/**
 * 同包弃用豁免（对齐官方 `TypeChecker::ShouldSkipDeprecationDiagnostic`）。
 *
 * 官方 `CheckUsageOfDeprecated` 在进入一个带 `@Deprecated` 的声明时把该声明记为
 * `deprecatedContext` / `strictDeprecatedContext`，离开时清空；报告使用点之前先判断
 * 目标声明与当前上下文声明是否同包：strict 目标只在上下文声明本身 strict 时豁免，
 * 非 strict 目标在任意同包弃用声明内部豁免。cjc 1.0.5 实测：弃用类 B 内部调用同包弃用类 A
 * 零诊断；非弃用声明内部调用弃用类 A 报 warning；非 strict 的弃用子类 D 继承 strict 弃用父类 C
 * 仍报 `strictness ... weaken` 与两处 `class 'C' is deprecated.`。
 */
internal fun CheckerContext.shouldSkipSamePackageDeprecation(targetPackage: FqName?, strict: Boolean): Boolean {
    if (targetPackage == null) return false
    for (symbol in containingDeclarations.asReversed()) {
        val declaration = symbol.cfir as? CfirDeclaration ?: continue
        if (!declaration.hasBuiltinAnnotation(BuiltInAnnotationKind.DEPRECATED)) continue
        val annotation = declaration.findBuiltinAnnotations(BuiltInAnnotationKind.DEPRECATED)
            .firstOrNull() as? CfirAnnotationCall ?: continue
        val enclosingIsStrict = annotation.booleanArgument("strict") == true
        if (strict && !enclosingIsStrict) continue
        val enclosingPackage = symbol.declarationPackageFqName() ?: continue
        if (enclosingPackage == targetPackage) return true
    }
    return false
}

/** 取符号所属声明的包名；callable 取 classId，class-like 直接取 classId。 */
private fun CfirBasedSymbol<*>.declarationPackageFqName(): FqName? = when (this) {
    // 顶层 callable 的 `classId` 为 null（见 CallableId 注释），包名只能取 `packageName`。
    is CfirCallableSymbol<*> -> callableId.packageName
    is CfirClassLikeSymbol<*> -> classId.packageFqName
    else -> null
}
