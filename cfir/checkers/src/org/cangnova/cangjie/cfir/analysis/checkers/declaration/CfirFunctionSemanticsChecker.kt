package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.modifierByToken
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 函数语义检查器（Function 分组）
 *
 * 检查 static 函数重载冲突（同名函数不能混合 static 和 non-static）。
 * 对齐 C++ TypeChecker 中 sema_static_function_overload_conflicts 检查。
 */
object CfirFunctionOverloadChecker : CfirSimpleFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        checkStaticNonStaticOverloadConflict(declaration)
    }

    /**
     * 检查同名函数不能混合 static 和 non-static。
     *
     * 对齐 C++ DiagKind::sema_static_function_overload_conflicts:
     * 当同一个类/结构体/枚举中存在同名的 static 和 non-static 函数时报错。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticNonStaticOverloadConflict(function: CfirNamedFunction) {
        val ownerClassId = function.symbol.callableId.classId ?: return
        val owner = context.session.symbolProvider
            .getClassLikeSymbolByClassId(ownerClassId)?.cfir as? CfirClassLikeDeclaration ?: return

        val isStatic = function.status.isStatic
        val functionName = function.name

        // 在同一类型的声明中查找同名但 static 属性不同的函数
        val hasConflict = owner.declarations.any { sibling ->
            sibling is CfirNamedFunction &&
                    sibling !== function &&
                    sibling.name == functionName &&
                    sibling.status.isStatic != isStatic
        }

        if (hasConflict) {
            reporter.reportOn(
                source = function.source,
                factory = CfirErrors.STATIC_FUNCTION_OVERLOAD_CONFLICTS,
                a = functionName,
            )
        }
    }
}

/**
 * 函数声明状态合法性检查器。
 *
 * 对齐仓颉声明属性语义：
 * - `mut func` 允许作为 struct / interface 成员函数；
 * - `static` 函数不能同时承担 open / abstract / override / operator 这类实例分派语义；
 * - 当真实源码修饰符已经由通用 modifier checker 诊断时，本 checker 不重复报函数名级诊断。
 */
object CfirFunctionDeclarationStatusChecker : CfirSimpleFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        checkMutFunction(declaration)
        checkStaticFunctionStatus(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMutFunction(function: CfirNamedFunction) {
        if (!function.status.isMut) return
        if (!function.isLocal && context.closestContainingTypeDeclaration() is CfirStruct) return
        if (!function.isLocal && context.closestContainingTypeDeclaration() is CfirInterface) return
        if (function.hasSourceModifier(CjTokens.MUT_KEYWORD)) return

        reporter.reportOn(
            source = function.functionNameDiagnosticSource(),
            factory = CfirErrors.MUT_ONLY_ON_FUNCTION,
            a = function.name,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticFunctionStatus(function: CfirNamedFunction) {
        if (!function.status.isStatic) return
        val conflictingStatusModifier = when {
            function.status.isOpen -> CjTokens.OPEN_KEYWORD
            function.status.isAbstract -> CjTokens.ABSTRACT_KEYWORD
            function.status.isOverride -> CjTokens.OVERRIDE_KEYWORD
            function.status.isOperator -> CjTokens.OPERATOR_KEYWORD
            else -> null
        }
        if (conflictingStatusModifier == null) {
            return
        }
        if (function.hasSourceModifier(CjTokens.STATIC_KEYWORD) && function.hasSourceModifier(conflictingStatusModifier)) return

        reporter.reportOn(
            source = function.functionNameDiagnosticSource(),
            factory = CfirErrors.STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE,
            a = function.name,
        )
    }

    private fun CheckerContext.closestContainingTypeDeclaration() =
        findClosestDeclaration<org.cangnova.cangjie.cfir.declarations.CfirDeclaration> { declaration ->
            declaration is CfirClassLikeDeclaration || declaration is CfirExtend
        }

    private fun CfirNamedFunction.hasSourceModifier(token: org.cangnova.cangjie.lexer.CjKeywordToken): Boolean =
        source?.realSourceModifiers()?.modifierByToken(token) != null
}

/**
 * 函数返回类型推断检查器
 *
 * 对齐 C++ DiagKind::sema_unable_to_infer_return_type
 */
object CfirFunctionReturnTypeInferenceChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirFunction) {
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is CfirErrorTypeRef && returnTypeRef.delegatedTypeRef == null) {
            if (declaration is CfirNamedFunction && declaration.body != null) {
                reporter.reportOn(
                    source = declaration.functionNameDiagnosticSource(),
                    factory = CfirErrors.UNABLE_TO_INFER_RETURN_TYPE,
                )
            }
        }
    }
}

/**
 * 默认参数限制检查器
 *
 * 对齐 C++ DiagKind::sema_cannot_have_default_param (Diags.cpp:414):
 * operator / foreign / open / abstract 函数不能有默认参数。
 */
object CfirDefaultParameterChecker : CfirSimpleFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        val hasDefaultParam = declaration.valueParameters.any { it.defaultValue != null }
        if (!hasDefaultParam) return

        val kind = when {
            declaration.status.isOperator -> "operator overloading"
            declaration.status.isForeign -> "foreign"
            declaration.status.isOpen -> "'open'"
            declaration.status.isAbstract -> "abstract"
            else -> return
        }
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.CANNOT_HAVE_DEFAULT_PARAM,
            a = kind,
        )
    }
}
