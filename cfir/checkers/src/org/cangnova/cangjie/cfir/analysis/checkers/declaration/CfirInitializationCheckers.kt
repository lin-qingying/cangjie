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
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
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
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap

/**
 * 初始化赋值在当前控制流中的事实分类。
 *
 * 赋值合法性检查器只消费这一层的事实，不再根据源码偏移量猜测“此前是否写过”。
 */
internal enum class CfirInitializationAssignmentKind {
    /** 当前赋值是该存储在所有可达路径上的首次初始化。 */
    INITIALIZATION,

    /** 当前赋值可能在此前已经完成初始化后再次执行。 */
    REASSIGNMENT,

    /** 赋值伴随初始化优先级错误，不能再追加不可变赋值诊断。 */
    PRIORITY_INITIALIZATION_DIAGNOSTIC,

    /** 当前赋值不属于初始化分析跟踪的存储。 */
    NOT_TRACKED,

    /** 用于合并多次观察结果的稳定优先级。 */
    ;

    internal val priority: Int
        get() = when (this) {
            NOT_TRACKED -> 0
            INITIALIZATION -> 1
            REASSIGNMENT -> 2
            PRIORITY_INITIALIZATION_DIAGNOSTIC -> 3
        }
}

/**
 * 按当前声明栈恢复函数所属的 class-like。
 *
 * 声明 checker 在进入函数节点前触发，当前函数本身通常尚未压入栈；而分析嵌套函数时
 * 栈中还可能同时存在多个 function。这里优先使用函数之前的声明，再从内向外寻找
 * 最近的 class-like，避免把嵌套函数误绑定到错误的 owner。
 *
 * 声明栈保存的是 [CfirBasedSymbol]，因此定位以函数符号身份为准，再把 class-like 符号投影回声明节点。
 */
private fun CheckerContext.ownerOf(function: CfirFunction): CfirClassLikeDeclaration? {
    val functionSymbol = function.symbol
    val functionIndex = containingDeclarations.indexOfLast { declaration -> declaration === functionSymbol }
    val declarationsBeforeFunction = if (functionIndex >= 0) {
        containingDeclarations.take(functionIndex)
    } else {
        containingDeclarations
    }
    return declarationsBeforeFunction.asReversed()
        .filterIsInstance<CfirClassLikeSymbol<*>>()
        .firstOrNull()
        ?.cfir
}

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
     * 可选的赋值初始化分类表，用于只计算初始化事实而不报告诊断的调用路径。
     *
     * 同一赋值可能在循环回访语义下由“首次初始化”升级为“可能重复写入”，
     * 因此这里保存分类而不是单一的初始化赋值集合。
     */
    private val assignmentClassifications: MutableMap<CfirAssignment, CfirInitializationAssignmentKind>? = null,
) {
    /**
     * 当前正在收集普通函数出口的函数及其显式 return 快照。
     *
     * 该上下文只在构造器完整性检查中启用。return 的 target 必须与当前函数一致，
     * 因而嵌套具名函数、匿名函数内部的 return 不会污染外层构造器的出口集合。
     */
    private var functionExitCollection: FunctionExitCollection? = null

    /**
     * 当前分析器已经报告的初始化流诊断数。
     *
     * 官方初始化检查对二元表达式按 `left && right` 组合检查结果：左操作数已经
     * 发现初始化错误时，不再从右操作数级联同一表达式的后续初始化诊断。计数器
     * 只用于识别一次子表达式分析是否新增了初始化流诊断，不改变控制流状态。
     */
    private var reportedInitializationDiagnosticCount: Int = 0

    /**
     * 检查函数或构造器体内的初始化读取语义。
     */
    fun checkFunction(function: CfirFunction) {
        val body = function.body ?: return
        val owner = if (function is CfirConstructor && function.isInstanceConstructor) {
            context.ownerOf(function)
        } else {
            null
        }
        analyzeFunctionBody(function, body, owner)
    }

    /**
     * 收集函数体内每个受初始化流管理的赋值分类。
     */
    fun collectAssignmentClassifications(function: CfirFunction): Map<CfirAssignment, CfirInitializationAssignmentKind> {
        val body = function.body ?: return emptyMap()
        val owner = if (function is CfirConstructor && function.isInstanceConstructor) {
            context.ownerOf(function)
        } else {
            null
        }
        analyzeFunctionBody(function, body, owner)
        return assignmentClassifications.orEmpty()
    }

    /**
     * 收集文件级 static/global 初始化序列中的赋值分类。
     */
    fun collectFileAssignmentClassifications(file: CfirFile): Map<CfirAssignment, CfirInitializationAssignmentKind> {
        checkFileStaticGlobalInitialization(file)
        return assignmentClassifications.orEmpty()
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
                kind = TrackedVariableKind.STATIC_OR_GLOBAL,
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
                        val ownerVisitOrder = declaration.variables.minOfOrNull { variable -> variable.visitOrder }
                            ?: declaration.visitOrder
                        collectRecursiveStaticFunctionReads(
                            root = initializer,
                            ownerVisitOrder = ownerVisitOrder,
                            trackedBySymbol = trackedBySymbol,
                            destination = recursiveStaticFunctionReads,
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
                reportInstanceMemberInitializerStaticGlobalReadsBeforeInitialization(initializer)
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

        val analysis = analyzeFunctionBody(
            function = constructor,
            body = body,
            owner = owner,
            collectFunctionExits = true,
        )
        val normalExitStates = buildList {
            addAll(analysis.returnExitStates)
            if (!analysis.endState.terminated) {
                add(analysis.endState)
            }
        }
        if (normalExitStates.isEmpty()) return

        owner.instanceFieldInfos(context)
            .filter { fieldInfo ->
                normalExitStates.any { exitState -> !exitState.isInitialized(fieldInfo.symbol) }
            }
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
        collectFunctionExits: Boolean = false,
    ): FunctionInitializationAnalysis {
        val parameterInfos = function.valueParameters.map { parameter ->
            TrackedVariableInfo(
                symbol = parameter.symbol,
                diagnosticName = parameter.name,
                kind = TrackedVariableKind.LOCAL_VARIABLE,
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

        val initialState = InitializationState.empty(expectedReceiver = owner?.symbol)
            .declareAll(parameterInfos, parameterInfos.map(TrackedVariableInfo::symbol).toSet())
            .declareAll(fieldInfos, preInitializedFields)

        if (!collectFunctionExits) {
            return FunctionInitializationAnalysis(
                endState = analyzeStatements(body.statements, initialState),
                returnExitStates = emptyList(),
            )
        }

        val previousCollection = functionExitCollection
        val currentCollection = FunctionExitCollection(function)
        functionExitCollection = currentCollection
        return try {
            FunctionInitializationAnalysis(
                endState = analyzeStatements(body.statements, initialState),
                returnExitStates = currentCollection.returnExitStates.toList(),
            )
        } finally {
            functionExitCollection = previousCollection
        }
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
        is CfirFunction -> analyzeNestedFunctionDeclaration(statement, state)
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
                    kind = TrackedVariableKind.LOCAL_VARIABLE,
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
                kind = TrackedVariableKind.INSTANCE_MEMBER,
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
            functionExitCollection
                ?.takeIf { collection -> expression.target.labeledElement === collection.function }
                ?.returnExitStates
                ?.add(afterResult)
            afterResult.terminate()
        }
        is CfirThrowExpression -> analyzeExpression(expression.exception, state).terminate()
        is CfirFunctionCall -> analyzeFunctionCall(expression, state)
        is CfirAnonymousFunctionExpression -> analyzeAnonymousFunctionExpression(expression, state)
        is CfirNamedAccessExpression -> analyzeVariableRead(expression, state, accessMode)
        is CfirQualifiedAccessExpression -> analyzeQualifiedAccess(expression, state, accessMode)
        is CfirBlock -> analyzeScopedBlock(expression, state)
        is CfirBinaryOp -> analyzeBinaryOperands(expression.left, expression.right, state)
        is CfirComparisonExpression -> analyzeBinaryOperands(expression.left, expression.right, state)
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
        val diagnosticCountBeforeRightValue = reportedInitializationDiagnosticCount
        val afterRightValue = analyzeExpression(assignment.rValue, state)
        val rightValueHasPriorityDiagnostic =
            reportedInitializationDiagnosticCount != diagnosticCountBeforeRightValue
        return analyzeAssignmentTarget(
            assignment = assignment,
            lValue = assignment.lValue,
            state = afterRightValue,
            priorityDiagnostic = rightValueHasPriorityDiagnostic,
        )
    }

    /**
     * 分析赋值左值。
     */
    private fun analyzeAssignmentTarget(
        assignment: CfirAssignment,
        lValue: CfirExpression,
        state: InitializationState,
        priorityDiagnostic: Boolean = false,
    ): InitializationState = when (lValue) {
        is CfirQualifiedAccessExpression -> analyzeAssignmentTargetAccess(
            assignment = assignment,
            access = lValue,
            state = state,
            priorityDiagnostic = priorityDiagnostic,
        )

        is CfirTupleLiteral -> lValue.elements.fold(state) { currentState, element ->
            analyzeAssignmentTarget(assignment, element, currentState, priorityDiagnostic)
        }

        else -> analyzeExpression(lValue, state)
    }

    /**
     * 按官方初始化检查的首错规则分析二元表达式。
     *
     * 左操作数中的初始化错误不阻止类型检查继续工作，但初始化检查器自身不再遍历
     * 右操作数，从而避免 `b - a - a` 在首个 `b` 之后继续级联未初始化诊断。
     */
    private fun analyzeBinaryOperands(
        left: CfirExpression,
        right: CfirExpression,
        state: InitializationState,
    ): InitializationState {
        val diagnosticCountBeforeLeft = reportedInitializationDiagnosticCount
        val afterLeft = analyzeExpression(left, state)
        if (reportedInitializationDiagnosticCount != diagnosticCountBeforeLeft) return afterLeft
        return analyzeExpression(right, afterLeft)
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
        priorityDiagnostic: Boolean,
    ): InitializationState {
        val diagnosticCountBeforeReceiver = reportedInitializationDiagnosticCount
        val afterReceiver = access.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state
        val targetPriorityDiagnostic = priorityDiagnostic ||
                reportedInitializationDiagnosticCount != diagnosticCountBeforeReceiver
        val symbol = access.resolvedAccessSymbolOrNull() ?: return afterReceiver
        val trackedVariable = afterReceiver.trackedVariable(symbol)

        return when {
            trackedVariable != null && trackedVariable.kind == TrackedVariableKind.INSTANCE_MEMBER &&
                    !access.isInitializationReceiverFor(afterReceiver.expectedReceiver) -> {
                // 其他实例上的字段不能推进当前构造器的 this 状态。
                if (targetPriorityDiagnostic) {
                    recordAssignmentClassification(
                        assignment,
                        CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
                    )
                }
                afterReceiver
            }

            trackedVariable != null -> {
                val nestedInitializerAccessKind =
                    afterReceiver.illegalMemberAccessKindFromNestedInitializer(symbol)
                if (nestedInitializerAccessKind != null) {
                    reportIllegalMemberAccessFromNestedInitializer(
                        accessKind = nestedInitializerAccessKind,
                        diagnosticName = access.calleeReference.referenceNameOrNull()
                            ?: symbol.nameOrNull()
                            ?: Name.ERROR_NAME,
                        source = access.calleeReference.source ?: access.source,
                    )
                    recordAssignmentClassification(
                        assignment,
                        CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
                    )
                    return afterReceiver
                }
                if (afterReceiver.shouldReportCaptureBeforeInitialization(symbol)) {
                    reportCaptureBeforeInitialization(
                        diagnosticName = access.calleeReference.referenceNameOrNull()
                            ?: symbol.nameOrNull()
                            ?: Name.ERROR_NAME,
                        source = access.calleeReference.source ?: access.source,
                    )
                    recordAssignmentClassification(
                        assignment,
                        CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
                    )
                    return afterReceiver
                }

                if (targetPriorityDiagnostic) {
                    recordAssignmentClassification(
                        assignment,
                        CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
                    )
                } else {
                    val classification = if (
                        afterReceiver.isPossiblyInitialized(symbol) || afterReceiver.mayRevisitAssignment(symbol)
                    ) {
                        CfirInitializationAssignmentKind.REASSIGNMENT
                    } else {
                        CfirInitializationAssignmentKind.INITIALIZATION
                    }
                    recordAssignmentClassification(assignment, classification)
                }
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
     * 记录赋值分类，并保留同一赋值在不同控制流观察下的最高优先级事实。
     */
    private fun recordAssignmentClassification(
        assignment: CfirAssignment,
        classification: CfirInitializationAssignmentKind,
    ) {
        val classifications = assignmentClassifications ?: return
        val previous = classifications[assignment]
        if (previous == null || classification.priority > previous.priority) {
            classifications[assignment] = classification
        }
    }

    /**
     * 判断赋值目标的有效接收者是否绑定到当前构造器的 `this`。
     *
     * 无显式接收者的成员写入由解析阶段提供隐式 dispatch receiver；在错误恢复节点
     * 中该 receiver 可能为空，此时只要当前状态确实处于实例构造器上下文即可按当前
     * owner 解释。显式的其他实例永远不能推进当前对象的字段初始化状态。
     */
    private fun CfirQualifiedAccessExpression.isInitializationReceiverFor(
        expectedReceiver: CfirThisOwnerSymbol<*>?,
    ): Boolean {
        if (expectedReceiver == null) return false
        val receiver = explicitReceiver ?: dispatchReceiver ?: return true
        val unwrappedReceiver = receiver.unwrapSmartcastExpression()
        return (unwrappedReceiver as? CfirThisReceiverExpression)
            ?.calleeReference
            ?.boundSymbol == expectedReceiver
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
                        kind = TrackedVariableKind.LOCAL_VARIABLE,
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
            val repeatableBodyState = analyzeScopedBlock(
                expression.body,
                state.enterRepeatableRegion(),
            ).restoreRepeatableDepth(state.repeatableDepth)
            val afterFirstBody = repeatableBodyState.withoutTermination()
            val afterCondition = analyzeExpression(expression.condition, afterFirstBody)
            if (repeatableBodyState.terminated) afterCondition.terminate() else afterCondition
        } else {
            val afterCondition = analyzeExpression(expression.condition, state)
            val afterBody = analyzeScopedBlock(
                expression.body,
                afterCondition.enterRepeatableRegion(),
            ).restoreRepeatableDepth(afterCondition.repeatableDepth)
            mergeBranchStates(afterCondition, afterBody)
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
        val loopState = expression.variable.pattern.bindingVariables().fold(
            afterIterable.enterRepeatableRegion(),
        ) { currentState, bindingVariable ->
            currentState.declare(
                TrackedVariableInfo(
                    symbol = bindingVariable.symbol,
                    diagnosticName = bindingVariable.name,
                    kind = TrackedVariableKind.LOCAL_VARIABLE,
                ),
                initialized = true,
            )
        }
        val afterBody = analyzeScopedBlock(expression.body, loopState)
            .restoreRepeatableDepth(afterIterable.repeatableDepth)
            .retainOnly(afterIterable.tracked.keys)
        return mergeBranchStates(afterIterable, afterBody)
    }

    /**
     * 嵌套具名函数声明不会顺序执行其函数体；这里仅分析捕获语义，不推进外层初始化状态。
     */
    private fun analyzeNestedFunctionDeclaration(
        function: CfirFunction,
        state: InitializationState,
    ): InitializationState {
        analyzeNestedFunctionBody(function, state)
        return state
    }

    /**
     * 匿名函数表达式创建闭包时需要检查捕获，但闭包体不是外层控制流的顺序语句。
     */
    private fun analyzeAnonymousFunctionExpression(
        expression: CfirAnonymousFunctionExpression,
        state: InitializationState,
    ): InitializationState {
        analyzeNestedFunctionBody(expression.anonymousFunction, state)
        return state
    }

    /**
     * 以独立函数上下文分析嵌套函数的默认参数与函数体。
     *
     * 默认参数在函数体之前、按参数声明顺序求值；参数声明只进入当前嵌套函数状态，
     * 不会推进外层初始化流。成员初始化器中的实例成员访问与普通局部变量捕获在
     * report 阶段分类，分别对应官方 illegal-member 与 capture-before-init 语义。
     */
    private fun analyzeNestedFunctionBody(
        function: CfirFunction,
        outerState: InitializationState,
    ) {
        var nestedState = outerState.withNestedFunctionContext()
        for (parameter in function.valueParameters) {
            parameter.defaultValue?.let { defaultValue ->
                nestedState = analyzeExpression(defaultValue, nestedState)
            }
            nestedState = nestedState.declare(
                trackedVariable = TrackedVariableInfo(
                    symbol = parameter.symbol,
                    diagnosticName = parameter.name,
                    kind = TrackedVariableKind.LOCAL_VARIABLE,
                ),
                initialized = true,
            )
        }
        function.body?.let { body -> analyzeStatements(body.statements, nestedState) }
    }

    /**
     * 分析函数调用表达式。
     */
    private fun analyzeFunctionCall(
        expression: CfirFunctionCall,
        state: InitializationState,
    ): InitializationState {
        val diagnosticCountBeforeReceiver = reportedInitializationDiagnosticCount
        var currentState = expression.explicitReceiver?.let { receiver ->
            analyzeExpression(receiver, state)
        } ?: state
        if (reportedInitializationDiagnosticCount != diagnosticCountBeforeReceiver) return currentState

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
            val diagnosticCountBeforeArgument = reportedInitializationDiagnosticCount
            currentState = analyzeExpression(argument, currentState)
            if (reportedInitializationDiagnosticCount != diagnosticCountBeforeArgument) break
        }
        if (expression.origin == CfirFunctionCallOrigin.ConstructorDelegationThis) {
            currentState = currentState.markAllInstanceFieldsInitialized()
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
            else -> if (afterReceiver.isTracked(symbol)) {
                reportReadIfNeeded(
                    symbol = symbol,
                    diagnosticName = expression.calleeReference.referenceNameOrNull()
                        ?: symbol.nameOrNull()
                        ?: Name.ERROR_NAME,
                    source = expression.calleeReference.source ?: expression.source,
                    state = afterReceiver,
                )
            } else if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver)) {
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
            else -> if (afterReceiver.isTracked(symbol)) {
                reportReadIfNeeded(
                    symbol = symbol,
                    diagnosticName = expression.calleeReference.referenceNameOrNull()
                        ?: symbol.nameOrNull()
                        ?: Name.ERROR_NAME,
                    source = expression.calleeReference.source ?: expression.source,
                    state = afterReceiver,
                )
            } else if (expression.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver)) {
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
     * 字段初始化器中非法 `super.member` 与 struct 显式 `this.member` 已由专门 checker 分类。
     * 隐式 `this` 成员函数/属性捕获仍属于初始化流，应继续报告初始化前访问。
     */
    private fun CfirQualifiedAccessExpression.shouldSkipIllegalMemberAccessInMemberInitializer(
        state: InitializationState,
    ): Boolean {
        if (!state.inMemberInitializer) return false
        if (hasSuperReceiver()) return true
        return state.memberInitializerOwner is CfirStruct && hasExplicitThisReceiver()
    }

    private fun CfirQualifiedAccessExpression.hasSuperReceiver(): Boolean =
        explicitReceiver is CfirSuperReceiverExpression || dispatchReceiver is CfirSuperReceiverExpression

    private fun CfirQualifiedAccessExpression.hasExplicitThisReceiver(): Boolean {
        val explicit = explicitReceiver as? CfirThisReceiverExpression
        val dispatch = dispatchReceiver as? CfirThisReceiverExpression
        return explicit?.calleeReference?.isImplicit == false ||
                dispatch?.calleeReference?.isImplicit == false
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
                    is CfirFunction -> analyzeNestedFunctionDeclaration(element, currentState)
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
        symbol: CfirBasedSymbol<*>,
        diagnosticName: Name,
        source: org.cangnova.cangjie.source.CjSourceElement?,
        state: InitializationState,
    ): InitializationState {
        val nestedInitializerAccessKind = state.illegalMemberAccessKindFromNestedInitializer(symbol)
        if (nestedInitializerAccessKind != null) {
            reportIllegalMemberAccessFromNestedInitializer(
                accessKind = nestedInitializerAccessKind,
                diagnosticName = diagnosticName,
                source = source,
            )
            return state
        }
        if (!state.isTracked(symbol) || state.isInitialized(symbol)) return state
        if (state.shouldReportCaptureBeforeInitialization(symbol)) {
            reportCaptureBeforeInitialization(diagnosticName, source)
            return state
        }
        reportUsedBeforeInitialization(diagnosticName, source)
        return state
    }

    /**
     * 报告初始化完成前的变量读取或成员访问。
     */
    private fun reportUsedBeforeInitialization(
        diagnosticName: Name,
        source: org.cangnova.cangjie.source.CjSourceElement?,
    ) {
        reportedInitializationDiagnosticCount++
        if (reportReadDiagnostics) {
            with(context) {
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.USED_BEFORE_INITIALIZATION,
                    a = diagnosticName,
                )
            }
        }
    }

    /**
     * 报告嵌套函数或匿名函数捕获尚未初始化的局部变量。
     */
    private fun reportCaptureBeforeInitialization(
        diagnosticName: Name,
        source: org.cangnova.cangjie.source.CjSourceElement?,
    ) {
        reportedInitializationDiagnosticCount++
        if (reportReadDiagnostics) {
            with(context) {
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.CAPTURE_BEFORE_INITIALIZATION,
                    a = diagnosticName,
                )
            }
        }
    }

    /**
     * 报告成员初始化器嵌套 callable 对当前实例或继承实例存储的非法捕获。
     *
     * 该语义与普通未初始化读取不同：对象仍处于成员初始化阶段，闭包可能在完整对象
     * 构造前逃逸，因此必须保留官方独立诊断，不能降级为 USED_BEFORE_INITIALIZATION。
     */
    private fun reportIllegalMemberAccessFromNestedInitializer(
        accessKind: NestedInitializerMemberAccessKind,
        diagnosticName: Name,
        source: org.cangnova.cangjie.source.CjSourceElement?,
    ) {
        reportedInitializationDiagnosticCount++
        if (!reportReadDiagnostics) return

        with(context) {
            when (accessKind) {
                NestedInitializerMemberAccessKind.CURRENT_MEMBER -> reporter.reportOn(
                    source = source,
                    factory = CfirErrors.ILLEGAL_USAGE_OF_MEMBER,
                    a = diagnosticName,
                )

                NestedInitializerMemberAccessKind.SUPER_MEMBER -> reporter.reportOn(
                    source = source,
                    factory = CfirErrors.ILLEGAL_USAGE_OF_SUPER_MEMBER,
                    a = diagnosticName,
                )
            }
        }
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
        val assignedStaticVariables = linkedSetOf<CfirBasedSymbol<*>>()

        order = collectStaticInitializerVariableDependencies(
            declaration = declaration,
            body = body,
            trackedBySymbol = trackedBySymbol,
            nextVisitOrder = order,
            assignedStaticVariables = assignedStaticVariables,
            destination = recursiveStaticFunctionReads,
        )

        state = analyzeStatements(body.statements, state)
        for (variable in trackedBySymbol.values) {
            if (variable.initialized) continue
            if (!state.isInitialized(variable.symbol)) continue
            variable.initialized = true
            if (variable.symbol.initializationSymbol() !in assignedStaticVariables) {
                variable.visitOrder = order++
            }
        }

        declaration.visitOrder = order++
        collectRecursiveStaticFunctionReads(body, declaration.visitOrder, trackedBySymbol, recursiveStaticFunctionReads)
        return StaticInitializerProcessingResult(state, order)
    }

    /**
     * 为 `static init` 中首次初始化 static 字段的赋值建立字段级依赖图根。
     *
     * 官方 `GlobalVarChecker::CollectForStaticInit` 会在 `field = rhs` 处把当前
     * `DefNode` 切换为 field，并只从 rhs 收集递归 callable 依赖。字段是否在所有
     * 控制流路径上完成初始化仍由 [analyzeStatements] 决定；这里仅维护图顺序，
     * 不能把分支或未调用嵌套函数中的写入当成 definite assignment。
     */
    private fun collectStaticInitializerVariableDependencies(
        declaration: StaticGlobalInitializerDeclaration,
        body: CfirBlock,
        trackedBySymbol: Map<CfirBasedSymbol<*>, StaticGlobalInitializerVariable>,
        nextVisitOrder: Int,
        assignedStaticVariables: MutableSet<CfirBasedSymbol<*>>,
        destination: MutableList<StaticGlobalUseEdge>,
    ): Int {
        var order = nextVisitOrder

        body.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            // 声明 callable 不表示执行；只有立即调用的 lambda 才进入当前 static init 路径。
            override fun visitFunction(function: CfirFunction) = Unit

            override fun visitProperty(property: CfirProperty) = Unit

            override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                val receiver = functionCall.explicitReceiver
                if (receiver is CfirAnonymousFunctionExpression) {
                    receiver.anonymousFunction.body?.accept(this, null)
                } else {
                    receiver?.accept(this, null)
                }
                functionCall.argumentList.arguments.forEach { argument -> argument.accept(this, null) }
            }

            override fun visitAssignment(assignment: CfirAssignment) {
                val targetSymbol = (assignment.lValue as? CfirQualifiedAccessExpression)
                    ?.resolvedAccessSymbolOrNull()
                    ?.initializationSymbol()
                val targetVariable = targetSymbol?.let(trackedBySymbol::get)
                val isCurrentOwnerStaticField = targetVariable?.field?.status?.isStatic == true &&
                        targetVariable.nominalOwnerClassId == declaration.nominalOwnerClassId &&
                        !targetVariable.initialized

                if (
                    targetSymbol != null && targetVariable != null && isCurrentOwnerStaticField &&
                    assignedStaticVariables.add(targetSymbol)
                ) {
                    targetVariable.visitOrder = order++
                    collectRecursiveStaticFunctionReads(
                        root = assignment.rValue,
                        ownerVisitOrder = targetVariable.visitOrder,
                        trackedBySymbol = trackedBySymbol,
                        destination = destination,
                    )
                    return
                }

                assignment.rValue.accept(this, null)
                assignment.lValue.accept(this, null)
            }
        }, null)

        return order
    }

    /**
     * 收集 static init 可达 static 函数体里的 static/global 变量读取。
     */
    private fun collectRecursiveStaticFunctionReads(
        root: CfirElement,
        ownerVisitOrder: Int,
        trackedBySymbol: Map<CfirBasedSymbol<*>, StaticGlobalInitializerVariable>,
        destination: MutableList<StaticGlobalUseEdge>,
    ) {
        val visitedFunctions = linkedSetOf<CfirFunction>()
        var reachableCallableDepth = 0
        lateinit var visitor: org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

        fun collectFromFunction(function: CfirFunction) {
            if (!visitedFunctions.add(function)) return
            val functionBody = function.body ?: return
            reachableCallableDepth++
            try {
                functionBody.accept(visitor, null)
            } finally {
                reachableCallableDepth--
            }
        }

        visitor = object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            // callable 声明属于 called-later 子图，不能因普通树遍历而进入。
            override fun visitFunction(function: CfirFunction) = Unit

            override fun visitProperty(property: CfirProperty) = Unit

            override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

            override fun visitAssignment(assignment: CfirAssignment) {
                assignment.rValue.accept(this, null)
                visitWriteTarget(assignment.lValue)
            }

            override fun visitIncrementDecrementExpression(incrementDecrementExpression: CfirIncrementDecrementExpression) {
                val target = incrementDecrementExpression.expression
                if (target is CfirQualifiedAccessExpression) {
                    visitAccess(target, InitializationAccessMode.READ)
                    visitAccess(target, InitializationAccessMode.WRITE_TARGET, visitReceiver = false)
                } else {
                    target.accept(this, null)
                }
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                functionCall.resolvedInitializationCallableOrNull(InitializationAccessMode.READ)
                    ?.let(::collectFromFunction)
                visitFunctionCallReceiver(functionCall)
                for (argument in functionCall.argumentList.arguments) {
                    argument.accept(this, null)
                }
            }

            private fun visitFunctionCallReceiver(functionCall: CfirFunctionCall) {
                val receiver = functionCall.explicitReceiver
                if (receiver is CfirAnonymousFunctionExpression) {
                    collectFromFunction(receiver.anonymousFunction)
                } else {
                    receiver?.accept(this, null)
                }
            }

            override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                visitAccess(qualifiedAccessExpression, InitializationAccessMode.READ)
            }

            override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression) {
                visitAccess(namedAccessExpression, InitializationAccessMode.READ)
            }

            private fun visitWriteTarget(target: CfirExpression) {
                when (target) {
                    is CfirQualifiedAccessExpression -> visitAccess(target, InitializationAccessMode.WRITE_TARGET)
                    is CfirTupleLiteral -> target.elements.forEach(::visitWriteTarget)
                    else -> target.accept(this, null)
                }
            }

            /** 读 property 进入 getter，写目标进入 setter；变量写目标不形成 use edge。 */
            private fun visitAccess(
                access: CfirQualifiedAccessExpression,
                accessMode: InitializationAccessMode,
                visitReceiver: Boolean = true,
            ) {
                if (visitReceiver) {
                    access.explicitReceiver?.accept(this, null)
                }
                if (accessMode == InitializationAccessMode.READ && reachableCallableDepth > 0) {
                    collectUseEdge(access)
                }
                access.resolvedInitializationCallableOrNull(accessMode)?.let(::collectFromFunction)
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
        }

        root.accept(visitor, null)
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
     * 实例字段初始化器也不能读取同文件中后声明的顶层 global 变量。
     *
     * 官方 `IsVarUsedBeforeDefinition` 对这种普通作用域前向引用报错，但访问另一个
     * class-like 的 static 成员由 global/static 初始化图处理，不能按实例字段的源码偏移误判。
     */
    private fun reportInstanceMemberInitializerStaticGlobalReadsBeforeInitialization(
        initializer: CfirExpression,
    ) {
        val file = context.containingFileSymbol?.cfir ?: return
        val trackedBySymbol = file.staticGlobalInitializerDeclarations()
            .flatMap(StaticGlobalInitializerDeclaration::variables)
            .filter { variable -> variable.nominalOwnerClassId == null }
            .associateBy { variable -> variable.symbol.initializationSymbol() }
        if (trackedBySymbol.isEmpty()) return

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
                val targetVariable = trackedBySymbol[symbol] ?: return
                val accessOffset = access.calleeReference.source?.startOffset ?: access.source?.startOffset ?: return
                if (accessOffset >= targetVariable.sourceOffset) return

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
        if (!state.hasUninitializedInstanceFields()) return state
        if (state.inNestedFunction && !state.inMemberInitializer) return state
        if (!symbol.isInstanceMemberFunctionOrProperty()) return state
        reportedInitializationDiagnosticCount++
        if (reportReadDiagnostics) {
            with(context) {
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.USED_BEFORE_INITIALIZATION,
                    a = diagnosticName,
                )
            }
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
     * 同一诊断收集轮次内的函数级初始化事实缓存。
     *
     * 赋值合法性 checker 会逐个访问 assignment；若每次都重新遍历整个构造器，Record
     * 这类成员密集文件会退化为 O(n²)。缓存按 context 和声明 identity 隔离，避免跨文件
     * 泄漏符号事实。
     */
    private class ClassificationCache {
        val byFunction: IdentityHashMap<CfirFunction, Map<CfirAssignment, CfirInitializationAssignmentKind>> =
            IdentityHashMap()
        val byFile: IdentityHashMap<CfirFile, Map<CfirAssignment, CfirInitializationAssignmentKind>> =
            IdentityHashMap()
    }

    private val caches = Collections.synchronizedMap(WeakHashMap<CheckerContext, ClassificationCache>())

    /**
     * 返回赋值表达式在所有可见初始化 owner 中的统一分类。
     *
     * 内层函数先分析，外层构造器随后分析。这样嵌套 lambda 对外层未初始化存储的
     * 捕获可以升级为优先级分类，而不会被内层函数的普通“未跟踪”结果覆盖。
     */
    fun classifyAssignment(
        assignment: CfirAssignment,
        context: CheckerContext,
    ): CfirInitializationAssignmentKind {
        val cache = synchronized(caches) {
            caches.getOrPut(context) { ClassificationCache() }
        }
        var classification = CfirInitializationAssignmentKind.NOT_TRACKED
        val enclosingFunctions = context.containingDeclarations
            .filterIsInstance<CfirFunctionSymbol<*>>()
            .map { it.cfir }
            .asReversed()

        for (function in enclosingFunctions) {
            val functionClassifications = cache.byFunction[function] ?: run {
                val computed = linkedMapOf<CfirAssignment, CfirInitializationAssignmentKind>()
                CfirInitializationFlowAnalyzer(
                    context = context,
                    reporter = EmptyDiagnosticReporter,
                    reportReadDiagnostics = false,
                    assignmentClassifications = computed,
                ).collectAssignmentClassifications(function)
                computed.toMap().also { cache.byFunction[function] = it }
            }
            classification = classification.merge(functionClassifications[assignment])
        }

        context.containingFileSymbol?.cfir?.let { file ->
            val fileClassifications = cache.byFile[file] ?: run {
                val computed = linkedMapOf<CfirAssignment, CfirInitializationAssignmentKind>()
                CfirInitializationFlowAnalyzer(
                    context = context,
                    reporter = EmptyDiagnosticReporter,
                    reportReadDiagnostics = false,
                    assignmentClassifications = computed,
                ).collectFileAssignmentClassifications(file)
                computed.toMap().also { cache.byFile[file] = it }
            }
            classification = classification.merge(fileClassifications[assignment])
        }

        return classification
    }

    /** 合并不同 owner 观察到的初始化事实，保留优先级更高的结论。 */
    private fun CfirInitializationAssignmentKind.merge(
        other: CfirInitializationAssignmentKind?,
    ): CfirInitializationAssignmentKind {
        other ?: return this
        return if (other.priority > priority) other else this
    }

    /** 判断赋值是否属于不重复的首次初始化。 */
    fun isInitializationAssignment(
        assignment: CfirAssignment,
        context: CheckerContext,
    ): Boolean = classifyAssignment(assignment, context) == CfirInitializationAssignmentKind.INITIALIZATION

    /** 判断赋值是否因初始化优先级错误而不应追加不可变诊断。 */
    fun hasPriorityInitializationDiagnostic(
        assignment: CfirAssignment,
        context: CheckerContext,
    ): Boolean = classifyAssignment(assignment, context) ==
            CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC

    /** 判断赋值是否在流分析中被识别为后续重复写入。 */
    fun isReassignment(
        assignment: CfirAssignment,
        context: CheckerContext,
    ): Boolean = classifyAssignment(assignment, context) == CfirInitializationAssignmentKind.REASSIGNMENT
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
 * 初始化流中跟踪符号的语义种类。
 *
 * 局部变量、实例成员与 static/global 存储的闭包访问规则不同，不能只用
 * “是否为实例字段”反推捕获诊断，否则 static/global 会被误报为局部捕获。
 */
private enum class TrackedVariableKind(
    /**
     * 嵌套函数闭包是否会捕获该种类中尚未初始化的存储。
     */
    val canBeCapturedBeforeInitialization: Boolean,
) {
    /** 普通局部变量或参数。 */
    LOCAL_VARIABLE(canBeCapturedBeforeInitialization = true),

    /** 当前 class-like 或可见父类的实例存储成员。 */
    INSTANCE_MEMBER(canBeCapturedBeforeInitialization = true),

    /** static 字段或顶层 global 变量。 */
    STATIC_OR_GLOBAL(canBeCapturedBeforeInitialization = false),
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
     * 被跟踪符号的初始化语义种类。
     */
    val kind: TrackedVariableKind,

    /**
     * 声明发生时所处的可重复执行区域深度。
     *
     * 赋值若进入更深的循环区域，说明同一存储可能在一次执行中被回访，即使当前
     * 线性遍历尚未看到第二个语法赋值，也必须按重复赋值处理。
     */
    val declarationRepeatableDepth: Int = 0,
) {
    /**
     * 是否为实例字段或主构造成员属性。
     */
    val isInstanceField: Boolean
        get() = kind == TrackedVariableKind.INSTANCE_MEMBER
}

/**
 * 单个函数初始化流分析的出口结果。
 *
 * [endState] 表示函数体末尾的顺序流；[returnExitStates] 保存每个可达显式 return
 * 在退出函数前的初始化快照。构造器完整性必须同时检查这两类普通出口。
 */
private data class FunctionInitializationAnalysis(
    val endState: InitializationState,
    val returnExitStates: List<InitializationState>,
)

/**
 * 当前函数显式 return 出口的收集上下文。
 */
private data class FunctionExitCollection(
    val function: CfirFunction,
    val returnExitStates: MutableList<InitializationState> = mutableListOf(),
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
     * 至少一条可达路径上已经完成初始化的符号集合。
     *
     * [initialized] 负责 definite assignment；本集合负责识别可能重复初始化。
     * 分支合并时前者取交集、后者取并集。
     */
    val possiblyInitialized: Set<CfirBasedSymbol<*>>,

    /**
     * 当前实例字段初始化所匹配的 `this` owner。
     *
     * 只有写入该 receiver 的字段才能推进当前构造器状态；写入其他实例不能完成
     * 当前对象的字段初始化。
     */
    val expectedReceiver: CfirThisOwnerSymbol<*>?,

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

    /**
     * 当前是否在嵌套具名函数或匿名函数体内。
     */
    val inNestedFunction: Boolean,

    /**
     * 当前所在的可重复执行区域深度，用于识别循环中的潜在重复初始化。
     */
    val repeatableDepth: Int,
) {
    /**
     * 初始化状态工厂。
     */
    companion object {
        /**
         * 创建空初始化状态。
         */
        fun empty(expectedReceiver: CfirThisOwnerSymbol<*>? = null): InitializationState = InitializationState(
            tracked = emptyMap(),
            initialized = emptySet(),
            possiblyInitialized = emptySet(),
            expectedReceiver = expectedReceiver,
            terminated = false,
            inMemberInitializer = false,
            memberInitializerOwner = null,
            inNestedFunction = false,
            repeatableDepth = 0,
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
        val normalizedTrackedVariable = trackedVariable.copy(
            symbol = normalizedSymbol,
            declarationRepeatableDepth = repeatableDepth,
        )
        val nextTracked = tracked + (normalizedSymbol to normalizedTrackedVariable)
        val nextInitialized = if (initialized) {
            this.initialized + normalizedSymbol
        } else {
            this.initialized - normalizedSymbol
        }
        val nextPossiblyInitialized = if (initialized) {
            possiblyInitialized + normalizedSymbol
        } else {
            possiblyInitialized - normalizedSymbol
        }
        return copy(
            tracked = nextTracked,
            initialized = nextInitialized,
            possiblyInitialized = nextPossiblyInitialized,
        )
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
        return copy(
            initialized = initialized + normalizedSymbol,
            possiblyInitialized = possiblyInitialized + normalizedSymbol,
        )
    }

    /**
     * 标记当前状态跟踪的所有实例字段已经初始化。
     */
    fun markAllInstanceFieldsInitialized(): InitializationState {
        val instanceFieldSymbols = tracked
            .filterValues(TrackedVariableInfo::isInstanceField)
            .keys
        if (instanceFieldSymbols.isEmpty()) return this
        return copy(
            initialized = initialized + instanceFieldSymbols,
            possiblyInitialized = possiblyInitialized + instanceFieldSymbols,
        )
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
     * 判断指定符号是否可能已经初始化。
     */
    fun isPossiblyInitialized(symbol: CfirBasedSymbol<*>): Boolean =
        symbol.initializationSymbol() in possiblyInitialized

    /**
     * 取得指定符号的初始化跟踪信息。
     */
    fun trackedVariable(symbol: CfirBasedSymbol<*>): TrackedVariableInfo? =
        tracked[symbol.initializationSymbol()]

    /**
     * 判断赋值是否位于声明所在区域之外的可重复执行区域。
     */
    fun mayRevisitAssignment(symbol: CfirBasedSymbol<*>): Boolean {
        val variable = trackedVariable(symbol) ?: return false
        return variable.declarationRepeatableDepth < repeatableDepth
    }

    /**
     * 判断当前状态中是否仍有未初始化的实例字段。
     */
    fun hasUninitializedInstanceFields(): Boolean {
        return tracked.any { (symbol, variableInfo) ->
            variableInfo.isInstanceField && symbol !in initialized
        }
    }

    /**
     * 成员初始化器中的嵌套函数会捕获尚未完成构造的当前对象。
     *
     * 因此读取或写入当前类/父类实例成员时，即使该单个字段已经按声明顺序完成初始化，
     * 仍按官方 illegal-usage-of-member / super-member 语义报告成员非法访问。
     */
    fun illegalMemberAccessKindFromNestedInitializer(
        symbol: CfirBasedSymbol<*>,
    ): NestedInitializerMemberAccessKind? {
        if (!inMemberInitializer || !inNestedFunction) return null
        val normalizedSymbol = symbol.initializationSymbol()
        if (tracked[normalizedSymbol]?.kind != TrackedVariableKind.INSTANCE_MEMBER) return null

        val currentOwnerClassId = memberInitializerOwner?.symbol?.classId ?: return null
        val targetOwnerClassId = normalizedSymbol.initializationMemberOwnerClassId() ?: return null
        return if (targetOwnerClassId == currentOwnerClassId) {
            NestedInitializerMemberAccessKind.CURRENT_MEMBER
        } else {
            NestedInitializerMemberAccessKind.SUPER_MEMBER
        }
    }

    /**
     * 判断嵌套函数中是否需要报告捕获未初始化存储。
     *
     * 局部变量和构造器正在初始化的实例成员都属于闭包捕获；static/global
     * 拥有独立的初始化顺序规则。成员初始化器中的实例成员非法访问由调用方
     * 在进入该判断前优先分类，不会退化为捕获诊断。
     */
    fun shouldReportCaptureBeforeInitialization(symbol: CfirBasedSymbol<*>): Boolean {
        val normalizedSymbol = symbol.initializationSymbol()
        val variableInfo = tracked[normalizedSymbol] ?: return false
        return inNestedFunction &&
                variableInfo.kind.canBeCapturedBeforeInitialization &&
                normalizedSymbol !in initialized
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
     * 进入嵌套函数体上下文。
     */
    fun withNestedFunctionContext(): InitializationState =
        if (inNestedFunction) this else copy(inNestedFunction = true)

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
            possiblyInitialized = possiblyInitialized.union(other.possiblyInitialized).filterTo(linkedSetOf()) { symbol ->
                symbol in sharedTrackedSymbols
            },
            expectedReceiver = commonExpectedReceiver(other),
            terminated = terminated && other.terminated,
            inMemberInitializer = inMemberInitializer && other.inMemberInitializer,
            memberInitializerOwner = commonMemberInitializerOwner(other),
            inNestedFunction = inNestedFunction && other.inNestedFunction,
            repeatableDepth = minOf(repeatableDepth, other.repeatableDepth),
        )
    }

    private fun commonExpectedReceiver(other: InitializationState): CfirThisOwnerSymbol<*>? =
        expectedReceiver.takeIf { it == other.expectedReceiver }

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
            possiblyInitialized = possiblyInitialized.filterTo(linkedSetOf()) { symbol ->
                symbol in normalizedVisibleSymbols
            },
        )
    }

    /**
     * 进入一个可能执行多次的循环区域。
     */
    fun enterRepeatableRegion(): InitializationState = copy(repeatableDepth = repeatableDepth + 1)

    /**
     * 将循环体结果恢复到外层重复区域深度。
     */
    fun restoreRepeatableDepth(depth: Int): InitializationState =
        if (repeatableDepth == depth) this else copy(repeatableDepth = depth)

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

/** 成员初始化器嵌套 callable 的实例成员访问归属。 */
private enum class NestedInitializerMemberAccessKind {
    /** 访问当前 class-like 自身声明的实例成员。 */
    CURRENT_MEMBER,

    /** 访问可见父类声明的实例成员。 */
    SUPER_MEMBER,
}

/**
 * 取得初始化跟踪符号的真实声明 owner。
 *
 * substitution/fake/delegated override 只是使用点视图，成员归属必须回到原声明后再与
 * 当前成员初始化器 owner 比较，否则继承成员会被误分类为当前成员。
 */
private fun CfirBasedSymbol<*>.initializationMemberOwnerClassId(): ClassId? {
    val callable = when (this) {
        is CfirVariableSymbol<*> -> takeIf { isBound }?.cfir as? CfirCallableDeclaration
        is CfirPropertySymbol -> takeIf { isBound }?.cfir as? CfirCallableDeclaration
        is CfirPropertyAccessorSymbol -> takeIf { isBound }?.cfir as? CfirCallableDeclaration
        else -> null
    }
    return callable?.unwrapFakeOverridesOrDelegated()?.symbol?.callableId?.classId
        ?: when (this) {
            is CfirVariableSymbol<*> -> callableId.classId
            is CfirPropertySymbol -> callableId.classId
            is CfirPropertyAccessorSymbol -> propertySymbol.callableId.classId
            else -> null
        }
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
        .filter { field -> this !is CfirInterface || field.status.isStatic }
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
     * 变量声明在源文件中的起始偏移量。
     */
    val sourceOffset: Int,
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
                sourceOffset = source?.startOffset ?: Int.MAX_VALUE,
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
            sourceOffset = bindingVariable.source?.startOffset ?: source?.startOffset ?: Int.MAX_VALUE,
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
    // interface 中 let/var 字段是语法级非法声明；错误恢复节点不能进入实例字段初始化语义。
    if (this is CfirInterface) return emptyList()

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
        kind = TrackedVariableKind.INSTANCE_MEMBER,
    )

/**
 * 将 static 字段变量转换为初始化跟踪信息。
 */
private fun CfirFieldVariable.toTrackedStaticFieldInfo(): TrackedVariableInfo =
    TrackedVariableInfo(
        symbol = symbol,
        diagnosticName = name,
        kind = TrackedVariableKind.STATIC_OR_GLOBAL,
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
            kind = TrackedVariableKind.INSTANCE_MEMBER,
        )
    }
}

/**
 * 收集带 initializer 的实例字段。
 */
private fun CfirClassLikeDeclaration.instanceFieldsWithInitializer(): List<CfirFieldVariable> {
    if (this is CfirInterface) return emptyList()

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

/** 从调用表达式解析初始化依赖图需要进入的真实 callable。 */
private fun CfirFunctionCall.resolvedInitializationCallableOrNull(
    accessMode: InitializationAccessMode,
): CfirFunction? = resolvedCallableSymbolOrNull()?.resolvedInitializationCallableOrNull(accessMode)

/** 从普通访问表达式解析初始化依赖图需要进入的真实 callable。 */
private fun CfirQualifiedAccessExpression.resolvedInitializationCallableOrNull(
    accessMode: InitializationAccessMode,
): CfirFunction? = resolvedAccessSymbolOrNull()?.resolvedInitializationCallableOrNull(accessMode)

/**
 * 将解析符号映射为 global/static 初始化图中的 callable 子图。
 *
 * property 读取只进入 getter，赋值目标只进入 setter；普通函数、构造器和访问器
 * 都回到 substitution/fake/delegated override 的真实声明，避免使用点视图丢失 body。
 */
private fun CfirBasedSymbol<*>.resolvedInitializationCallableOrNull(
    accessMode: InitializationAccessMode,
): CfirFunction? = when (this) {
    is CfirPropertyAccessorSymbol -> {
        if (!isBound) {
            null
        } else {
            val property = propertySymbol.resolvedInitializationPropertyOrNull()
            if (isGetter) property?.getter else property?.setter
        }
    }

    is CfirPropertySymbol -> resolvedInitializationPropertyOrNull()?.let { property ->
        when (accessMode) {
            InitializationAccessMode.READ -> property.getter
            InitializationAccessMode.WRITE_TARGET -> property.setter
        }
    }

    is CfirFunctionSymbol<*> -> if (isBound) {
        unwrapSubstitutionOverrides()
            .unwrapFakeOverridesOrDelegated()
            .cfir as? CfirFunction
    } else {
        null
    }

    else -> null
}

/** 取得 property 使用点背后的真实声明。 */
private fun CfirPropertySymbol.resolvedInitializationPropertyOrNull(): CfirProperty? =
    if (isBound) {
        unwrapSubstitutionOverrides()
            .unwrapFakeOverridesOrDelegated()
            .cfir
    } else {
        null
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
