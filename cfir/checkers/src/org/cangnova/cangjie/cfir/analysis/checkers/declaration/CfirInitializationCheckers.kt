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
import org.cangnova.cangjie.cfir.correspondingProperty
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
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
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
    /**
     * 当前 checker 上下文。
     */
    private val context: CheckerContext,

    /**
     * 诊断报告器。
     */
    private val reporter: DiagnosticReporter,

    /**
     * 是否在分析过程中报告读取未初始化变量的诊断。
     */
    private val reportReadDiagnostics: Boolean = true,

    /**
     * 可选的初始化赋值收集集合，用于只分类赋值而不报告诊断的调用路径。
     */
    private val initializationAssignments: MutableSet<CfirAssignment>? = null,
) {
    /**
     * 检查函数或构造器体内的初始化读取语义。
     */
    fun checkFunction(function: CfirFunction) {
        val body = function.body ?: return
        val owner = if (function is CfirConstructor && function.isInstanceConstructor) {
            context.findClosestDeclaration<CfirClassLikeDeclaration>()
        } else {
            null
        }
        analyzeFunctionBody(function, body, owner)
    }

    /**
     * 收集函数体中会完成初始化的赋值表达式。
     */
    fun collectInitializationAssignments(function: CfirFunction): Set<CfirAssignment> {
        val body = function.body ?: return emptySet()
        val owner = if (function is CfirConstructor && function.isInstanceConstructor) {
            context.findClosestDeclaration<CfirClassLikeDeclaration>()
        } else {
            null
        }
        analyzeFunctionBody(function, body, owner)
        return initializationAssignments.orEmpty()
    }

    /**
     * 检查 class-like 成员初始化器按声明顺序读取实例字段的语义。
     */
    fun checkClassLikeMemberInitialization(classLike: CfirClassLikeDeclaration) {
        val trackedFields = classLike.instanceFieldInfos(context, includeInherited = true)
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
                state = analyzeExpression(initializer, state.withMemberInitializerContext())
                    .withoutMemberInitializerContext()
                state = state.markInitialized(field.symbol)
            }
        }
        reportFieldsLeftUninitializedByDefaultConstructor(classLike, state)
    }

    /**
     * 检查实例构造器结束时是否已初始化所有必要实例字段。
     */
    fun checkConstructorCompleteness(
        owner: CfirClassLikeDeclaration,
        constructor: CfirConstructor,
    ) {
        if (!constructor.isInstanceConstructor) return
        if (constructor.isRedundantPrimaryConstructor(owner)) return
        val body = constructor.body ?: return
        if (constructor.firstDelegationKind() == ConstructorDelegationKind.THIS) return

        val endState = analyzeFunctionBody(constructor, body, owner)
        if (endState.terminated) return

        owner.instanceFieldInfos(context)
            .filter { fieldInfo -> !endState.isInitialized(fieldInfo.symbol) }
            .forEach { fieldInfo ->
                with(context) {
                    reporter.reportOn(
                        source = constructor.source?.firstCharacterDiagnosticSource(),
                        factory = CfirErrors.CLASS_UNINITIALIZED_FIELD,
                        a = fieldInfo.diagnosticName,
                    )
                }
            }
    }

    /**
     * 建立函数体初始化分析的初始状态并分析语句序列。
     */
    private fun analyzeFunctionBody(
        function: CfirFunction,
        body: CfirBlock,
        owner: CfirClassLikeDeclaration?,
    ): InitializationState {
        val parameterInfos = function.valueParameters.map { parameter ->
            TrackedVariableInfo(
                symbol = parameter.symbol,
                diagnosticName = parameter.name,
                isInstanceField = false,
            )
        }
        val fieldInfos = if (function is CfirConstructor && function.isInstanceConstructor && owner != null) {
            owner.instanceFieldInfos(context)
        } else {
            emptyList()
        }
        val preInitializedFields = if (function is CfirConstructor && function.isInstanceConstructor && owner != null) {
            buildSet<CfirBasedSymbol<*>> {
                owner.instanceFieldsWithInitializer().mapTo(this, CfirFieldVariable::symbol)
                owner.primaryConstructorInitializedPropertiesFor(function).mapTo(this, CfirProperty::symbol)
            }
        } else {
            emptySet()
        }

        val initialState = InitializationState.empty()
            .declareAll(parameterInfos, parameterInfos.map(TrackedVariableInfo::symbol).toSet())
            .declareAll(fieldInfos, preInitializedFields)

        return analyzeStatements(body.statements, initialState)
    }

    /**
     * 顺序分析语句列表，遇到已终止状态时提前结束。
     */
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

    /**
     * 分析单条 CFIR 语句。
     */
    private fun analyzeStatement(
        statement: CfirElement,
        state: InitializationState,
    ): InitializationState = when (statement) {
        is CfirPatternVariable -> analyzePatternVariable(statement, state)
        is CfirFieldVariable -> analyzeFieldVariable(statement, state)
        is CfirExpression -> analyzeExpression(statement, state)
        else -> state
    }

    /**
     * 分析 pattern variable 声明及其 initializer。
     */
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
                    isInstanceField = false,
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

    /**
     * 分析字段变量声明及其 initializer。
     */
    private fun analyzeFieldVariable(
        variable: CfirFieldVariable,
        state: InitializationState,
    ): InitializationState {
        if (variable.status.isStatic) return state

        val declared = state.declare(
            TrackedVariableInfo(
                symbol = variable.symbol,
                diagnosticName = variable.name,
                isInstanceField = true,
            ),
            initialized = false,
        )
        val afterInitializer = variable.initializer?.let { initializer ->
            analyzeExpression(initializer, declared.withMemberInitializerContext())
                .withoutMemberInitializerContext()
        } ?: declared
        return if (variable.initializer != null) afterInitializer.markInitialized(variable.symbol) else afterInitializer
    }

    /**
     * 分析表达式对初始化状态的影响。
     */
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

    /**
     * 分析赋值表达式。
     */
    private fun analyzeAssignment(
        assignment: CfirAssignment,
        state: InitializationState,
    ): InitializationState {
        val afterRightValue = analyzeExpression(assignment.rValue, state)
        return analyzeAssignmentTarget(assignment, assignment.lValue, afterRightValue)
    }

    /**
     * 分析赋值左值。
     */
    private fun analyzeAssignmentTarget(
        assignment: CfirAssignment,
        lValue: CfirExpression,
        state: InitializationState,
    ): InitializationState = when (lValue) {
        is CfirQualifiedAccessExpression -> analyzeAssignmentTargetAccess(assignment, lValue, state)

        else -> analyzeExpression(lValue, state)
    }

    /**
     * 赋值左值要沿官方 `InitializationChecker::CheckInitInAssignExpr` 语义区分：
     * 变量左值可以推进初始化状态；成员 `prop`/accessor 不是存储槽，未完成初始化时
     * 需要回到同一套成员访问检查。Kotlin FIR 对应路径是在 `FirDataFlowAnalyzer.exitVariableAssignment`
     * 中只把可跟踪 property/variable 写入初始化流。
     */
    private fun analyzeAssignmentTargetAccess(
        assignment: CfirAssignment,
        access: CfirQualifiedAccessExpression,
        state: InitializationState,
    ): InitializationState {
        val afterReceiver = access.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state
        val symbol = access.resolvedAccessSymbolOrNull() ?: return afterReceiver

        return when {
            symbol is CfirVariableSymbol<*> || afterReceiver.isTracked(symbol) -> {
                recordInitializationAssignmentIfNeeded(symbol, afterReceiver, assignment)
                afterReceiver.markInitialized(symbol)
            }

            afterReceiver.inMemberInitializer && access.explicitReceiver is CfirSuperReceiverExpression -> afterReceiver

            else -> reportIllegalMemberAccessIfNeeded(
                symbol = symbol,
                diagnosticName = access.calleeReference.referenceNameOrNull()
                    ?: symbol.nameOrNull()
                    ?: Name.ERROR_NAME,
                source = access.calleeReference.source ?: access.source,
                state = afterReceiver,
            )
        }
    }

    /**
     * 在当前赋值确实是首次初始化写入时记录赋值表达式。
     *
     * 该信息供外部分类器判断赋值是否属于初始化赋值；同名主构造成员属性已经占用的字段
     * 不再作为字段首次初始化处理。
     */
    private fun recordInitializationAssignmentIfNeeded(
        symbol: CfirBasedSymbol<*>,
        state: InitializationState,
        assignment: CfirAssignment,
    ) {
        if (initializationAssignments == null) return
        if (!state.isTracked(symbol) || state.isInitialized(symbol)) return
        if (symbol is CfirVariableSymbol<*> && symbol.hasSameNamePrimaryConstructorPropertyInOwner()) return
        initializationAssignments += assignment
    }

    /**
     * 主构造参数生成的成员属性与显式字段同名时，官方 Sema 已把该名字视为已由构造参数占用。
     * 后续对显式 `let` 字段的赋值不能再享受“构造器内首次初始化”的不可变豁免。
     */
    private fun CfirVariableSymbol<*>.hasSameNamePrimaryConstructorPropertyInOwner(): Boolean {
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>() ?: return false
        val targetName = name
        return owner.declarations
            .asSequence()
            .filterIsInstance<CfirConstructor>()
            .flatMap { constructor -> constructor.valueParameters.asSequence() }
            .mapNotNull { parameter -> parameter.correspondingProperty }
            .any { property -> property.name == targetName }
    }

    /**
     * 在没有显式构造器体的类型中，报告仍未初始化的实例字段。
     */
    private fun reportFieldsLeftUninitializedByDefaultConstructor(
        classLike: CfirClassLikeDeclaration,
        state: InitializationState,
    ) {
        val constructors = classLike.declarations.filterIsInstance<CfirConstructor>()
            .filter(CfirConstructor::isInstanceConstructor)
        if (constructors.any { it.body != null }) return

        classLike.declarations
            .filterIsInstance<CfirFieldVariable>()
            .filter { field -> !field.status.isStatic && !state.isInitialized(field.symbol) }
            .forEach { field ->
                with(context) {
                    reporter.reportOn(
                        source = field.source,
                        factory = CfirErrors.CLASS_UNINITIALIZED_FIELD,
                        a = field.name,
                    )
                }
            }
    }

    /**
     * 分析 if 表达式并合并 then/else 两个分支的初始化状态。
     */
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

    /**
     * 分析 match 表达式，并把每个分支 pattern binding 作为已初始化局部变量引入。
     */
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
                        isInstanceField = false,
                    ),
                    initialized = true,
                )
            }
            val afterGuard = branch.guard?.let { analyzeExpression(it, withBindings) } ?: withBindings
            analyzeScopedBlock(branch.body, afterGuard)
        }
        return branchStates.reduceOrNull(::mergeBranchStates) ?: afterSubject
    }

    /**
     * 分析 try/catch/finally 表达式。
     */
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

    /**
     * 分析循环表达式。
     */
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

    /**
     * 分析 for-in 表达式及其循环变量绑定。
     */
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
                    isInstanceField = false,
                ),
                initialized = true,
            )
        }
        analyzeScopedBlock(expression.body, loopState)
        return afterIterable
    }

    /**
     * 分析函数调用表达式。
     */
    private fun analyzeFunctionCall(
        expression: CfirFunctionCall,
        state: InitializationState,
    ): InitializationState {
        var currentState = expression.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state

        val callableSymbol = expression.resolvedCallableSymbolOrNull()
        if (callableSymbol != null && !expression.origin.isConstructorDelegation) {
            val diagnosticName = expression.calleeReference.referenceNameOrNull()
                ?: callableSymbol.nameOrNull()
                ?: Name.ERROR_NAME
            currentState = when (callableSymbol) {
                is CfirVariableSymbol<*> -> reportReadIfNeeded(
                    symbol = callableSymbol,
                    diagnosticName = diagnosticName,
                    source = expression.calleeReference.source ?: expression.source,
                    state = currentState,
                )

                else -> if (currentState.inMemberInitializer && expression.explicitReceiver is CfirSuperReceiverExpression) {
                    currentState
                } else reportIllegalMemberAccessIfNeeded(
                    symbol = callableSymbol,
                    diagnosticName = diagnosticName,
                    source = expression.calleeReference.source ?: expression.source,
                    state = currentState,
                )
            }
        }

        for (argument in expression.argumentList.arguments) {
            currentState = analyzeExpression(argument, currentState)
        }
        return currentState
    }

    /**
     * 分析命名访问表达式中的变量读取或成员访问。
     */
    private fun analyzeVariableRead(
        expression: CfirNamedAccessExpression,
        state: InitializationState,
        accessMode: InitializationAccessMode,
    ): InitializationState {
        val afterReceiver = expression.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state

        if (accessMode != InitializationAccessMode.READ) return afterReceiver

        return when (val symbol = expression.resolvedAccessSymbolOrNull()) {
            is CfirVariableSymbol<*> -> reportReadIfNeeded(
                symbol = symbol,
                diagnosticName = expression.calleeReference.referenceNameOrNull() ?: symbol.name,
                source = expression.calleeReference.source ?: expression.source,
                state = afterReceiver,
            )

            null -> afterReceiver
            else -> if (afterReceiver.inMemberInitializer && expression.explicitReceiver is CfirSuperReceiverExpression) {
                afterReceiver
            } else reportIllegalMemberAccessIfNeeded(
                symbol = symbol,
                diagnosticName = expression.calleeReference.referenceNameOrNull()
                    ?: symbol.nameOrNull()
                    ?: Name.ERROR_NAME,
                source = expression.calleeReference.source ?: expression.source,
                state = afterReceiver,
            )
        }
    }

    /**
     * 分析 qualified access 表达式中的变量读取或成员访问。
     */
    private fun analyzeQualifiedAccess(
        expression: CfirQualifiedAccessExpression,
        state: InitializationState,
        accessMode: InitializationAccessMode,
    ): InitializationState {
        val afterReceiver = expression.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state

        if (accessMode != InitializationAccessMode.READ) return afterReceiver

        return when (val symbol = expression.resolvedAccessSymbolOrNull()) {
            is CfirVariableSymbol<*> -> reportReadIfNeeded(
                symbol = symbol,
                diagnosticName = expression.calleeReference.referenceNameOrNull() ?: symbol.name,
                source = expression.calleeReference.source ?: expression.source,
                state = afterReceiver,
            )

            null -> afterReceiver
            else -> reportIllegalMemberAccessIfNeeded(
                symbol = symbol,
                diagnosticName = expression.calleeReference.referenceNameOrNull()
                    ?: symbol.nameOrNull()
                    ?: Name.ERROR_NAME,
                source = expression.calleeReference.source ?: expression.source,
                state = afterReceiver,
            )
        }
    }

    /**
     * 按子节点顺序分析普通元素。
     */
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

    /**
     * 分析作用域块，并在退出块时丢弃块内声明的局部变量跟踪。
     */
    private fun analyzeScopedBlock(
        block: CfirBlock,
        state: InitializationState,
    ): InitializationState {
        val visibleBeforeBlock = state.tracked.keys
        val analyzedState = analyzeStatements(block.statements, state)
        return analyzedState.retainOnly(visibleBeforeBlock)
    }

    /**
     * 合并两个控制流分支的初始化状态。
     */
    private fun mergeBranchStates(
        left: InitializationState,
        right: InitializationState,
    ): InitializationState {
        if (left.terminated && right.terminated) return left.intersect(right).terminate()
        if (left.terminated) return right
        if (right.terminated) return left
        return left.intersect(right)
    }

    /**
     * 如有必要，报告读取未初始化变量。
     */
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

    /**
     * 官方 `CheckIllegalMemberAccess` 在实例成员未全部初始化前禁止访问成员属性/函数。
     *
     * 本项目当前没有独立的 `sema_illegal_usage_of_member` 诊断面，LLT 中该语义
     * 统一落到 `USED_BEFORE_INITIALIZATION`，位置使用被访问成员名。
     */
    private fun reportIllegalMemberAccessIfNeeded(
        symbol: CfirBasedSymbol<*>,
        diagnosticName: Name,
        source: org.cangnova.cangjie.source.CjSourceElement?,
        state: InitializationState,
    ): InitializationState {
        if (!reportReadDiagnostics || !state.hasUninitializedInstanceFields()) return state
        if (!symbol.isInstanceMemberFunctionOrProperty()) return state
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

/**
 * 初始化赋值分类器。
 *
 * 该入口复用初始化流分析器，但关闭诊断报告，只判断某个赋值是否属于初始化赋值。
 */
internal object CfirInitializationAssignmentClassifier {
    /**
     * 判断赋值表达式是否完成了当前函数上下文中的变量或字段初始化。
     */
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

/**
 * 空诊断报告器。
 *
 * 用于只收集初始化赋值事实、不产生诊断的分析路径。
 */
private object EmptyDiagnosticReporter : DiagnosticReporter() {
    /**
     * 忽略所有诊断。
     */
    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) = Unit

    /**
     * 空报告器永远不含 error。
     */
    override val hasErrors: Boolean get() = false

    /**
     * 空报告器永远不含会被 -Werror 提升的 warning。
     */
    override val hasWarningsForWError: Boolean get() = false
}

/**
 * 初始化分析中访问表达式的角色。
 */
private enum class InitializationAccessMode {
    /**
     * 普通读取访问。
     */
    READ,

    /**
     * 赋值目标访问。
     */
    WRITE_TARGET,
}

/**
 * 初始化分析中被跟踪的变量或字段信息。
 */
private data class TrackedVariableInfo(
    /**
     * 被跟踪的 CFIR 符号。
     */
    val symbol: CfirBasedSymbol<*>,

    /**
     * 诊断中展示的名称。
     */
    val diagnosticName: Name,

    /**
     * 该符号是否表示实例字段或主构造成员属性。
     */
    val isInstanceField: Boolean,
)

/**
 * 初始化流分析状态。
 */
private data class InitializationState(
    /**
     * 当前作用域可见且需要跟踪的符号表。
     */
    val tracked: Map<CfirBasedSymbol<*>, TrackedVariableInfo>,

    /**
     * 已经完成初始化的符号集合。
     */
    val initialized: Set<CfirBasedSymbol<*>>,

    /**
     * 当前控制流是否已经终止。
     */
    val terminated: Boolean,

    /**
     * 当前是否处于成员初始化器求值上下文。
     */
    val inMemberInitializer: Boolean,
) {
    /**
     * 初始化状态工厂。
     */
    companion object {
        /**
         * 创建空初始化状态。
         */
        fun empty(): InitializationState = InitializationState(
            tracked = emptyMap(),
            initialized = emptySet(),
            terminated = false,
            inMemberInitializer = false,
        )
    }

    /**
     * 声明一个新的跟踪变量并设置初始初始化状态。
     */
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

    /**
     * 批量声明跟踪变量。
     */
    fun declareAll(
        trackedVariables: Collection<TrackedVariableInfo>,
        initializedSymbols: Set<CfirBasedSymbol<*>>,
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

    /**
     * 标记指定符号已经初始化。
     */
    fun markInitialized(symbol: CfirBasedSymbol<*>): InitializationState {
        val normalizedSymbol = symbol.initializationSymbol()
        if (normalizedSymbol !in tracked) return this
        return copy(initialized = initialized + normalizedSymbol)
    }

    /**
     * 判断指定符号是否被当前状态跟踪。
     */
    fun isTracked(symbol: CfirBasedSymbol<*>): Boolean = symbol.initializationSymbol() in tracked

    /**
     * 判断指定符号是否已经初始化。
     */
    fun isInitialized(symbol: CfirBasedSymbol<*>): Boolean = symbol.initializationSymbol() in initialized

    /**
     * 判断当前状态中是否仍有未初始化的实例字段。
     */
    fun hasUninitializedInstanceFields(): Boolean {
        return tracked.any { (symbol, variableInfo) ->
            variableInfo.isInstanceField && symbol !in initialized
        }
    }

    /**
     * 进入成员初始化器上下文。
     */
    fun withMemberInitializerContext(): InitializationState =
        if (inMemberInitializer) this else copy(inMemberInitializer = true)

    /**
     * 离开成员初始化器上下文。
     */
    fun withoutMemberInitializerContext(): InitializationState =
        if (!inMemberInitializer) this else copy(inMemberInitializer = false)

    /**
     * 取两个分支状态的交集。
     */
    fun intersect(other: InitializationState): InitializationState {
        val sharedTrackedSymbols = tracked.keys.intersect(other.tracked.keys)
        return InitializationState(
            tracked = tracked.filterKeys { symbol -> symbol in sharedTrackedSymbols },
            initialized = initialized.intersect(other.initialized).filterTo(linkedSetOf()) { symbol ->
                symbol in sharedTrackedSymbols
            },
            terminated = terminated && other.terminated,
            inMemberInitializer = inMemberInitializer && other.inMemberInitializer,
        )
    }

    /**
     * 只保留指定可见符号集合。
     */
    fun retainOnly(visibleSymbols: Set<CfirBasedSymbol<*>>): InitializationState {
        val normalizedVisibleSymbols = visibleSymbols.mapTo(linkedSetOf()) { it.initializationSymbol() }
        return copy(
            tracked = tracked.filterKeys { symbol -> symbol in normalizedVisibleSymbols },
            initialized = initialized.filterTo(linkedSetOf()) { symbol -> symbol in normalizedVisibleSymbols },
        )
    }

    /**
     * 标记当前控制流已终止。
     */
    fun terminate(): InitializationState = if (terminated) this else copy(terminated = true)

    /**
     * 清除当前控制流终止标记。
     */
    fun withoutTermination(): InitializationState = if (!terminated) this else copy(terminated = false)
}

/**
 * 将访问器、属性和 substitution override 符号归一化为初始化分析使用的存储符号。
 */
private fun CfirBasedSymbol<*>.initializationSymbol(): CfirBasedSymbol<*> = when {
    this is CfirVariableSymbol<*> && isBound -> unwrapSubstitutionOverrides()
    this is CfirPropertySymbol && isBound -> unwrapSubstitutionOverrides()
    this is CfirPropertyAccessorSymbol && isBound -> propertySymbol.unwrapSubstitutionOverrides()
    else -> this
}

/**
 * 构造器委托调用种类。
 */
private enum class ConstructorDelegationKind {
    /**
     * `this(...)` 委托。
     */
    THIS,

    /**
     * `super(...)` 委托。
     */
    SUPER,
}

// 仓颉 AST 中 `static init` 同时带 STATIC 与 CONSTRUCTOR 属性，但它不是实例构造器。
/**
 * 判断构造器是否是实例构造器。
 */
private val CfirConstructor.isInstanceConstructor: Boolean
    get() = !status.isStatic

/**
 * 函数体初始化读取检查器。
 */
object CfirFunctionInitializationChecker : CfirFunctionChecker() {
    /**
     * 检查函数或构造器体内的初始化语义。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        CfirInitializationFlowAnalyzer(context, reporter).checkFunction(declaration)
    }
}

/**
 * class-like 成员初始化器声明顺序检查器。
 */
object CfirClassLikeInitializationChecker : CfirClassLikeChecker() {
    /**
     * 检查 class-like 声明的成员初始化器。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        CfirInitializationFlowAnalyzer(context, reporter).checkClassLikeMemberInitialization(declaration)
    }
}

/**
 * 构造器完成实例字段初始化检查器。
 */
object CfirConstructorInitializationChecker : CfirConstructorChecker() {
    /**
     * 检查构造器结束时实例字段是否全部初始化。
     */
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

/**
 * 初始化检查中的实例字段集合。
 *
 * 官方 `InitializationChecker::GetNonFuncDeclsInSuperClass` 会把父类非 private
 * 实例字段纳入子类初始化阶段；因此成员初始化器检查需要同时跟踪本类字段和可见父类字段。
 */
private fun CfirClassLikeDeclaration.instanceFieldInfos(
    context: CheckerContext,
    includeInherited: Boolean = false,
): List<TrackedVariableInfo> = buildList {
    if (includeInherited && this@instanceFieldInfos is CfirClass) {
        addAll(inheritedInstanceFieldInfos(context, visitedClasses = linkedSetOf()))
    }
    addAll(declaredInstanceFieldInfos())
}

/**
 * 收集当前 class-like 自身声明的实例字段和主构造成员属性。
 */
private fun CfirClassLikeDeclaration.declaredInstanceFieldInfos(): List<TrackedVariableInfo> {
    return buildList {
        declarations
            .filterIsInstance<CfirFieldVariable>()
            .filter { field -> !field.status.isStatic }
            .mapTo(this, CfirFieldVariable::toTrackedInstanceFieldInfo)
        addAll(primaryConstructorPropertyInfos())
    }
}

/**
 * 收集可见父类实例字段和主构造成员属性。
 */
private fun CfirClassLikeDeclaration.inheritedInstanceFieldInfos(
    context: CheckerContext,
    visitedClasses: MutableSet<ClassId>,
): List<TrackedVariableInfo> = buildList {
    for (superTypeRef in superTypeRefs) {
        val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
        val superClassId = superType.classId
        if (!visitedClasses.add(superClassId)) continue

        val superClass = context.session.symbolProvider
            .getClassLikeSymbolByClassId(superClassId)
            ?.cfir as? CfirClass ?: continue

        addAll(
            superClass.declarations
                .filterIsInstance<CfirFieldVariable>()
                .filter { field -> !field.status.isStatic && field.status.visibility != Visibilities.Private }
                .map(CfirFieldVariable::toTrackedInstanceFieldInfo)
        )
        addAll(
            superClass.primaryConstructorPropertyInfos()
                .filter { propertyInfo ->
                    (propertyInfo.symbol as? CfirPropertySymbol)?.cfir?.status?.visibility != Visibilities.Private
                }
        )
        addAll(superClass.inheritedInstanceFieldInfos(context, visitedClasses))
    }
}

/**
 * 将字段变量转换为初始化跟踪信息。
 */
private fun CfirFieldVariable.toTrackedInstanceFieldInfo(): TrackedVariableInfo =
    TrackedVariableInfo(
        symbol = symbol,
        diagnosticName = name,
        isInstanceField = true,
    )

/**
 * 主构造 `let/var` 参数在 raw CFIR 中以 [CfirProperty] 进入类声明树。
 *
 * 官方初始化检查收集的是类中的非函数成员声明，Kotlin FIR 也按 property/backing-field
 * 做初始化 CFA；因此这里不能只跟踪 [CfirFieldVariable]。同名冲突时仅最早的
 * 存储声明参与初始化检查，保持与官方 PreCheck “后声明报重定义”的语义一致。
 */
private fun CfirClassLikeDeclaration.primaryConstructorPropertyInfos(): List<TrackedVariableInfo> {
    val effectiveProperties = primaryConstructorParametersWithProperties()
        .mapNotNull { (_, property) ->
            property.takeIf { isEarliestStorageDeclaration(property) }
        }
        .toList()

    return effectiveProperties.map { property ->
        TrackedVariableInfo(
            symbol = property.symbol,
            diagnosticName = property.name,
            isInstanceField = true,
        )
    }
}

/**
 * 收集带 initializer 的实例字段。
 */
private fun CfirClassLikeDeclaration.instanceFieldsWithInitializer(): List<CfirFieldVariable> {
    return declarations
        .filterIsInstance<CfirFieldVariable>()
        .filter { field -> !field.status.isStatic && field.initializer != null }
}

/**
 * 收集主构造器参数已经初始化的成员属性。
 */
private fun CfirClassLikeDeclaration.primaryConstructorInitializedPropertiesFor(constructor: CfirConstructor): List<CfirProperty> {
    if (!constructor.isPrimary) return emptyList()
    return constructor.valueParameters
        .asSequence()
        .mapNotNull(CfirValueParameter::correspondingProperty)
        .mapNotNull { property ->
            property.takeIf {
                isEarliestStorageDeclaration(property) &&
                        isUniquePrimaryConstructorProperty(property)
            }
        }
        .toList()
}

/**
 * 枚举主构造器参数及其生成的成员属性。
 */
private fun CfirClassLikeDeclaration.primaryConstructorParametersWithProperties(): Sequence<Pair<CfirValueParameter, CfirProperty>> {
    return declarations
        .asSequence()
        .filterIsInstance<CfirConstructor>()
        .filter(CfirConstructor::isPrimary)
        .flatMap { constructor -> constructor.valueParameters.asSequence() }
        .mapNotNull { parameter -> parameter.correspondingProperty?.let { property -> parameter to property } }
}

/**
 * 判断主构造成员属性名称是否唯一。
 */
private fun CfirClassLikeDeclaration.isUniquePrimaryConstructorProperty(property: CfirProperty): Boolean {
    val propertyName = property.name
    return primaryConstructorParametersWithProperties()
        .map { (_, candidate) -> candidate }
        .none { candidate -> candidate !== property && candidate.name == propertyName }
}

/**
 * 判断属性是否是同名存储声明中最早出现的声明。
 */
private fun CfirClassLikeDeclaration.isEarliestStorageDeclaration(property: CfirProperty): Boolean {
    val propertyOffset = property.source?.startOffset ?: Int.MAX_VALUE
    val propertyName = property.name
    return declarations
        .asSequence()
        .filter { declaration -> declaration !== property }
        .filter { declaration -> declaration.storageDeclarationNameOrNull() == propertyName }
        .none { declaration -> (declaration.source?.startOffset ?: Int.MAX_VALUE) < propertyOffset }
}

/**
 * 取得可作为存储声明参与初始化检查的声明名称。
 */
private fun CfirDeclaration.storageDeclarationNameOrNull(): Name? = when (this) {
    is CfirFieldVariable -> name
    is CfirProperty -> name
    else -> null
}

/**
 * 取得 pattern variable 诊断使用的主要绑定名称。
 */
private fun CfirPatternVariable.primaryDiagnosticName(): Name {
    return pattern.primaryBindingNameOrNull() ?: symbol.name
}

/**
 * 取得构造器体第一条语句的委托调用种类。
 */
private fun CfirConstructor.firstDelegationKind(): ConstructorDelegationKind? {
    val firstStatement = body?.statements?.firstOrNull() as? CfirFunctionCall ?: return null
    return when (firstStatement.origin) {
        CfirFunctionCallOrigin.ConstructorDelegationThis -> ConstructorDelegationKind.THIS
        CfirFunctionCallOrigin.ConstructorDelegationSuper -> ConstructorDelegationKind.SUPER
        else -> null
    }
}

/**
 * 多个主构造器时，官方 Sema 只把第二个及后续声明作为
 * `sema_multiple_primary_constructors` 的非法声明处理，不再要求它完成字段初始化。
 */
private fun CfirConstructor.isRedundantPrimaryConstructor(owner: CfirClassLikeDeclaration): Boolean {
    if (!isPrimary) return false
    val firstPrimary = owner.declarations
        .asSequence()
        .filterIsInstance<CfirConstructor>()
        .filter(CfirConstructor::isPrimary)
        .minByOrNull { constructor -> constructor.source?.startOffset ?: Int.MAX_VALUE }
    return firstPrimary != null && firstPrimary !== this
}

/**
 * 从函数调用中解析 callable 符号。
 */
private fun CfirFunctionCall.resolvedCallableSymbolOrNull(): CfirBasedSymbol<*>? {
    return when (val calleeReference = calleeReference) {
        is CfirResolvedNamedReference -> calleeReference.resolvedSymbol
        is CfirResolvedErrorReference -> calleeReference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> calleeReference.candidateSymbol
        else -> null
    }
}

/**
 * 从 qualified access 中解析访问目标符号。
 */
private fun CfirQualifiedAccessExpression.resolvedAccessSymbolOrNull(): CfirBasedSymbol<*>? {
    return when (val calleeReference = calleeReference) {
        is CfirResolvedNamedReference -> calleeReference.resolvedSymbol
        is CfirResolvedErrorReference -> calleeReference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> calleeReference.candidateSymbol
        else -> null
    }
}

/**
 * 判断符号是否表示实例函数、实例属性或实例属性访问器。
 */
private fun CfirBasedSymbol<*>.isInstanceMemberFunctionOrProperty(): Boolean {
    return when (this) {
        is CfirPropertyAccessorSymbol -> isBound && propertySymbol.callableId.classId != null && !propertySymbol.cfir.status.isStatic
        is CfirPropertySymbol -> isBound && callableId.classId != null && !cfir.status.isStatic
        is CfirNamedFunctionSymbol -> isBound && callableId.classId != null && !cfir.status.isStatic
        else -> false
    }
}

/**
 * 取得符号用于诊断展示的名称。
 */
private fun CfirBasedSymbol<*>.nameOrNull(): Name? {
    return when (this) {
        is CfirVariableSymbol<*> -> name
        is CfirPropertyAccessorSymbol -> if (isBound) propertySymbol.name else null
        is CfirPropertySymbol -> name
        is CfirNamedFunctionSymbol -> name
        else -> null
    }
}

/**
 * 取得引用中的命名引用名称。
 */
private fun org.cangnova.cangjie.cfir.references.CfirReference.referenceNameOrNull(): Name? {
    return when (this) {
        is org.cangnova.cangjie.cfir.references.CfirNamedReference -> name
        else -> null
    }
}
