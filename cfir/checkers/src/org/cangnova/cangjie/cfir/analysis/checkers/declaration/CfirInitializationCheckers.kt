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

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.primaryBindingNameOrNull
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.name.Name

/**
 * 初始化语义不是解析器职责，而是 definite assignment 风格的后置语义检查。
 *
 * 这里抽出一个可复用的初始化流分析器，统一服务于：
 * 1. 函数/构造器体里“变量在初始化前被读取”；
 * 2. 类体字段初始化表达式的声明顺序检查；
 * 3. 构造器结束时“仍有实例字段未初始化”。
 */
private class CfirInitializationFlowAnalyzer(
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val reportReadDiagnostics: Boolean = true,
    private val initializationAssignments: MutableSet<CfirAssignment>? = null,
) {
    fun checkFunction(function: CfirFunction) {
        val body = function.body ?: return
        val owner = if (function is CfirConstructor) {
            context.findClosestDeclaration<CfirClassLikeDeclaration>()
        } else {
            null
        }
        analyzeFunctionBody(function, body, owner)
    }

    fun collectInitializationAssignments(function: CfirFunction): Set<CfirAssignment> {
        val body = function.body ?: return emptySet()
        val owner = if (function is CfirConstructor) {
            context.findClosestDeclaration<CfirClassLikeDeclaration>()
        } else {
            null
        }
        analyzeFunctionBody(function, body, owner)
        return initializationAssignments.orEmpty()
    }

    fun checkClassLikeMemberInitialization(classLike: CfirClassLikeDeclaration) {
        val trackedFields = classLike.instanceFieldInfos()
        if (trackedFields.isEmpty()) return

        var state = InitializationState.empty().declareAll(
            trackedVariables = trackedFields,
            initializedSymbols = emptySet(),
        )

        for (declaration in classLike.declarations) {
            val field = declaration as? CfirFieldVariable ?: continue
            if (field.status.isStatic) continue

            val initializer = field.initializer
            if (initializer != null) {
                state = analyzeExpression(initializer, state)
                state = state.markInitialized(field.symbol)
            }
        }
    }

    fun checkConstructorCompleteness(
        owner: CfirClassLikeDeclaration,
        constructor: CfirConstructor,
    ) {
        val body = constructor.body ?: return
        if (constructor.firstDelegationKind() == ConstructorDelegationKind.THIS) return

        val endState = analyzeFunctionBody(constructor, body, owner)
        if (endState.terminated) return

        owner.instanceFieldInfos()
            .filter { fieldInfo -> !endState.isInitialized(fieldInfo.symbol) }
            .forEach { fieldInfo ->
                with(context) {
                    reporter.reportOn(
                        source = constructor.source,
                        factory = CfirErrors.CLASS_UNINITIALIZED_FIELD,
                        a = fieldInfo.diagnosticName,
                    )
                }
            }
    }

    private fun analyzeFunctionBody(
        function: CfirFunction,
        body: CfirBlock,
        owner: CfirClassLikeDeclaration?,
    ): InitializationState {
        val parameterInfos = function.valueParameters.map { parameter ->
            TrackedVariableInfo(
                symbol = parameter.symbol,
                diagnosticName = parameter.name,
            )
        }
        val fieldInfos = if (function is CfirConstructor && owner != null) owner.instanceFieldInfos() else emptyList()
        val preInitializedFields = if (function is CfirConstructor && owner != null) {
            owner.instanceFieldsWithInitializer().map(CfirFieldVariable::symbol).toSet()
        } else {
            emptySet()
        }

        val initialState = InitializationState.empty()
            .declareAll(parameterInfos, parameterInfos.map(TrackedVariableInfo::symbol).toSet())
            .declareAll(fieldInfos, preInitializedFields)

        return analyzeStatements(body.statements, initialState)
    }

    private fun analyzeStatements(
        statements: List<CfirElement>,
        initialState: InitializationState,
    ): InitializationState {
        var currentState = initialState
        for (statement in statements) {
            if (currentState.terminated) return currentState
            currentState = analyzeStatement(statement, currentState)
        }
        return currentState
    }

    private fun analyzeStatement(
        statement: CfirElement,
        state: InitializationState,
    ): InitializationState = when (statement) {
        is CfirPatternVariable -> analyzePatternVariable(statement, state)
        is CfirFieldVariable -> analyzeFieldVariable(statement, state)
        is CfirExpression -> analyzeExpression(statement, state)
        else -> state
    }

    private fun analyzePatternVariable(
        variable: CfirPatternVariable,
        state: InitializationState,
    ): InitializationState {
        var declared = state
        for (bindingVariable in variable.pattern.bindingVariables()) {
            declared = declared.declare(
                TrackedVariableInfo(
                    symbol = bindingVariable.symbol,
                    diagnosticName = bindingVariable.name,
                ),
                initialized = false,
            )
        }
        val afterInitializer = variable.initializer?.let { initializer ->
            analyzeExpression(initializer, declared)
        } ?: declared
        return if (variable.initializer != null) {
            variable.pattern.bindingVariables().fold(afterInitializer) { currentState, bindingVariable ->
                currentState.markInitialized(bindingVariable.symbol)
            }
        } else {
            afterInitializer
        }
    }

    private fun analyzeFieldVariable(
        variable: CfirFieldVariable,
        state: InitializationState,
    ): InitializationState {
        if (variable.status.isStatic) return state

        val declared = state.declare(
            TrackedVariableInfo(
                symbol = variable.symbol,
                diagnosticName = variable.name,
            ),
            initialized = false,
        )
        val afterInitializer = variable.initializer?.let { initializer ->
            analyzeExpression(initializer, declared)
        } ?: declared
        return if (variable.initializer != null) afterInitializer.markInitialized(variable.symbol) else afterInitializer
    }

    private fun analyzeExpression(
        expression: CfirExpression,
        state: InitializationState,
        accessMode: InitializationAccessMode = InitializationAccessMode.READ,
    ): InitializationState = when (expression) {
        is CfirAssignment -> analyzeAssignment(expression, state)
        is CfirIfExpression -> analyzeIfExpression(expression, state)
        is CfirMatchExpression -> analyzeMatchExpression(expression, state)
        is CfirTryExpression -> analyzeTryExpression(expression, state)
        is CfirForInExpression -> analyzeForInExpression(expression, state)
        is CfirLoopExpression -> analyzeLoopExpression(expression, state)
        is CfirReturnExpression -> {
            val afterResult = expression.result?.let { analyzeExpression(it, state) } ?: state
            afterResult.terminate()
        }
        is CfirThrowExpression -> analyzeExpression(expression.exception, state).terminate()
        is CfirFunctionCall -> analyzeFunctionCall(expression, state)
        is CfirNamedAccessExpression -> analyzeVariableRead(expression, state, accessMode)
        is CfirQualifiedAccessExpression -> analyzeQualifiedAccess(expression, state, accessMode)
        is CfirBlock -> analyzeScopedBlock(expression, state)
        is CfirBinaryOp -> analyzeChildrenSequentially(expression, state)
        is CfirComparisonExpression -> analyzeChildrenSequentially(expression, state)
        is CfirTypeOperator -> analyzeChildrenSequentially(expression, state)
        is CfirTypeConversion -> analyzeChildrenSequentially(expression, state)
        is CfirRangeExpression -> analyzeChildrenSequentially(expression, state)
        is CfirStringInterpolation -> analyzeChildrenSequentially(expression, state)
        is CfirArrayLiteral -> analyzeChildrenSequentially(expression, state)
        is CfirTupleLiteral -> analyzeChildrenSequentially(expression, state)
        is CfirSpawnExpression -> analyzeScopedBlock(expression.body, state)
        is CfirSynchronizedExpression -> {
            val afterMonitor = analyzeChildrenSequentially(expression, state)
            analyzeScopedBlock(expression.body, afterMonitor)
        }
        is CfirUnsafeExpression -> analyzeChildrenSequentially(expression, state)
        is CfirSubscriptExpression -> analyzeChildrenSequentially(expression, state)
        else -> analyzeChildrenSequentially(expression, state)
    }

    private fun analyzeAssignment(
        assignment: CfirAssignment,
        state: InitializationState,
    ): InitializationState {
        val afterRightValue = analyzeExpression(assignment.rValue, state)
        return analyzeAssignmentTarget(assignment, assignment.lValue, afterRightValue)
    }

    private fun analyzeAssignmentTarget(
        assignment: CfirAssignment,
        lValue: CfirExpression,
        state: InitializationState,
    ): InitializationState = when (lValue) {
        is CfirNamedAccessExpression -> {
            val afterReceiver = lValue.explicitReceiver?.let { receiver ->
                analyzeExpression(receiver, state)
            } ?: state
            lValue.resolvedVariableSymbolOrNull()?.let { symbol ->
                recordInitializationAssignmentIfNeeded(symbol, state, assignment)
                afterReceiver.markInitialized(symbol)
            } ?: afterReceiver
        }

        is CfirQualifiedAccessExpression -> {
            val afterReceiver = lValue.explicitReceiver?.let { receiver ->
                analyzeExpression(receiver, state)
            } ?: state
            lValue.resolvedVariableSymbolOrNull()?.let { symbol ->
                recordInitializationAssignmentIfNeeded(symbol, state, assignment)
                afterReceiver.markInitialized(symbol)
            } ?: afterReceiver
        }

        else -> analyzeExpression(lValue, state)
    }

    private fun recordInitializationAssignmentIfNeeded(
        symbol: CfirVariableSymbol<*>,
        state: InitializationState,
        assignment: CfirAssignment,
    ) {
        if (initializationAssignments == null) return
        if (!state.isTracked(symbol) || state.isInitialized(symbol)) return
        initializationAssignments += assignment
    }

    private fun analyzeIfExpression(
        expression: CfirIfExpression,
        state: InitializationState,
    ): InitializationState {
        val afterCondition = analyzeExpression(expression.condition, state)
        val thenState = analyzeScopedBlock(expression.thenBranch, afterCondition)
        val elseState = expression.elseBranch?.let { elseBranch ->
            when (elseBranch) {
                is CfirBlock -> analyzeScopedBlock(elseBranch, afterCondition)
                else -> analyzeExpression(elseBranch, afterCondition)
            }
        } ?: afterCondition

        return mergeBranchStates(thenState, elseState)
    }

    private fun analyzeMatchExpression(
        expression: CfirMatchExpression,
        state: InitializationState,
    ): InitializationState {
        val afterSubject = expression.subject?.let { analyzeExpression(it, state) } ?: state
        val branchStates = expression.branches.map { branch ->
            val withBindings = branch.pattern.bindingVariables().fold(afterSubject) { currentState, bindingVariable ->
                currentState.declare(
                    TrackedVariableInfo(
                        symbol = bindingVariable.symbol,
                        diagnosticName = bindingVariable.name,
                    ),
                    initialized = true,
                )
            }
            val afterGuard = branch.guard?.let { analyzeExpression(it, withBindings) } ?: withBindings
            analyzeScopedBlock(branch.body, afterGuard)
        }
        return branchStates.reduceOrNull(::mergeBranchStates) ?: afterSubject
    }

    private fun analyzeTryExpression(
        expression: CfirTryExpression,
        state: InitializationState,
    ): InitializationState {
        val tryState = analyzeScopedBlock(expression.tryBlock, state)
        val catchStates = expression.catches.map { catchClause ->
            analyzeScopedBlock(catchClause.body, state)
        }

        val mergedWithoutFinally = (listOf(tryState) + catchStates).reduce(::mergeBranchStates)
        val finallyBlock = expression.finallyBlock ?: return mergedWithoutFinally
        val afterFinally = analyzeScopedBlock(finallyBlock, mergedWithoutFinally.withoutTermination())
        return if (mergedWithoutFinally.terminated) afterFinally.terminate() else afterFinally
    }

    private fun analyzeLoopExpression(
        expression: CfirLoopExpression,
        state: InitializationState,
    ): InitializationState {
        return if (expression.isDoWhile) {
            val afterFirstBody = analyzeScopedBlock(expression.body, state)
            val afterCondition = analyzeExpression(expression.condition, afterFirstBody.withoutTermination())
            if (afterFirstBody.terminated) afterCondition.terminate() else afterCondition
        } else {
            val afterCondition = analyzeExpression(expression.condition, state)
            analyzeScopedBlock(expression.body, afterCondition)
            afterCondition
        }
    }

    private fun analyzeForInExpression(
        expression: CfirForInExpression,
        state: InitializationState,
    ): InitializationState {
        val afterIterable = analyzeExpression(expression.iterable, state)
        val loopState = expression.variable.pattern.bindingVariables().fold(afterIterable) { currentState, bindingVariable ->
            currentState.declare(
                TrackedVariableInfo(
                    symbol = bindingVariable.symbol,
                    diagnosticName = bindingVariable.name,
                ),
                initialized = true,
            )
        }
        analyzeScopedBlock(expression.body, loopState)
        return afterIterable
    }

    private fun analyzeFunctionCall(
        expression: CfirFunctionCall,
        state: InitializationState,
    ): InitializationState {
        var currentState = expression.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state

        val callableVariable = expression.resolvedCallableVariableSymbolOrNull()
        if (callableVariable != null && !expression.origin.isConstructorDelegation) {
            currentState = reportReadIfNeeded(
                symbol = callableVariable,
                diagnosticName = expression.calleeReference.referenceNameOrNull() ?: callableVariable.name,
                source = expression.calleeReference.source ?: expression.source,
                state = currentState,
            )
        }

        for (argument in expression.argumentList.arguments) {
            currentState = analyzeExpression(argument, currentState)
        }
        return currentState
    }

    private fun analyzeVariableRead(
        expression: CfirNamedAccessExpression,
        state: InitializationState,
        accessMode: InitializationAccessMode,
    ): InitializationState {
        val afterReceiver = expression.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state

        if (accessMode != InitializationAccessMode.READ) return afterReceiver

        val symbol = expression.resolvedVariableSymbolOrNull() ?: return afterReceiver
        val diagnosticName = expression.calleeReference.referenceNameOrNull() ?: symbol.name
        return reportReadIfNeeded(symbol, diagnosticName, expression.calleeReference.source ?: expression.source, afterReceiver)
    }

    private fun analyzeQualifiedAccess(
        expression: CfirQualifiedAccessExpression,
        state: InitializationState,
        accessMode: InitializationAccessMode,
    ): InitializationState {
        val afterReceiver = expression.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state

        if (accessMode != InitializationAccessMode.READ) return afterReceiver

        val symbol = expression.resolvedVariableSymbolOrNull() ?: return afterReceiver
        val diagnosticName = expression.calleeReference.referenceNameOrNull() ?: symbol.name
        return reportReadIfNeeded(symbol, diagnosticName, expression.calleeReference.source ?: expression.source, afterReceiver)
    }

    private fun analyzeChildrenSequentially(
        element: CfirElement,
        state: InitializationState,
    ): InitializationState {
        var currentState = state
        element.acceptChildren(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                currentState = when (element) {
                    is CfirPatternVariable -> analyzePatternVariable(element, currentState)
                    is CfirFieldVariable -> analyzeFieldVariable(element, currentState)
                    is CfirExpression -> analyzeExpression(element, currentState)
                    else -> currentState
                }
            }
        }, null)
        return currentState
    }

    private fun analyzeScopedBlock(
        block: CfirBlock,
        state: InitializationState,
    ): InitializationState {
        val visibleBeforeBlock = state.tracked.keys
        val analyzedState = analyzeStatements(block.statements, state)
        return analyzedState.retainOnly(visibleBeforeBlock)
    }

    private fun mergeBranchStates(
        left: InitializationState,
        right: InitializationState,
    ): InitializationState {
        if (left.terminated && right.terminated) return left.intersect(right).terminate()
        if (left.terminated) return right
        if (right.terminated) return left
        return left.intersect(right)
    }

    private fun reportReadIfNeeded(
        symbol: CfirVariableSymbol<*>,
        diagnosticName: Name,
        source: org.cangnova.cangjie.source.CjSourceElement?,
        state: InitializationState,
    ): InitializationState {
        if (!reportReadDiagnostics || !state.isTracked(symbol) || state.isInitialized(symbol)) return state
        with(context) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.USED_BEFORE_INITIALIZATION,
                a = diagnosticName,
            )
        }
        return state
    }
}

internal object CfirInitializationAssignmentClassifier {
    fun isInitializationAssignment(
        assignment: CfirAssignment,
        context: CheckerContext,
    ): Boolean {
        val function = context.findClosestDeclaration<CfirFunction>() ?: return false
        val initializationAssignments = linkedSetOf<CfirAssignment>()
        CfirInitializationFlowAnalyzer(
            context = context,
            reporter = EmptyDiagnosticReporter,
            reportReadDiagnostics = false,
            initializationAssignments = initializationAssignments,
        ).collectInitializationAssignments(function)
        return assignment in initializationAssignments
    }
}

private object EmptyDiagnosticReporter : DiagnosticReporter() {
    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) = Unit
    override val hasErrors: Boolean get() = false
    override val hasWarningsForWError: Boolean get() = false
}

private enum class InitializationAccessMode {
    READ,
    WRITE_TARGET,
}

private data class TrackedVariableInfo(
    val symbol: CfirVariableSymbol<*>,
    val diagnosticName: Name,
)

private data class InitializationState(
    val tracked: Map<CfirVariableSymbol<*>, TrackedVariableInfo>,
    val initialized: Set<CfirVariableSymbol<*>>,
    val terminated: Boolean,
) {
    companion object {
        fun empty(): InitializationState = InitializationState(emptyMap(), emptySet(), terminated = false)
    }

    fun declare(
        trackedVariable: TrackedVariableInfo,
        initialized: Boolean,
    ): InitializationState {
        val normalizedSymbol = trackedVariable.symbol.initializationSymbol()
        val normalizedTrackedVariable = trackedVariable.copy(symbol = normalizedSymbol)
        val nextTracked = tracked + (normalizedSymbol to normalizedTrackedVariable)
        val nextInitialized = if (initialized) {
            this.initialized + normalizedSymbol
        } else {
            this.initialized - normalizedSymbol
        }
        return copy(tracked = nextTracked, initialized = nextInitialized)
    }

    fun declareAll(
        trackedVariables: Collection<TrackedVariableInfo>,
        initializedSymbols: Set<CfirVariableSymbol<*>>,
    ): InitializationState {
        val normalizedInitializedSymbols = initializedSymbols.mapTo(linkedSetOf()) { it.initializationSymbol() }
        var currentState = this
        for (trackedVariable in trackedVariables) {
            currentState = currentState.declare(
                trackedVariable = trackedVariable,
                initialized = trackedVariable.symbol.initializationSymbol() in normalizedInitializedSymbols,
            )
        }
        return currentState
    }

    fun markInitialized(symbol: CfirVariableSymbol<*>): InitializationState {
        val normalizedSymbol = symbol.initializationSymbol()
        if (normalizedSymbol !in tracked) return this
        return copy(initialized = initialized + normalizedSymbol)
    }

    fun isTracked(symbol: CfirVariableSymbol<*>): Boolean = symbol.initializationSymbol() in tracked

    fun isInitialized(symbol: CfirVariableSymbol<*>): Boolean = symbol.initializationSymbol() in initialized

    fun intersect(other: InitializationState): InitializationState {
        val sharedTrackedSymbols = tracked.keys.intersect(other.tracked.keys)
        return InitializationState(
            tracked = tracked.filterKeys { symbol -> symbol in sharedTrackedSymbols },
            initialized = initialized.intersect(other.initialized).filterTo(linkedSetOf()) { symbol ->
                symbol in sharedTrackedSymbols
            },
            terminated = terminated && other.terminated,
        )
    }

    fun retainOnly(visibleSymbols: Set<CfirVariableSymbol<*>>): InitializationState {
        val normalizedVisibleSymbols = visibleSymbols.mapTo(linkedSetOf()) { it.initializationSymbol() }
        return copy(
            tracked = tracked.filterKeys { symbol -> symbol in normalizedVisibleSymbols },
            initialized = initialized.filterTo(linkedSetOf()) { symbol -> symbol in normalizedVisibleSymbols },
        )
    }

    fun terminate(): InitializationState = if (terminated) this else copy(terminated = true)

    fun withoutTermination(): InitializationState = if (!terminated) this else copy(terminated = false)
}

private fun CfirVariableSymbol<*>.initializationSymbol(): CfirVariableSymbol<*> =
    if (isBound) unwrapSubstitutionOverrides() else this

private enum class ConstructorDelegationKind {
    THIS,
    SUPER,
}

object CfirFunctionInitializationChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        CfirInitializationFlowAnalyzer(context, reporter).checkFunction(declaration)
    }
}

object CfirClassLikeInitializationChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        CfirInitializationFlowAnalyzer(context, reporter).checkClassLikeMemberInitialization(declaration)
    }
}

object CfirConstructorInitializationChecker : CfirConstructorChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirConstructor) {
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>() ?: return
        CfirInitializationFlowAnalyzer(
            context = context,
            reporter = reporter,
            reportReadDiagnostics = false,
        ).checkConstructorCompleteness(owner, declaration)
    }
}

private fun CfirClassLikeDeclaration.instanceFieldInfos(): List<TrackedVariableInfo> {
    return declarations
        .filterIsInstance<CfirFieldVariable>()
        .filter { field -> !field.status.isStatic }
        .map { field ->
            TrackedVariableInfo(
                symbol = field.symbol,
                diagnosticName = field.name,
            )
        }
}

private fun CfirClassLikeDeclaration.instanceFieldsWithInitializer(): List<CfirFieldVariable> {
    return declarations
        .filterIsInstance<CfirFieldVariable>()
        .filter { field -> !field.status.isStatic && field.initializer != null }
}

private fun CfirPatternVariable.primaryDiagnosticName(): Name {
    return pattern.primaryBindingNameOrNull() ?: symbol.name
}

private fun CfirConstructor.firstDelegationKind(): ConstructorDelegationKind? {
    val firstStatement = body?.statements?.firstOrNull() as? CfirFunctionCall ?: return null
    return when (firstStatement.origin) {
        CfirFunctionCallOrigin.ConstructorDelegationThis -> ConstructorDelegationKind.THIS
        CfirFunctionCallOrigin.ConstructorDelegationSuper -> ConstructorDelegationKind.SUPER
        else -> null
    }
}

private fun CfirFunctionCall.resolvedCallableVariableSymbolOrNull(): CfirVariableSymbol<*>? {
    return when (val calleeReference = calleeReference) {
        is CfirResolvedNamedReference -> calleeReference.resolvedSymbol as? CfirVariableSymbol<*>
        is CfirResolvedErrorReference -> calleeReference.resolvedSymbol as? CfirVariableSymbol<*>
        else -> null
    }
}

private fun CfirQualifiedAccessExpression.resolvedVariableSymbolOrNull(): CfirVariableSymbol<*>? {
    return when (val calleeReference = calleeReference) {
        is CfirResolvedNamedReference -> calleeReference.resolvedSymbol as? CfirVariableSymbol<*>
        is CfirResolvedErrorReference -> calleeReference.resolvedSymbol as? CfirVariableSymbol<*>
        else -> null
    }
}

private fun org.cangnova.cangjie.cfir.references.CfirReference.referenceNameOrNull(): Name? {
    return when (this) {
        is org.cangnova.cangjie.cfir.references.CfirNamedReference -> name
        else -> null
    }
}
