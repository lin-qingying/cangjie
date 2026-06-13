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
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMutationTargetClassifier
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * const 变量初始化检查。
 *
 * 官方 `ConstEvaluationChecker::ChkVarDeclAbstract` 中，`const` 变量初始化器按
 * strong const 表达式检查；const 函数体内的 `let` 由函数体 checker 以 weak const
 * 规则处理，因此这里跳过已由 const 函数体接管的局部声明，避免重复诊断。
 */
object CfirConstVariableInitializerChecker : CfirCallableDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration) {
        val variable = declaration as? CfirVariable ?: return
        if (!variable.status.isConst) return
        if (context.findClosestDeclaration<CfirFunction> { it.status.isConst } != null) return

        val initializer = variable.initializer ?: return
        context.constEvaluator().checkExpression(initializer, isWeak = false)
    }
}

/**
 * const 函数体检查。
 *
 * 对齐官方 `ChkFuncDecl`/`ChkFuncBody`：参数默认值和函数体按 weak const 检查；
 * 函数体中的 `var`、非 const 局部函数、赋值、调用等由同一个表达式递归器处理。
 */
object CfirConstFunctionBodyChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.status.isConst) return
        context.constEvaluator(thisIsConst = true, inConstInit = declaration is CfirConstructor)
            .checkFunctionBody(declaration, requireConstDeclaration = false)
    }
}

/**
 * class/struct const 聚合规则检查。
 *
 * 对齐官方 `ChkClassDecl`/`ChkStructDecl`：
 * - class 有显式 const init 时不能定义 var 字段；
 * - 没有 const init 时不能定义 const 实例成员函数；
 * - 有 const init 时，对应 static/non-static 字段初始化器必须是 strong const。
 *
 * 注意：官方 `ChkClassDeclHasConstInitNoVarMember` 只作用于 class，不作用于 struct。
 */
object CfirConstDeclarationChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass && declaration !is CfirStruct) return

        val constConstructors = declaration.declarations
            .filterIsInstance<CfirConstructor>()
            .filter { it.status.isConst }
        val hasConstConstructor = constConstructors.isNotEmpty()

        if (hasConstConstructor) {
            if (declaration is CfirClass) {
                reportClassConstInitWithVarMembers(declaration, constConstructors)
            }
            checkConstInitializers(declaration, constConstructors)
        } else {
            reportConstMemberFunctionsWithoutConstInit(declaration.declarations)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportClassConstInitWithVarMembers(
        declaration: CfirClassLikeDeclaration,
        constConstructors: List<CfirConstructor>,
    ) {
        val hasVarField = declaration.declarations.any { it is CfirFieldVariable && it.isVar }
        if (!hasVarField) return

        for (constructor in constConstructors) {
            reporter.reportOn(
                source = constructor.constructorNameDiagnosticSource(includeConstKeyword = true),
                factory = CfirErrors.CLASS_CONST_INIT_WITH_VAR,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConstInitializers(
        declaration: CfirClassLikeDeclaration,
        constConstructors: List<CfirConstructor>,
    ) {
        val hasStaticConstInit = constConstructors.any { it.status.isStatic }
        val hasNonStaticConstInit = constConstructors.any { !it.status.isStatic }
        if (!hasStaticConstInit && !hasNonStaticConstInit) return

        val evaluator = context.constEvaluator(owner = declaration)
        for (field in declaration.declarations.filterIsInstance<CfirFieldVariable>()) {
            if (field.status.isConst) continue
            val initializer = field.initializer ?: continue
            if ((hasStaticConstInit && field.status.isStatic) || (hasNonStaticConstInit && !field.status.isStatic)) {
                evaluator.checkExpression(initializer, isWeak = false)
            }
        }
    }
}

/**
 * extend 中 const 实例函数的 const init 约束。
 *
 * 官方 `ChkExtendDecl` 会解析被扩展的 class/struct：目标没有 const init 时，
 * extend 内不能定义 const 实例成员函数。
 */
object CfirConstExtendDeclarationChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        val target = CfirExtendSemantics.targetDeclaration(context, declaration) ?: return
        if (target !is CfirClass && target !is CfirStruct) return
        if (target.declarations.any { it is CfirConstructor && it.status.isConst }) return

        reportConstMemberFunctionsWithoutConstInit(declaration.declarations)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportConstMemberFunctionsWithoutConstInit(declarations: List<CfirDeclaration>) {
    for (function in declarations.filterIsInstance<CfirNamedFunction>()) {
        if (!function.status.isConst || function.status.isStatic) continue
        reporter.reportOn(
            source = function.functionNameDiagnosticSource(),
            factory = CfirErrors.NO_CONST_INIT,
        )
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun CheckerContext.constEvaluator(
    owner: CfirClassLikeDeclaration? = findClosestDeclaration(),
    thisIsConst: Boolean = findClosestDeclaration<CfirFunction>()?.status?.isConst == true,
    inConstInit: Boolean = findClosestDeclaration<CfirConstructor>()?.status?.isConst == true,
): CfirConstExpressionEvaluator =
    CfirConstExpressionEvaluator(
        context = context,
        reporter = reporter,
        owner = owner,
        thisIsConst = thisIsConst,
        inConstInit = inConstInit,
    )

private class CfirConstExpressionEvaluator(
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val owner: CfirClassLikeDeclaration?,
    private val thisIsConst: Boolean,
    private val inConstInit: Boolean,
    private val weakSymbols: MutableSet<CfirBasedSymbol<*>> = linkedSetOf(),
) {
    private val memberFieldSymbols: Set<CfirBasedSymbol<*>> =
        owner?.declarations
            ?.filterIsInstance<CfirFieldVariable>()
            ?.filterNot { it.status.isStatic }
            ?.mapTo(linkedSetOf()) { it.symbol }
            .orEmpty()
    private val memberFieldSymbolIds: Set<CallableId> =
        memberFieldSymbols.mapNotNullTo(linkedSetOf()) { it.callableIdOrNull() }

    private val instanceMemberSymbols: Set<CfirBasedSymbol<*>> =
        owner?.declarations
            ?.filterIsInstance<CfirCallableDeclaration>()
            ?.filterNot { it.status.isStatic }
            ?.mapTo(linkedSetOf()) { it.symbol }
            .orEmpty()
    private val instanceMemberSymbolIds: Set<CallableId> =
        instanceMemberSymbols.mapNotNullTo(linkedSetOf()) { it.callableIdOrNull() }

    private val weakSymbolIds: MutableSet<CallableId> =
        weakSymbols.mapNotNullTo(linkedSetOf()) { it.callableIdOrNull() }

    fun checkFunctionBody(
        function: CfirFunction,
        requireConstDeclaration: Boolean,
    ): Boolean {
        if (requireConstDeclaration && !function.status.isConst) {
            reportExpectFunction(function)
            weakSymbols += function.symbol
            return false
        }

        collectWeakSymbols(function)
        var result = true
        for (parameter in function.valueParameters) {
            val defaultValue = parameter.defaultValue ?: continue
            result = checkExpression(defaultValue, isWeak = true, allowReturn = false) && result
        }
        val body = function.body
        if (body != null) {
            result = checkBlock(body, isWeak = true, allowReturn = true) && result
        }
        return result
    }

    fun checkExpression(
        expression: CfirExpression,
        isWeak: Boolean,
        allowReturn: Boolean = false,
    ): Boolean = when (expression) {
        is CfirErrorExpression,
        is CfirLazyExpression -> false

        is CfirLiteralExpression -> checkLiteralExpression(expression, isWeak)
        is CfirStringInterpolation -> {
            reportExpectExpression(expression, isWeak = false)
            false
        }

        is CfirArrayLiteral -> checkArrayLiteral(expression, isWeak, allowReturn)
        is CfirTupleLiteral -> checkExpressions(expression.elements, isWeak, allowReturn)
        is CfirThisReceiverExpression,
        is CfirSuperReceiverExpression -> true

        is CfirWrappedExpression -> checkExpression(expression.expression, isWeak, allowReturn)
        is CfirTypeOperator -> checkExpression(expression.argument, isWeak, allowReturn)
        is CfirTypeConversion -> checkExpression(expression.argument, isWeak, allowReturn)
        is CfirBinaryOp -> checkExpression(expression.left, isWeak, allowReturn) and
                checkExpression(expression.right, isWeak, allowReturn)

        is CfirComparisonExpression -> checkExpression(expression.left, isWeak, allowReturn) and
                checkExpression(expression.right, isWeak, allowReturn)

        is CfirRangeExpression -> {
            var result = checkExpression(expression.start, isWeak, allowReturn)
            result = checkExpression(expression.end, isWeak, allowReturn) && result
            expression.step?.let { result = checkExpression(it, isWeak, allowReturn) && result }
            result
        }

        is CfirReturnExpression -> checkReturnExpression(expression, isWeak, allowReturn)
        is CfirThrowExpression -> checkExpression(expression.exception, isWeak, allowReturn)
        is CfirPerformExpression -> checkExpression(expression.expression, isWeak, allowReturn)
        is CfirIfExpression -> checkIfExpression(expression, isWeak, allowReturn)
        is CfirMatchExpression -> checkMatchExpression(expression, isWeak, allowReturn)
        is CfirTryExpression -> checkTryExpression(expression, isWeak, allowReturn)
        is CfirFunctionCall -> checkFunctionCall(expression, isWeak, allowReturn)
        is CfirNamedAccessExpression -> checkQualifiedAccess(expression, isWeak)
        is CfirQualifiedAccessExpression -> checkQualifiedAccess(expression, isWeak)
        is CfirSubscriptExpression -> checkSubscriptExpression(expression, isWeak, allowReturn)
        is CfirAssignment -> checkAssignment(expression, isWeak, allowReturn)
        is CfirIncrementDecrementExpression -> checkIncrementDecrementExpression(expression, isWeak)
        is CfirAnonymousFunctionExpression -> childForFunctionBody().checkFunctionBody(
            expression.anonymousFunction,
            requireConstDeclaration = false,
        )

        is CfirBlock -> checkBlock(expression, isWeak, allowReturn)
        is CfirUnsafeExpression,
        is CfirSpawnExpression,
        is CfirSynchronizedExpression,
        is CfirLoopExpression,
        is CfirForInExpression,
        is CfirBreakExpression,
        is CfirContinueExpression -> {
            reportExpectExpression(expression, isWeak)
            false
        }

        is CfirOptionalExpression,
        is CfirOptionalChainExpression -> checkExpression(expression.expression, isWeak, allowReturn)

        else -> {
            reportExpectExpression(expression, isWeak)
            false
        }
    }

    private fun checkLiteralExpression(expression: CfirLiteralExpression, isWeak: Boolean): Boolean {
        if (expression.coneTypeOrNull.isStdArray()) {
            reportExpectExpression(expression, isWeak)
            return false
        }
        return true
    }

    private fun checkArrayLiteral(
        expression: CfirArrayLiteral,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        if (expression.coneTypeOrNull.isStdArray()) {
            reportExpectExpression(expression, isWeak)
            return false
        }
        return checkExpressions(expression.elements, isWeak, allowReturn)
    }

    private fun checkReturnExpression(
        expression: CfirReturnExpression,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        if (!allowReturn) {
            reportExpectExpression(expression, isWeak)
            return false
        }
        return checkExpression(expression.result, isWeak, allowReturn)
    }

    private fun checkIfExpression(
        expression: CfirIfExpression,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        var result = checkExpression(expression.condition, isWeak, allowReturn)
        result = checkBlock(expression.thenBranch, isWeak, allowReturn) && result
        expression.elseBranch?.let { result = checkExpression(it, isWeak, allowReturn) && result }
        return result
    }

    private fun checkMatchExpression(
        expression: CfirMatchExpression,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        var result = true
        expression.subject?.let { result = checkExpression(it, isWeak, allowReturn) && result }
        for (branch in expression.branches) {
            branch.guard?.let { result = checkExpression(it, isWeak, allowReturn) && result }
            result = checkBlock(branch.body, isWeak, allowReturn) && result
        }
        return result
    }

    private fun checkTryExpression(
        expression: CfirTryExpression,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        if (expression.resources.isNotEmpty()) {
            reportExpectExpression(expression, isWeak)
            return false
        }

        var result = checkBlock(expression.tryBlock, isWeak, allowReturn)
        for (handler in expression.handlers) {
            result = checkBlock(handler.body, isWeak, allowReturn) && result
        }
        for (catchClause in expression.catches) {
            result = checkBlock(catchClause.body, isWeak, allowReturn) && result
        }
        expression.finallyBlock?.let { result = checkBlock(it, isWeak, allowReturn) && result }
        return result
    }

    private fun checkFunctionCall(
        expression: CfirFunctionCall,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        if (expression.isStringBinaryOperatorCall()) {
            var result = expression.explicitReceiver?.let { checkExpression(it, isWeak, allowReturn) } ?: true
            result = checkExpressions(expression.argumentList.arguments, isWeak, allowReturn) && result
            return result
        }
        if (expression.resolvedSymbolOrNull()?.isBound != true) {
            expression.explicitReceiver?.let { checkExpression(it, isWeak, allowReturn) }
            expression.dispatchReceiver?.let { checkExpression(it, isWeak, allowReturn) }
            return false
        }
        if (!checkQualifiedAccess(expression, isWeak)) return false
        return checkExpressions(expression.argumentList.arguments, isWeak, allowReturn)
    }

    private fun checkQualifiedAccess(
        expression: CfirQualifiedAccessExpression,
        isWeak: Boolean,
    ): Boolean {
        val target = expression.resolvedSymbolOrNull() ?: return false
        if (!target.isBound) return false

        val receiver = expression.explicitReceiver ?: expression.dispatchReceiver
        if (receiver != null) {
            if (isStaticConstAccess(receiver, target)) return true

            if (!checkReceiverForMemberAccess(receiver, isWeak)) {
                if (receiver is CfirThisReceiverExpression || receiver is CfirSuperReceiverExpression) {
                    reportExpectExpression(expression.nonConstReceiverDiagnosticExpression(receiver), isWeak)
                }
                return false
            }
            if (target.isConstMemberOnConstReceiver()) {
                return true
            }
            reportExpectExpression(expression, isWeak)
            return false
        }

        if (target.requiresConstReceiver() && !thisIsConst) {
            reportExpectExpression(expression, isWeak)
            return false
        }

        if (target.isConstReference()) return true
        if (target.isMemberFieldSymbol() && thisIsConst) return true
        if (isWeak && target.isWeakConstSymbol()) return true

        reportExpectExpression(expression, isWeak)
        return false
    }

    private fun checkReceiverForMemberAccess(
        receiver: CfirExpression,
        isWeak: Boolean,
    ): Boolean {
        if (receiver is CfirThisReceiverExpression || receiver is CfirSuperReceiverExpression) {
            return thisIsConst
        }
        return checkExpression(receiver, isWeak)
    }

    private fun CfirQualifiedAccessExpression.nonConstReceiverDiagnosticExpression(
        receiver: CfirExpression,
    ): CfirExpression {
        if (receiver is CfirThisReceiverExpression && receiver.calleeReference.isImplicit) return this
        return receiver.takeIf { it.source != null } ?: this
    }

    private fun checkSubscriptExpression(
        expression: CfirSubscriptExpression,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        val receiverType = expression.receiver.coneTypeOrNull
        if (!receiverType.isTupleOrVArray()) {
            reportExpectExpression(expression, isWeak)
            return false
        }

        var result = checkExpression(expression.receiver, isWeak, allowReturn)
        for (index in expression.indices) {
            result = checkExpression(index, isWeak, allowReturn) && result
        }
        return result
    }

    private fun checkAssignment(
        expression: CfirAssignment,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        if (assignmentTargetDiagnosticOwns(expression)) {
            return false
        }

        val target = expression.lValue.resolvedSymbolOrNull()
        if (inConstInit && target?.isMemberFieldSymbol() == true) {
            return checkExpression(expression.rValue, isWeak, allowReturn)
        }

        checkMutatingTargetAccess(expression.lValue, isWeak, allowReturn)?.let { isConstTargetAccess ->
            if (!isConstTargetAccess) return false
        }
        reportExpectExpression(expression, isWeak)
        return false
    }

    private fun assignmentTargetDiagnosticOwns(expression: CfirAssignment): Boolean {
        val access = expression.lValue as? CfirQualifiedAccessExpression ?: return false
        val assignmentTarget = context(context) {
            CfirMutationTargetClassifier.classifyAssignment(access, expression)
        }
        return when (assignmentTarget) {
            is CfirMutationTargetClassifier.MutationTarget.ImmutableValue,
            is CfirMutationTargetClassifier.MutationTarget.NonAssignableName,
                -> true

            CfirMutationTargetClassifier.MutationTarget.Assignable,
            null,
                -> false
        }
    }

    private fun checkIncrementDecrementExpression(
        expression: CfirIncrementDecrementExpression,
        isWeak: Boolean,
    ): Boolean {
        val target = expression.expression.resolvedSymbolOrNull()
        if (inConstInit && target?.isMemberFieldSymbol() == true) {
            return true
        }

        checkMutatingTargetAccess(expression.expression, isWeak, allowReturn = false)?.let { isConstTargetAccess ->
            if (!isConstTargetAccess) return false
        }
        reportExpectExpression(expression, isWeak = true)
        return false
    }

    /**
     * 赋值、自增自减等修改表达式在官方前端会先按 desugared 访问链检查接收者。
     *
     * 当 `array[0]++` / `array[1] = a` 这类表达式的被修改对象本身不是 const 时，
     * 诊断归属在访问链的 receiver，而不是外层 mutation 节点。
     */
    private fun checkMutatingTargetAccess(
        expression: CfirExpression,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean? = when (expression) {
        is CfirSubscriptExpression -> {
            var result = checkExpression(expression.receiver, isWeak, allowReturn)
            for (index in expression.indices) {
                result = checkExpression(index, isWeak, allowReturn) && result
            }
            result
        }

        is CfirQualifiedAccessExpression -> {
            val receiver = expression.explicitReceiver ?: expression.dispatchReceiver
            receiver?.let { checkReceiverForMemberAccess(it, isWeak) }
        }

        is CfirWrappedExpression -> checkMutatingTargetAccess(expression.expression, isWeak, allowReturn)
        is CfirTypeOperator -> checkMutatingTargetAccess(expression.argument, isWeak, allowReturn)
        is CfirTypeConversion -> checkMutatingTargetAccess(expression.argument, isWeak, allowReturn)
        else -> null
    }

    private fun checkBlock(
        block: CfirBlock,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        var result = true
        for (statement in block.statements) {
            result = checkStatement(statement, isWeak, allowReturn) && result
        }
        return result
    }

    private fun checkStatement(
        statement: CfirStatement,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean = when (statement) {
        is CfirVariable -> checkVariableDeclaration(statement)
        is CfirFunction -> checkFunctionBody(statement, requireConstDeclaration = true)
        is CfirExpression -> checkExpression(statement, isWeak, allowReturn)
        else -> true
    }

    private fun checkVariableDeclaration(variable: CfirVariable): Boolean {
        if (variable.isVar) {
            context(context) {
                reporter.reportOn(
                    source = variable.source,
                    factory = CfirErrors.CANNOT_DEFINE_VAR_IN_CONST_FUNCTION,
                )
            }
            addWeakVariable(variable)
            return false
        }

        addWeakVariable(variable)
        val initializer = variable.initializer ?: return true
        return checkExpression(initializer, isWeak = !variable.status.isConst, allowReturn = false)
    }

    private fun checkExpressions(
        expressions: List<CfirExpression>,
        isWeak: Boolean,
        allowReturn: Boolean,
    ): Boolean {
        var result = true
        for (expression in expressions) {
            result = checkExpression(expression, isWeak, allowReturn) && result
        }
        return result
    }

    private fun collectWeakSymbols(function: CfirFunction) {
        function.valueParameters.forEach { addWeakSymbol(it.symbol) }
        function.body?.statements?.forEach(::collectWeakSymbols)
    }

    private fun collectWeakSymbols(statement: CfirStatement) {
        when (statement) {
            is CfirVariable -> addWeakVariable(statement)
            is CfirFunction -> {
                addWeakSymbol(statement.symbol)
                statement.body?.statements?.forEach(::collectWeakSymbols)
            }

            is CfirBlock -> statement.statements.forEach(::collectWeakSymbols)
            is CfirIfExpression -> {
                collectWeakSymbols(statement.thenBranch)
                (statement.elseBranch as? CfirStatement)?.let(::collectWeakSymbols)
            }

            is CfirMatchExpression -> statement.branches.forEach { branch ->
                branch.pattern.bindingVariables().forEach { addWeakSymbol(it.symbol) }
                collectWeakSymbols(branch.body)
            }

            is CfirTryExpression -> {
                collectWeakSymbols(statement.tryBlock)
                statement.handlers.forEach { collectWeakSymbols(it.body) }
                statement.catches.forEach { collectWeakSymbols(it.body) }
                statement.finallyBlock?.let(::collectWeakSymbols)
            }
        }
    }

    private fun addWeakVariable(variable: CfirVariable) {
        addWeakSymbol(variable.symbol)
        if (variable is CfirPatternVariable) {
            variable.pattern.bindingVariables().forEach { addWeakSymbol(it.symbol) }
        }
    }

    private fun addWeakSymbol(symbol: CfirBasedSymbol<*>) {
        weakSymbols += symbol
        symbol.callableIdOrNull()?.let { weakSymbolIds += it }
    }

    private fun childForFunctionBody(): CfirConstExpressionEvaluator =
        CfirConstExpressionEvaluator(
            context = context,
            reporter = reporter,
            owner = owner,
            thisIsConst = true,
            inConstInit = false,
            weakSymbols = linkedSetOf(),
        )

    private fun reportExpectFunction(function: CfirFunction) {
        context(context) {
            reporter.reportOn(
                source = function.expectConstFunctionSource(),
                factory = CfirErrors.EXPECT_CONST,
                a = "function",
            )
        }
    }

    private fun reportExpectExpression(expression: CfirExpression, isWeak: Boolean) {
        context(context) {
            reporter.reportOn(
                source = expression.expectConstDiagnosticSource(),
                factory = CfirErrors.EXPECT_CONST,
                a = if (isWeak) "expression" else "expression guaranteed to be evaluated at compile time",
            )
        }
    }

    private fun isStaticConstAccess(receiver: CfirExpression, target: CfirBasedSymbol<*>): Boolean {
        val receiverSymbol = receiver.resolvedSymbolOrNull()
        if (receiverSymbol !is CfirClassLikeSymbol<*> && receiverSymbol !is CfirFileSymbol) return false
        if (target is CfirEnumConstructorSymbol) return true
        val callable = target as? CfirCallableSymbol<*> ?: return false
        return callable.cfir.status.isConst && callable.cfir.status.isStatic
    }

    private fun CfirBasedSymbol<*>.isConstMemberOnConstReceiver(): Boolean = when (this) {
        is CfirEnumConstructorSymbol -> true
        is CfirVariableSymbol<*> -> true
        is CfirFunctionSymbol<*> -> cfir.status.isConst
        else -> false
    }

    private fun CfirBasedSymbol<*>.isConstReference(): Boolean = when (this) {
        is CfirEnumConstructorSymbol -> true
        is CfirCallableSymbol<*> -> cfir.status.isConst
        else -> false
    }

    private fun CfirBasedSymbol<*>.isMemberFieldSymbol(): Boolean =
        this in memberFieldSymbols || callableIdOrNull()?.let { it in memberFieldSymbolIds } == true

    private fun CfirBasedSymbol<*>.isOwnerInstanceMemberSymbol(): Boolean =
        this in instanceMemberSymbols || callableIdOrNull()?.let { it in instanceMemberSymbolIds } == true

    private fun CfirBasedSymbol<*>.isWeakConstSymbol(): Boolean =
        this in weakSymbols || callableIdOrNull()?.let { it in weakSymbolIds } == true

    private fun CfirBasedSymbol<*>.requiresConstReceiver(): Boolean {
        if (this is CfirConstructorSymbol || this is CfirEnumConstructorSymbol) return false
        if (isOwnerInstanceMemberSymbol()) return true
        val callable = this as? CfirCallableSymbol<*> ?: return false
        if (callable.cfir.status.isStatic) return false
        if (this !is CfirFieldVariableSymbol && this !is CfirPropertySymbol && this !is CfirFunctionSymbol<*>) {
            return false
        }
        return callable.cfir.dispatchReceiverType != null
    }
}

private fun CfirFunctionCall.isStringBinaryOperatorCall(): Boolean {
    if (origin != CfirFunctionCallOrigin.Operator) return false
    val receiver = explicitReceiver ?: return false
    if (!receiver.coneTypeOrNull.isStdString()) return false
    if (argumentList.arguments.isEmpty()) return false
    val target = resolvedSymbolOrNull() as? CfirCallableSymbol<*> ?: return false
    return target.callableId.packageName == StdlibClassIds.String.packageFqName
}

private fun CfirFunction.expectConstFunctionSource(): AbstractCjSourceElement? =
    when (this) {
        is CfirNamedFunction -> functionNameDiagnosticSource()
        is CfirConstructor -> constructorNameDiagnosticSource()
        else -> source
    }

private fun CfirQualifiedAccessExpression.resolvedSymbolOrNull(): CfirBasedSymbol<*>? =
    when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    }

private fun CfirExpression.resolvedSymbolOrNull(): CfirBasedSymbol<*>? =
    (this as? CfirQualifiedAccessExpression)?.resolvedSymbolOrNull()

private fun CfirExpression.expectConstDiagnosticSource(): AbstractCjSourceElement? =
    (this as? CfirQualifiedAccessExpression)?.calleeReference?.source ?: source

private fun CfirBasedSymbol<*>.callableIdOrNull(): CallableId? =
    (this as? CfirCallableSymbol<*>)?.callableId

private fun ConeCangJieType?.isStdArray(): Boolean {
    if (this == null) return false
    if (this is ConeVArrayType) return false
    return classIdOrPrimitiveClassId == StdlibClassIds.Array
}

private fun ConeCangJieType?.isStdString(): Boolean {
    if (this == null) return false
    return classIdOrPrimitiveClassId == StdlibClassIds.String
}

private fun ConeCangJieType?.isTupleOrVArray(): Boolean {
    if (this == null) return false
    return this is ConeTupleType || this is ConeVArrayType
}
