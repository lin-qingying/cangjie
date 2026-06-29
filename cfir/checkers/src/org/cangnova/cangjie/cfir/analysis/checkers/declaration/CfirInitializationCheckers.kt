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
     * 检查 class-like 成员初始化器按声明顺序读取字段的语义。
     */
    fun checkClassLikeMemberInitialization(classLike: CfirClassLikeDeclaration) {
        checkClassLikeStaticMemberInitialization(classLike)
        checkClassLikeInstanceMemberInitialization(classLike)
    }

    /**
     * 检查同一文件内 static/global 变量按源码顺序初始化的语义。
     */
    fun checkFileStaticGlobalInitialization(file: CfirFile) {
        val trackedDeclarations = file.staticGlobalInitializerDeclarations()
        if (trackedDeclarations.isEmpty()) return

        val trackedBySymbol = trackedDeclarations
            .flatMap { declaration -> declaration.variables }
            .associateBy { variable -> variable.symbol.initializationSymbol() }
        val trackedInfos = trackedBySymbol.values.map { variable ->
            TrackedVariableInfo(
                symbol = variable.symbol,
                diagnosticName = variable.diagnosticName,
                isInstanceField = false,
            )
        }
        val recursiveStaticFunctionReads = mutableListOf<StaticGlobalUseEdge>()
        var state = InitializationState.empty().declareAll(trackedInfos, emptySet())
        var nextVisitOrder = 0

        for (declaration in trackedDeclarations) {
            when (declaration.kind) {
                StaticGlobalInitializerKind.VARIABLE -> {
                    declaration.variables.forEach { variable ->
                        if (variable.visitOrder < 0) {
                            variable.visitOrder = nextVisitOrder++
                        }
                    }
                    declaration.initializer?.let { initializer ->
                        reportStaticGlobalReadsBeforeInitialization(
                            currentDeclaration = declaration,
                            initializer = initializer,
                            trackedBySymbol = trackedBySymbol,
                            initialized = state.initialized,
                        )
                        state = declaration.markVariablesInitialized(state)
                    }
                }

                StaticGlobalInitializerKind.STATIC_INIT -> {
                    val result = processStaticInitializerBody(
                        declaration = declaration,
                        initialState = state,
                        trackedBySymbol = trackedBySymbol,
                        nextVisitOrder = nextVisitOrder,
                        recursiveStaticFunctionReads = recursiveStaticFunctionReads,
                    )
                    state = result.state
                    nextVisitOrder = result.nextVisitOrder
                }
            }
        }

        reportRecursiveStaticFunctionReadsBeforeInitialization(recursiveStaticFunctionReads)
        reportUninitializedStaticFields(trackedBySymbol.values)
    }

    /**
     * 检查 static 字段初始化器。
     *
     * 官方 `CollectToDeclsInfo` 会在遍历类成员时立即检查 static 非函数声明；
     * static 字段按声明顺序初始化，而实例字段在 static 初始化上下文中不可视为已初始化。
     */
    private fun checkClassLikeStaticMemberInitialization(classLike: CfirClassLikeDeclaration) {
        val trackedFields = classLike.staticInitializerFieldInfos()
        if (trackedFields.isEmpty()) return

        var state = InitializationState.empty().declareAll(
            trackedVariables = trackedFields,
            initializedSymbols = emptySet(),
        )

        for (declaration in classLike.declarations) {
            val field = declaration as? CfirFieldVariable ?: continue
            if (!field.status.isStatic) continue

            val initializer = field.initializer
            if (initializer != null) {
                reportStaticVariableNonStaticMemberAccesses(field, initializer)
                state = analyzeExpression(initializer, state.withMemberInitializerContext(classLike))
                    .withoutMemberInitializerContext()
                state = state.markInitialized(field.symbol)
            }
        }
    }

    /**
     * 检查实例字段初始化器。
     */
    private fun checkClassLikeInstanceMemberInitialization(classLike: CfirClassLikeDeclaration) {
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
                state = analyzeExpression(initializer, state.withMemberInitializerContext(classLike))
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
                        source = constructor.constructorDeclarationHeaderDiagnosticSource(),
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
        is CfirAnonymousFunctionExpression -> state
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

            access.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver) -> afterReceiver

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
                is CfirVariableSymbol<*> -> if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(currentState)) {
                    currentState
                } else reportReadIfNeeded(
                    symbol = callableSymbol,
                    diagnosticName = diagnosticName,
                    source = expression.calleeReference.source ?: expression.source,
                    state = currentState,
                )

                else -> if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(currentState)) {
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
            is CfirVariableSymbol<*> -> if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver)) {
                afterReceiver
            } else reportReadIfNeeded(
                symbol = symbol,
                diagnosticName = expression.calleeReference.referenceNameOrNull() ?: symbol.name,
                source = expression.calleeReference.source ?: expression.source,
                state = afterReceiver,
            )

            null -> afterReceiver
            else -> if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver)) {
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
            is CfirVariableSymbol<*> -> if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver)) {
                afterReceiver
            } else reportReadIfNeeded(
                symbol = symbol,
                diagnosticName = expression.calleeReference.referenceNameOrNull() ?: symbol.name,
                source = expression.calleeReference.source ?: expression.source,
                state = afterReceiver,
            )

            null -> afterReceiver
            else -> if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver)) {
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
     * 字段初始化器中非法 `super.member` 与 struct `this.member` 已由专门的 this/super checker 分类。
     * 初始化流分析不再追加 `USED_BEFORE_INITIALIZATION`，避免同一根因产生级联诊断。
     */
    private fun CfirQualifiedAccessExpression.shouldSkipIllegalMemberAccessInMemberInitializer(
        state: InitializationState,
    ): Boolean {
        if (!state.inMemberInitializer) return false
        if (hasSuperReceiver()) return true
        return state.memberInitializerOwner is CfirStruct && hasThisReceiver()
    }

    private fun CfirQualifiedAccessExpression.hasSuperReceiver(): Boolean =
        explicitReceiver is CfirSuperReceiverExpression || dispatchReceiver is CfirSuperReceiverExpression

    private fun CfirQualifiedAccessExpression.hasThisReceiver(): Boolean =
        explicitReceiver is CfirThisReceiverExpression || dispatchReceiver is CfirThisReceiverExpression

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
     * static 变量初始化器不能直接读取当前类型的实例存储成员。
     *
     * 官方 `TypeCheckerImpl::CheckStaticVarAccessNonStatic` 挂在 static `VarDecl`
     * 上，并只扫描 initializer 中的简单引用；成员访问和嵌套函数体交给各自
     * 的访问规则，避免把 `A.b` 或 lambda 体误判成 static 变量直接访问。
     */
    private fun reportStaticVariableNonStaticMemberAccesses(
        staticField: CfirFieldVariable,
        initializer: CfirExpression,
    ) {
        initializer.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun visitFunction(function: CfirFunction) = Unit

            override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                for (argument in functionCall.argumentList.arguments) {
                    argument.accept(this, null)
                }
            }

            override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) = Unit

            override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression) {
                if (namedAccessExpression.explicitReceiver != null) return
                val memberName = namedAccessExpression
                    .resolvedAccessSymbolOrNull()
                    ?.nonStaticMemberNameForStaticVariableDiagnostic()
                    ?: return

                with(context) {
                    reporter.reportOn(
                        source = staticField.source?.firstCharacterDiagnosticSource(),
                        factory = CfirErrors.STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER,
                        a = memberName,
                    )
                }
            }
        }, null)
    }

    /**
     * 处理 static init 中对 static/global 变量的直接读取与直接初始化赋值。
     */
    private fun processStaticInitializerBody(
        declaration: StaticGlobalInitializerDeclaration,
        initialState: InitializationState,
        trackedBySymbol: Map<CfirBasedSymbol<*>, StaticGlobalInitializerVariable>,
        nextVisitOrder: Int,
        recursiveStaticFunctionReads: MutableList<StaticGlobalUseEdge>,
    ): StaticInitializerProcessingResult {
        val body = declaration.body ?: return StaticInitializerProcessingResult(initialState, nextVisitOrder)
        var state = initialState
        var order = nextVisitOrder

        state = analyzeStatements(body.statements, state)
        for (variable in trackedBySymbol.values) {
            if (variable.initialized) continue
            if (!state.isInitialized(variable.symbol)) continue
            variable.initialized = true
            variable.visitOrder = order++
        }

        declaration.visitOrder = order++
        collectRecursiveStaticFunctionReads(body, declaration.visitOrder, trackedBySymbol, recursiveStaticFunctionReads)
        return StaticInitializerProcessingResult(state, order)
    }

    /**
     * 收集 static init 可达 static 函数体里的 static/global 变量读取。
     */
    private fun collectRecursiveStaticFunctionReads(
        body: CfirBlock,
        ownerVisitOrder: Int,
        trackedBySymbol: Map<CfirBasedSymbol<*>, StaticGlobalInitializerVariable>,
        destination: MutableList<StaticGlobalUseEdge>,
    ) {
        val visitedFunctions = linkedSetOf<CfirFunction>()

        fun collectFromFunction(function: CfirFunction) {
            if (!visitedFunctions.add(function)) return
            val functionBody = function.body ?: return
            functionBody.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    element.acceptChildren(this, null)
                }

                override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

                override fun visitAssignment(assignment: CfirAssignment) {
                    assignment.rValue.accept(this, null)
                }

                override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                    functionCall.resolvedStaticOrGlobalFunctionOrNull()?.let(::collectFromFunction)
                    functionCall.explicitReceiver?.accept(this, null)
                    for (argument in functionCall.argumentList.arguments) {
                        argument.accept(this, null)
                    }
                }

                override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                    qualifiedAccessExpression.explicitReceiver?.accept(this, null)
                    collectUseEdge(qualifiedAccessExpression)
                }

                override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression) {
                    namedAccessExpression.explicitReceiver?.accept(this, null)
                    collectUseEdge(namedAccessExpression)
                }

                private fun collectUseEdge(access: CfirQualifiedAccessExpression) {
                    val symbol = access.resolvedAccessSymbolOrNull()?.initializationSymbol() ?: return
                    val variable = trackedBySymbol[symbol] ?: return
                    destination += StaticGlobalUseEdge(
                        ownerVisitOrder = ownerVisitOrder,
                        usedVariable = variable,
                        source = access.calleeReference.source ?: access.source,
                        diagnosticName = access.calleeReference.referenceNameOrNull() ?: variable.diagnosticName,
                    )
                }
            }, null)
        }

        body.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

            override fun visitPatternVariable(patternVariable: CfirPatternVariable) {
                val initializer = patternVariable.initializer ?: return
                if (initializer is CfirAnonymousFunctionExpression) return
                initializer.accept(this, null)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                functionCall.resolvedStaticOrGlobalFunctionOrNull()?.let(::collectFromFunction)
                functionCall.explicitReceiver?.accept(this, null)
                for (argument in functionCall.argumentList.arguments) {
                    argument.accept(this, null)
                }
            }
        }, null)
    }

    /**
     * 报告 static init 可达静态函数体中读取尚未初始化的 static/global 变量。
     */
    private fun reportRecursiveStaticFunctionReadsBeforeInitialization(edges: List<StaticGlobalUseEdge>) {
        for (edge in edges) {
            if (edge.usedVariable.visitOrder < edge.ownerVisitOrder) continue
            with(context) {
                reporter.reportOn(
                    source = edge.source,
                    factory = CfirErrors.USED_BEFORE_INITIALIZATION,
                    a = edge.diagnosticName,
                )
            }
        }
    }

    /**
     * 报告最终仍未初始化的 static 字段。
     */
    private fun reportUninitializedStaticFields(variables: Collection<StaticGlobalInitializerVariable>) {
        for (variable in variables) {
            val field = variable.field ?: continue
            if (!field.status.isStatic) continue
            if (variable.initialized) continue
            with(context) {
                reporter.reportOn(
                    source = field.fieldVariableNameDiagnosticSource(),
                    factory = CfirErrors.TYPE_UNINITIALIZED_STATIC_FIELD,
                    a = field.name,
                )
            }
        }
    }

    /**
     * static/global 字段初始化顺序检查。
     *
     * 官方 `IsVarUsedBeforeDefinition` 对同一文件中的 static/global 变量按源码位置判断；
     * 这里复用 resolved symbol，而不是重新做名字查找。
     */
    private fun reportStaticGlobalReadsBeforeInitialization(
        currentDeclaration: StaticGlobalInitializerDeclaration,
        initializer: CfirExpression,
        trackedBySymbol: Map<CfirBasedSymbol<*>, StaticGlobalInitializerVariable>,
        initialized: Set<CfirBasedSymbol<*>>,
    ) {
        initializer.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun visitFunction(function: CfirFunction) {
                function.body?.accept(this, null)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                reportAccessIfNeeded(functionCall)
                functionCall.acceptChildren(this, null)
            }

            override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                reportAccessIfNeeded(qualifiedAccessExpression)
                qualifiedAccessExpression.acceptChildren(this, null)
            }

            override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression) {
                reportAccessIfNeeded(namedAccessExpression)
                namedAccessExpression.acceptChildren(this, null)
            }

            private fun reportAccessIfNeeded(access: CfirQualifiedAccessExpression) {
                val symbol = access.resolvedAccessSymbolOrNull()?.initializationSymbol() ?: return
                if (symbol in initialized) return
                val targetVariable = trackedBySymbol[symbol] ?: return
                if (currentDeclaration.hasSameNominalOwnerAs(targetVariable)) return

                with(context) {
                    reporter.reportOn(
                        source = access.calleeReference.source ?: access.source,
                        factory = CfirErrors.USED_BEFORE_INITIALIZATION,
                        a = targetVariable.diagnosticName,
                    )
                }
            }
        }, null)
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

    /**
     * 当前成员初始化器所属的 class-like；用于区分 struct 字段初始化器中的 `this.member`。
     */
    val memberInitializerOwner: CfirClassLikeDeclaration?,
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
            memberInitializerOwner = null,
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
    fun withMemberInitializerContext(owner: CfirClassLikeDeclaration? = null): InitializationState =
        if (inMemberInitializer && memberInitializerOwner == owner) {
            this
        } else {
            copy(inMemberInitializer = true, memberInitializerOwner = owner)
        }

    /**
     * 离开成员初始化器上下文。
     */
    fun withoutMemberInitializerContext(): InitializationState =
        if (!inMemberInitializer) this else copy(inMemberInitializer = false, memberInitializerOwner = null)

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
            memberInitializerOwner = commonMemberInitializerOwner(other),
        )
    }

    private fun commonMemberInitializerOwner(other: InitializationState): CfirClassLikeDeclaration? {
        if (!inMemberInitializer || !other.inMemberInitializer) return null
        return memberInitializerOwner.takeIf { it == other.memberInitializerOwner }
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
 * 判断构造器是否是 static init。
 */
private val CfirConstructor.isStaticConstructor: Boolean
    get() = status.isStatic

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
 * 文件级 static/global 初始化顺序检查器。
 */
object CfirFileStaticGlobalInitializationChecker : CfirFileChecker() {
    /**
     * 检查同一文件中的顶层变量和 static 成员字段初始化顺序。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        CfirInitializationFlowAnalyzer(context, reporter).checkFileStaticGlobalInitialization(declaration)
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
 * 收集 static 初始化器需要跟踪的当前 class-like 字段。
 *
 * static 字段会按声明顺序推进初始化状态；实例字段也要进入跟踪集合，
 * 但在 static 初始化路径中不会被标记为已初始化，用于统一报告 static
 * 初始化器读取实例存储成员的非法初始化语义。
 */
private fun CfirClassLikeDeclaration.staticInitializerFieldInfos(): List<TrackedVariableInfo> {
    return declarations
        .filterIsInstance<CfirFieldVariable>()
        .map { field ->
            if (field.status.isStatic) {
                field.toTrackedStaticFieldInfo()
            } else {
                field.toTrackedInstanceFieldInfo()
            }
        }
}

/**
 * 文件级 static/global 初始化声明。
 *
 * 顶层 pattern variable 可能一次声明多个绑定变量；初始化顺序按外层声明推进，
 * 但读取检测必须跟踪每一个真正进入作用域的存储符号。
 */
private data class StaticGlobalInitializerDeclaration(
    /**
     * 当前初始化声明向同一文件作用域暴露的所有存储变量。
     */
    val variables: List<StaticGlobalInitializerVariable>,
    /**
     * 变量声明或字段声明上的显式初始化表达式；`static init` 块没有该表达式。
     */
    val initializer: CfirExpression?,
    /**
     * `static init` 声明体；普通变量或字段初始化声明没有该块。
     */
    val body: CfirBlock?,
    /**
     * 声明在源文件中的起始偏移量，用于按照源码顺序稳定排序初始化条目。
     */
    val sourceOffset: Int,
    /**
     * 名义上拥有该 static 字段或初始化块的 class-like 标识；顶层全局变量为空。
     */
    val nominalOwnerClassId: ClassId?,
    /**
     * 当前条目在 static/global 初始化顺序模型中的声明种类。
     */
    val kind: StaticGlobalInitializerKind,
    /**
     * 初始化遍历分配的访问序号，未进入遍历前保持为 `-1`。
     */
    var visitOrder: Int = -1,
)

/**
 * 文件级 static/global 初始化中可被读取的存储变量。
 */
private class StaticGlobalInitializerVariable(
    /**
     * 被初始化顺序检查跟踪的字段、顶层变量或 pattern binding 对应的 CFIR symbol。
     */
    val symbol: CfirBasedSymbol<*>,
    /**
     * 报告诊断时使用的变量名称，保留 pattern binding 等声明的用户可见名称。
     */
    val diagnosticName: Name,
    /**
     * 名义上拥有该 static 字段的 class-like 标识；顶层全局变量为空。
     */
    val nominalOwnerClassId: ClassId?,
    /**
     * 变量来源的字段声明；顶层 pattern/global 变量没有字段节点。
     */
    val field: CfirFieldVariable?,
    /**
     * 该变量在当前初始化顺序遍历中是否已经完成初始化。
     */
    var initialized: Boolean = false,
    /**
     * 初始化遍历分配给该变量的访问序号，未访问前保持为 `-1`。
     */
    var visitOrder: Int = -1,
)

/**
 * 文件级 static/global 初始化条目种类。
 */
private enum class StaticGlobalInitializerKind {
    VARIABLE,
    STATIC_INIT,
}

/**
 * static init 处理后的初始化状态。
 */
private data class StaticInitializerProcessingResult(
    /**
     * `static init` 块处理完成后得到的初始化状态快照。
     */
    val state: InitializationState,
    /**
     * 下一条初始化声明应该使用的访问序号。
     */
    val nextVisitOrder: Int,
)

/**
 * static init 经由 static/global 函数间接读取变量形成的使用边。
 */
private data class StaticGlobalUseEdge(
    /**
     * 发起读取的初始化条目访问序号。
     */
    val ownerVisitOrder: Int,
    /**
     * 被间接读取的 static/global 存储变量。
     */
    val usedVariable: StaticGlobalInitializerVariable,
    /**
     * 触发读取的源码位置；没有精确位置时为空。
     */
    val source: org.cangnova.cangjie.source.CjSourceElement?,
    /**
     * 报告诊断时展示的被读取变量名称。
     */
    val diagnosticName: Name,
)

/**
 * 收集同一文件内参与 static/global 初始化顺序检查的声明。
 */
private fun CfirFile.staticGlobalInitializerDeclarations(): List<StaticGlobalInitializerDeclaration> {
    return buildList {
        for (declaration in declarations) {
            collectStaticGlobalInitializerDeclarations(declaration)
        }
    }
}

/**
 * 递归收集顶层变量声明和 class-like static 字段。
 */
private fun MutableList<StaticGlobalInitializerDeclaration>.collectStaticGlobalInitializerDeclarations(
    declaration: CfirDeclaration,
) {
    when (declaration) {
        is CfirFieldVariable -> if (declaration.isStaticOrGlobalInitializerField()) {
            add(declaration.toStaticGlobalInitializerDeclaration())
        }

        is CfirPatternVariable -> if (!declaration.isLocal && declaration.symbol.callableId.classId == null) {
            add(declaration.toStaticGlobalInitializerDeclaration())
        }

        is CfirClassLikeDeclaration -> collectClassLikeStaticGlobalInitializerDeclarations(declaration)

        else -> Unit
    }
}

/**
 * 按官方 GlobalVarChecker 的顺序收集 class-like static 成员：
 * 先收集所有 static 字段，再收集 static init。
 */
private fun MutableList<StaticGlobalInitializerDeclaration>.collectClassLikeStaticGlobalInitializerDeclarations(
    classLike: CfirClassLikeDeclaration,
) {
    for (member in classLike.declarations) {
        when (member) {
            is CfirFieldVariable -> if (member.status.isStatic) {
                add(member.toStaticGlobalInitializerDeclaration())
            }

            is CfirClassLikeDeclaration -> collectClassLikeStaticGlobalInitializerDeclarations(member)

            else -> Unit
        }
    }
    for (member in classLike.declarations) {
        if (member is CfirConstructor && member.isStaticConstructor) {
            add(member.toStaticGlobalInitializerDeclaration(classLike))
        }
    }
}

/**
 * 顶层字段或 static 成员字段都参与同文件初始化顺序。
 */
private fun CfirFieldVariable.isStaticOrGlobalInitializerField(): Boolean =
    status.isStatic || symbol.callableId.classId == null

/**
 * 将字段变量转换成文件级 static/global 初始化声明条目。
 */
private fun CfirFieldVariable.toStaticGlobalInitializerDeclaration(): StaticGlobalInitializerDeclaration {
    return StaticGlobalInitializerDeclaration(
        variables = listOf(
            StaticGlobalInitializerVariable(
                symbol = symbol,
                diagnosticName = name,
                nominalOwnerClassId = symbol.callableId.classId,
                field = this,
            )
        ),
        initializer = initializer,
        body = null,
        sourceOffset = source?.startOffset ?: Int.MAX_VALUE,
        nominalOwnerClassId = symbol.callableId.classId,
        kind = StaticGlobalInitializerKind.VARIABLE,
    )
}

/**
 * 将顶层 pattern variable 转换成文件级 static/global 初始化声明条目。
 *
 * 顶层 `var a = ...` 在 CFIR 中通过 [CfirPatternVariable] 容器和内部
 * binding variable 暴露名称；初始化顺序必须跟踪真正进入作用域的绑定符号。
 */
private fun CfirPatternVariable.toStaticGlobalInitializerDeclaration(): StaticGlobalInitializerDeclaration {
    val variables = pattern.bindingVariables().map { bindingVariable ->
        StaticGlobalInitializerVariable(
            symbol = bindingVariable.symbol,
            diagnosticName = bindingVariable.name,
            nominalOwnerClassId = bindingVariable.symbol.callableId.classId,
            field = null,
        )
    }

    return StaticGlobalInitializerDeclaration(
        variables = variables,
        initializer = initializer,
        body = null,
        sourceOffset = source?.startOffset ?: Int.MAX_VALUE,
        nominalOwnerClassId = symbol.callableId.classId,
        kind = StaticGlobalInitializerKind.VARIABLE,
    )
}

/**
 * 将 static init 转换成文件级 static/global 初始化声明条目。
 */
private fun CfirConstructor.toStaticGlobalInitializerDeclaration(
    owner: CfirClassLikeDeclaration,
): StaticGlobalInitializerDeclaration {
    return StaticGlobalInitializerDeclaration(
        variables = emptyList(),
        initializer = null,
        body = body,
        sourceOffset = source?.startOffset ?: Int.MAX_VALUE,
        nominalOwnerClassId = owner.symbol.classId,
        kind = StaticGlobalInitializerKind.STATIC_INIT,
    )
}

/**
 * 判断当前初始化声明和目标变量是否属于同一个 nominal owner。
 *
 * 同一 class-like 内的 static 字段顺序由 class-like 初始化检查器处理，文件级
 * 检查器只负责 top-level 与跨 class-like 的 static/global 顺序。
 */
private fun StaticGlobalInitializerDeclaration.hasSameNominalOwnerAs(
    other: StaticGlobalInitializerVariable,
): Boolean = nominalOwnerClassId != null && nominalOwnerClassId == other.nominalOwnerClassId

/**
 * 将同一个初始化声明包含的所有存储符号标记为已初始化。
 */
private fun StaticGlobalInitializerDeclaration.markVariablesInitialized(
    state: InitializationState,
): InitializationState {
    var current = state
    for (variable in variables) {
        variable.initialized = true
        current = current.markInitialized(variable.symbol)
    }
    return current
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
 * 将 static 字段变量转换为初始化跟踪信息。
 */
private fun CfirFieldVariable.toTrackedStaticFieldInfo(): TrackedVariableInfo =
    TrackedVariableInfo(
        symbol = symbol,
        diagnosticName = name,
        isInstanceField = false,
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
 * 解析 static init 可递归进入的 static/global 函数。
 */
private fun CfirFunctionCall.resolvedStaticOrGlobalFunctionOrNull(): CfirFunction? {
    val function = when (val symbol = resolvedCallableSymbolOrNull()) {
        is CfirNamedFunctionSymbol -> if (symbol.isBound) symbol.cfir else null
        else -> null
    } ?: return null

    return if (function.symbol.callableId.classId == null || function.status.isStatic) {
        function
    } else {
        null
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
 * static 变量初始化器专用的非 static 成员分类。
 *
 * 官方 `CheckStaticVarAccessNonStatic` 明确跳过函数声明，只把非 static、
 * 非全局、带 nominal owner 的存储成员/属性作为 static 变量非法访问。
 */
private fun CfirBasedSymbol<*>.nonStaticMemberNameForStaticVariableDiagnostic(): Name? {
    return when (this) {
        is CfirVariableSymbol<*> -> if (isBound && callableId.classId != null && cfir is CfirFieldVariable && !cfir.status.isStatic) {
            name
        } else {
            null
        }

        is CfirPropertySymbol -> if (isBound && callableId.classId != null && !cfir.status.isStatic) {
            name
        } else {
            null
        }

        is CfirPropertyAccessorSymbol -> if (isBound) {
            propertySymbol.nonStaticMemberNameForStaticVariableDiagnostic()
        } else {
            null
        }

        else -> null
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
