/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.hasInvalidDeclaredUpperBoundsInCurrentContext
import org.cangnova.cangjie.cfir.analysis.checkers.modifierByToken
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.ConeNoSmallestCommonSupertypeDiagnostic
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/**
 * 函数语义检查器（Function 分组）
 *
 * 检查 static 函数重载冲突（同名函数不能混合 static 和 non-static）。
 * 对齐 C++ TypeChecker 中 sema_static_function_overload_conflicts 检查。
 */
object CfirFunctionOverloadChecker : CfirSimpleFunctionChecker() {
    /**
     * 检查单个命名函数的 static / non-static 重载冲突。
     */
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
        if (!function.status.isStatic) return

        val ownerDeclarations = functionOwnerDeclarations() ?: return
        val functionName = function.name

        val firstNonStatic = ownerDeclarations
            .filterIsInstance<CfirNamedFunction>()
            .firstOrNull { sibling ->
                sibling.name == functionName &&
                    !sibling.status.isStatic
            }

        if (firstNonStatic != null) {
            reporter.reportOn(
                source = function.source,
                factory = CfirErrors.STATIC_FUNCTION_OVERLOAD_CONFLICTS,
                a = functionName,
            )
        }
    }

    /**
     * static/non-static 重载冲突按声明作用域分组。
     *
     * extend 成员不是被扩展类型的物理 class member，不能通过 callableId.classId 回查 owner；
     * 这里使用 checker 上下文中的最近类型/extend 声明，和官方 PreCheck 的 scopeName 分组一致。
     */
    context(context: CheckerContext)
    private fun functionOwnerDeclarations(): List<CfirDeclaration>? {
        val owner = context.findClosestDeclaration<CfirDeclaration> { declaration ->
            declaration is CfirClassLikeDeclaration || declaration is CfirExtend
        } ?: return null

        return when (owner) {
            is CfirClassLikeDeclaration -> owner.declarations
            is CfirExtend -> owner.declarations
            else -> null
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
    /**
     * 检查命名函数声明状态组合的合法性。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        checkMutFunction(declaration)
        checkStaticFunctionStatus(declaration)
    }

    /**
     * 检查 `mut func` 的声明位置。
     *
     * `mut` 函数只允许出现在 struct / interface 及 struct 的扩展内（官方 mut.md）；
     * class / enum 成员由通用 modifier checker 以 WRONG_MODIFIER_TARGET 报告
     * （对应官方 `parse_illegal_modifier_in_scope`），此处不重复。extend 的非 struct
     * 目标（class / 原始类型 / enum）由官方 `sema_invalid_mut_modifier_extend_of_struct`
     * 管辖，映射为 MUT_ONLY_ON_FUNCTION，报告锚定 `mut` 关键字（与官方位置一致）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMutFunction(function: CfirNamedFunction) {
        if (!function.status.isMut) return
        if (function.isLocal) return
        val containingDeclaration = context.closestContainingTypeDeclaration()
        if (containingDeclaration is CfirStruct) return
        if (containingDeclaration is CfirInterface) return
        if (containingDeclaration is CfirExtend) {
            val target = CfirExtendSemantics.targetDeclaration(context, containingDeclaration)
            if (target is CfirStruct) return
            val mutSource = function.source?.realSourceModifiers()?.modifierByToken(CjTokens.MUT_KEYWORD)?.source
            reporter.reportOn(
                source = mutSource ?: function.functionNameDiagnosticSource(),
                factory = CfirErrors.MUT_ONLY_ON_FUNCTION,
                a = function.name,
            )
            return
        }
        // class / enum 成员的源码显式 `mut` 由通用 modifier checker 诊断，
        // 这里只覆盖无源码 mut 修饰符的隐式 mut 状态（如接口实现继承），避免重复报告。
        if (function.hasSourceModifier(CjTokens.MUT_KEYWORD)) return

        reporter.reportOn(
            source = function.functionNameDiagnosticSource(),
            factory = CfirErrors.MUT_ONLY_ON_FUNCTION,
            a = function.name,
        )
    }

    /**
     * 检查 static 函数不能同时承载实例分派状态。
     *
     * 只在冲突状态来自非源码修饰符或尚未由 modifier checker 成对处理时报告函数名级诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticFunctionStatus(function: CfirNamedFunction) {
        if (!function.status.isStatic) return
        val conflictingStatusModifier = when {
            function.status.isOpen && function.hasSourceModifier(CjTokens.OPEN_KEYWORD) -> CjTokens.OPEN_KEYWORD
            function.status.isAbstract && function.hasSourceModifier(CjTokens.ABSTRACT_KEYWORD) -> CjTokens.ABSTRACT_KEYWORD
            function.status.isOverride && function.hasSourceModifier(CjTokens.OVERRIDE_KEYWORD) -> CjTokens.OVERRIDE_KEYWORD
            function.status.isOperator && function.hasSourceModifier(CjTokens.OPERATOR_KEYWORD) -> CjTokens.OPERATOR_KEYWORD
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

    /**
     * 取得当前函数最近的类型或 extend 容器声明。
     */
    private fun CheckerContext.closestContainingTypeDeclaration() =
        findClosestDeclaration<CfirDeclaration> { declaration ->
            declaration is CfirClassLikeDeclaration || declaration is CfirExtend
        }

    /**
     * 判断函数源码上是否显式出现指定关键字修饰符。
     */
    private fun CfirNamedFunction.hasSourceModifier(token: org.cangnova.cangjie.lexer.CjKeywordToken): Boolean =
        source?.realSourceModifiers()?.modifierByToken(token) != null
}

/**
 * 函数返回类型推断检查器
 *
 * 对齐 C++：
 * - `DiagKind::sema_unable_to_infer_return_type`（递归/其他推断失败，位置=函数名）
 * - `DiagKind::sema_incompatible_func_body_and_return_type`（Join 失败，位置=尾表达式）
 */
object CfirFunctionReturnTypeInferenceChecker : CfirFunctionChecker() {
    /**
     * 检查函数返回类型是否因真正的推断失败而保留错误类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is CfirErrorTypeRef && returnTypeRef.isFunctionReturnTypeInferenceFailure()) {
            if (declaration.hasInvalidDeclaredUpperBoundsInCurrentContext()) {
                return
            }
            if (declaration is CfirNamedFunction && declaration.body != null) {
                // Join 失败（对齐官方 sema_incompatible_func_body_and_return_type）：
                // 错误类型 ref 的 source 由推断层指向 body 尾语句（CalcFuncRetTyFromBody 的 *lastNode）。
                if (returnTypeRef.diagnostic is ConeNoSmallestCommonSupertypeDiagnostic) {
                    reporter.reportOn(
                        source = returnTypeRef.source ?: declaration.functionNameDiagnosticSource(),
                        factory = CfirErrors.INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE,
                    )
                } else {
                    // 递归/其他推断失败（对齐官方 sema_unable_to_infer_return_type，位置=函数名）。
                    reporter.reportOn(
                        source = declaration.functionNameDiagnosticSource(),
                        factory = CfirErrors.UNABLE_TO_INFER_RETURN_TYPE,
                    )
                }
            }
        }
    }

    /**
     * 只消费返回类型推断自身产生的错误。
     *
     * 函数体尾表达式若是 ambiguous / unresolved / inapplicable call，原始错误已由
     * ErrorNodeDiagnosticCollectorComponent 或 CfirExpressionWithErrorTypeChecker 按调用点报告；
     * 这里不能把这类表达式错误再提升成函数名级 `UNABLE_TO_INFER_RETURN_TYPE`。
     */
    private fun CfirErrorTypeRef.isFunctionReturnTypeInferenceFailure(): Boolean {
        if (delegatedTypeRef != null) return false
        return when (val diagnostic = diagnostic) {
            is ConeNoSmallestCommonSupertypeDiagnostic -> true
            is ConeSimpleDiagnostic -> when (diagnostic.kind) {
                DiagnosticKind.InferenceError,
                DiagnosticKind.RecursionInImplicitTypes,
                -> true

                else -> false
            }
            else -> false
        }
    }
}

/**
 * finalizer 声明语义检查器。
 *
 * 对齐官方 C++:
 * - `DeclAttributeChecker::CheckFuncDeclAttributes` 中 `sema_finalizer_forbidden_in_class`
 * - `TypeChecker::CheckFinalizer` 中 `sema_forbid_generic_finalizer` / `sema_cannot_currying`
 */
object CfirFinalizerDeclarationChecker : CfirFunctionChecker() {
    /**
     * 检查 finalizer 声明的所有声明级限制。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        val finalizer = declaration as? CfirFinalizer ?: return

        checkFinalizerInInheritableClass(finalizer)
        checkGenericFinalizer(finalizer)
        checkCurriedFinalizer(finalizer)
    }

    /**
     * 检查 open / abstract class 中禁止声明 finalizer 的规则。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFinalizerInInheritableClass(finalizer: CfirFinalizer) {
        val owner = context.findClosestDeclaration<CfirClass>() ?: return
        val classKind = when {
            owner.status.isOpen -> "open"
            owner.status.isAbstract -> "abstract"
            else -> return
        }

        reporter.reportOn(
            source = finalizer.tildeSource(),
            factory = CfirErrors.FINALIZER_FORBIDDEN_IN_CLASS,
            a = owner.name,
            b = classKind,
        )
    }

    /**
     * 检查 finalizer 不能声明类型参数。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGenericFinalizer(finalizer: CfirFinalizer) {
        if (finalizer.typeParameters.isEmpty()) return

        reporter.reportOn(
            source = finalizer.tildeSource(),
            factory = CfirErrors.FORBID_GENERIC_FINALIZER,
            a = SpecialNames.END_INIT,
        )
    }

    /**
     * 检查 finalizer 不能使用多参数列表柯里化形式。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCurriedFinalizer(finalizer: CfirFinalizer) {
        val parameterLists = finalizer.attributes.functionBodyDiagnosticData?.valueParameterLists.orEmpty()
        if (parameterLists.size <= 1) return

        reporter.reportOn(
            source = parameterLists.first().source.leftParenthesisSource(),
            factory = CfirErrors.CANNOT_CURRYING,
            a = "finalizer",
        )
    }

    /**
     * 将参数列表 source 收窄到左括号首字符。
     */
    private fun AbstractCjSourceElement.leftParenthesisSource(): CjOffsetsOnlySourceElement {
        return CjOffsetsOnlySourceElement(
            startOffset = startOffset,
            endOffset = minOf(startOffset + 1, endOffset),
        )
    }

    /**
     * 取得 finalizer 的 `~` 首字符诊断 source。
     */
    private fun CfirFinalizer.tildeSource(): AbstractCjSourceElement? {
        val source = source ?: return null
        return CjOffsetsOnlySourceElement(
            startOffset = source.startOffset,
            endOffset = minOf(source.startOffset + 1, source.endOffset),
        )
    }
}

/**
 * 属性访问器声明语义检查器。
 *
 * 对齐官方 C++ `TypeChecker::TypeCheckerImpl::Synthesize(PropDecl&)`：
 * - getter 不能声明参数：`sema_cannot_have_parameter`
 * - getter / setter 不能拥有多个参数列表：`sema_cannot_currying`
 */
object CfirPropertyAccessorDeclarationChecker : CfirPropertyAccessorChecker() {
    /**
     * 检查单个属性访问器声明的参数列表限制。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirPropertyAccessor) {
        val declarationKind = if (declaration.isGetter) "getter" else "setter"

        if (declaration.isGetter && declaration.valueParameters.isNotEmpty()) {
            reporter.reportOn(
                source = declaration.accessorKeywordSource(),
                factory = CfirErrors.CANNOT_HAVE_PARAMETER,
                a = declarationKind,
            )
        }

        val parameterLists = declaration.attributes.functionBodyDiagnosticData?.valueParameterLists.orEmpty()
        if (parameterLists.size > 1) {
            reporter.reportOn(
                source = declaration.accessorKeywordSource(),
                factory = CfirErrors.CANNOT_CURRYING,
                a = declarationKind,
            )
        }
    }

    /**
     * 取得 getter/setter 关键字首字符诊断 source。
     */
    private fun CfirPropertyAccessor.accessorKeywordSource(): AbstractCjSourceElement? {
        val source = source ?: return null
        return CjOffsetsOnlySourceElement(
            startOffset = source.startOffset,
            endOffset = minOf(source.startOffset + 1, source.endOffset),
        )
    }
}

/**
 * 默认参数限制检查器
 *
 * 对齐 C++ DiagKind::sema_cannot_have_default_param (Diags.cpp:414):
 * operator / foreign / open / abstract 函数不能有默认参数。
 */
object CfirDefaultParameterChecker : CfirSimpleFunctionChecker() {
    /**
     * 检查函数默认参数是否出现在被禁止的函数种类上。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirNamedFunction) {
        val defaultParameters = declaration.valueParameters.filter { it.defaultValue != null }
        if (defaultParameters.isEmpty()) return
        val ownerIsInterface = context.findClosestDeclaration<CfirInterface>() != null
        val kind = when {
            declaration.status.isOperator -> "operator overloading"
            declaration.status.isForeign -> "foreign"
            declaration.status.isOpen -> "'open'"
            declaration.status.isAbstract || ownerIsInterface -> "abstract"
            else -> return
        }
        for (parameter in defaultParameters) {
            reporter.reportOn(
                source = parameter.source ?: declaration.source,
                factory = CfirErrors.CANNOT_HAVE_DEFAULT_PARAM,
                a = kind,
            )
        }
    }
}
