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
import org.cangnova.cangjie.cfir.session.cfirProvider
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
import org.cangnova.cangjie.source.CjFakeSourceElementKind
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
     * 当前正在收集 break / continue 状态的循环帧（最内层）。
     *
     * 与 [functionExitCollection] 同构：合法跳转把当前状态交给所属循环——continue
     * 进入条件求值入口，break 进入循环出口（对位 Kotlin FIR CFG 的
     * `addBackEdge(loopBlockExitNode, conditionEnterNode)` / `loopExitNodes`）。
     */
    private var loopJumpFrame: LoopJumpFrame? = null

    /**
     * 单个循环的跳转状态收集帧。
     */
    private class LoopJumpFrame(val loop: CfirLoopExpression) {
        val breakStates = mutableListOf<InitializationState>()
        val continueStates = mutableListOf<InitializationState>()

        /**
         * 是否正在分析该循环自身的条件表达式。
         *
         * 条件里的 break/continue 属于非法循环控制（官方 `CheckInitInLoop` 只处理
         * 循环体内的 jump），不得并入该循环的状态帧。
         */
        var collectingCondition: Boolean = false
    }

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
                        state = collectAndBindTopLevelDestructuring(
                            targets = declaration.variables,
                            initializer = initializer,
                            currentDeclaration = declaration,
                            trackedBySymbol = trackedBySymbol,
                            state = state,
                            recursiveStaticFunctionReads = recursiveStaticFunctionReads,
                        )
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
     * 跨文件 static/global 初始化依赖环检查。
     *
     * 官方 `GlobalVarChecker`（`external/cangjie_compiler/src/Sema/LegalityOfUsage/GlobalVarChecker.cpp`）
     * 把同一包内全部 global/static 存储变量建成 def-use 图：`DoCheck`（:643-651）先做
     * `CheckInSameFile`（:529-561，同文件 `visitOrder` 比较），只有同文件全部合法才进入
     * `CheckCrossFile`（:575-584）——为同一文件内相邻顶层变量补“后声明依赖先声明”的假边，
     * 再由 `CheckByToposort`（:601-641）做三色拓扑排序，遇到回边即报
     * `sema_used_before_initialization`。跨文件允许重排初始化顺序，因此只有真正成环才报，
     * 且整包至多一条（`CheckByToposort` 遇到第一个环立即返回）。
     *
     * 同文件阶段已由 [checkFileStaticGlobalInitialization] 覆盖，这里只补跨文件阶段。节点按官方
     * `CmpNodeByPos`（`include/cangjie/AST/Node.h:2874-2891`）的 `fileID` 语义排序，而官方 `fileID`
     * 来自源文件 basename 升序（`CompileStrategy.cpp:233-246` 显式排序后按序 `AddSource`），
     * 因此这里同样按 basename 升序、完整路径兜底。
     *
     * 诊断归属沿用仓库既有的逐文件模型：包内每个文件都重放同一张图，但只上报引用点位于
     * 当前文件的回边。官方 `ToposortDFS` 对同一张图总是命中同一条回边，因此整包恰好上报一次，
     * 且落在引用真正所在的文件里。该「文件 checker 扫描同包文件」的形态与
     * `CfirImportsChecker.collectPackageImportUsage` 一致。
     */
    fun checkCrossFileStaticGlobalInitializationCycle(file: CfirFile) {
        val packageFiles = file.staticGlobalInitializationPackageFiles()
        if (packageFiles.size < 2) return
        val currentFileIdentity = file.staticGlobalInitializationFileIdentity()

        val nodes = linkedMapOf<CfirBasedSymbol<*>, StaticGlobalDependencyNode>()
        val declarationsByFile = packageFiles.map { packageFile ->
            packageFile.staticGlobalInitializationFileIdentity() to
                packageFile.staticGlobalInitializerDeclarations()
        }
        for ((fileIdentity, declarations) in declarationsByFile) {
            for (declaration in declarations) {
                for (variable in declaration.variables) {
                    nodes.getOrPut(variable.symbol.initializationSymbol()) {
                        StaticGlobalDependencyNode(variable, fileIdentity)
                    }
                }
            }
        }
        if (nodes.isEmpty()) return

        var visitOrder = 0
        for ((fileIdentity, declarations) in declarationsByFile) {
            for (declaration in declarations) {
                when (declaration.kind) {
                    StaticGlobalInitializerKind.VARIABLE -> {
                        declaration.variables.forEach { variable ->
                            nodes[variable.symbol.initializationSymbol()]?.visitOrder = visitOrder++
                        }
                        declaration.initializer?.let { initializer ->
                            declaration.variables.forEach { variable ->
                                val node = nodes[variable.symbol.initializationSymbol()] ?: return@forEach
                                collectStaticGlobalDependencyReads(initializer, node, nodes)
                            }
                        }
                    }

                    StaticGlobalInitializerKind.STATIC_INIT -> {
                        visitOrder = collectStaticInitDependencyReads(
                            declaration = declaration,
                            nodes = nodes,
                            nextVisitOrder = visitOrder,
                        )
                    }
                }
            }
            // 官方 `AddInitializationOrderEdge`：同一文件内相邻顶层变量之间补假边，
            // 保证跨文件拓扑排序仍遵守同文件定义顺序。
            addStaticGlobalInitializationOrderEdges(
                declarations = declarations,
                fileIdentity = fileIdentity,
                nodes = nodes,
            )
        }

        // 官方 `DoCheck`：同文件检查发现顺序问题就不再进入跨文件检查。
        for (node in nodes.values) {
            for (edge in node.usage) {
                if (edge.source == null) continue
                if (edge.node.fileIdentity != node.fileIdentity) continue
                if (edge.node.visitOrder >= node.visitOrder) return
            }
        }

        for (node in nodes.values) {
            if (node.color == StaticGlobalDependencyColor.WHITE) {
                if (!toposortStaticGlobalDependency(node, currentFileIdentity)) return
            }
        }
    }

    /**
     * 收集 `static init` 体中直接初始化当前 class-like static 字段的读取边。
     *
     * 官方 `CollectForStaticInit`（`GlobalVarChecker.cpp:320-342`）在 `field = rhs` 处把当前
     * `DefNode` 切换为字段并只从 rhs 收集依赖；这里保持同一根选择，但把收集范围扩到
     * 直接读取（官方 `CollectVarUsageBFS` 不区分直接读取与函数可达读取）。
     */
    private fun collectStaticInitDependencyReads(
        declaration: StaticGlobalInitializerDeclaration,
        nodes: Map<CfirBasedSymbol<*>, StaticGlobalDependencyNode>,
        nextVisitOrder: Int,
    ): Int {
        val body = declaration.body ?: return nextVisitOrder
        var order = nextVisitOrder
        val assigned = linkedSetOf<CfirBasedSymbol<*>>()

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
                val targetNode = targetSymbol?.let { symbol -> nodes[symbol] }
                if (
                    targetSymbol != null && targetNode != null &&
                    targetNode.variable.field?.status?.isStatic == true &&
                    targetNode.variable.nominalOwnerClassId == declaration.nominalOwnerClassId &&
                    assigned.add(targetSymbol)
                ) {
                    targetNode.visitOrder = order++
                    collectStaticGlobalDependencyReads(assignment.rValue, targetNode, nodes)
                    return
                }

                assignment.rValue.accept(this, null)
                assignment.lValue.accept(this, null)
            }
        }, null)

        return order
    }

    /**
     * 为同一文件内相邻顶层变量补“后声明依赖先声明”的假边。
     *
     * 官方只处理 `file.decls` 中的 `VAR_DECL`；class-like static 字段不参与假边构造。
     */
    private fun addStaticGlobalInitializationOrderEdges(
        declarations: List<StaticGlobalInitializerDeclaration>,
        fileIdentity: String,
        nodes: Map<CfirBasedSymbol<*>, StaticGlobalDependencyNode>,
    ) {
        var previous: StaticGlobalDependencyNode? = null
        for (declaration in declarations) {
            if (declaration.kind != StaticGlobalInitializerKind.VARIABLE) continue
            if (declaration.nominalOwnerClassId != null) continue
            for (variable in declaration.variables) {
                val node = nodes[variable.symbol.initializationSymbol()] ?: continue
                if (node.fileIdentity != fileIdentity) continue
                previous?.let { earlier ->
                    node.usage += StaticGlobalDependencyEdge(
                        node = earlier,
                        source = null,
                        diagnosticName = earlier.variable.diagnosticName,
                        fileIdentity = fileIdentity,
                    )
                }
                previous = node
            }
        }
    }

    /**
     * 官方 `CheckByToposort`/`ToposortDFS` 的三色拓扑排序。
     *
     * @return 拓扑排序是否成功；返回 false 表示检测到环。
     */
    private fun toposortStaticGlobalDependency(
        node: StaticGlobalDependencyNode,
        currentFileIdentity: String,
    ): Boolean {
        if (node.color == StaticGlobalDependencyColor.BLACK) return true
        if (node.color == StaticGlobalDependencyColor.GRAY) return false
        node.color = StaticGlobalDependencyColor.GRAY
        for (edge in node.usage) {
            if (!toposortStaticGlobalDependency(edge.node, currentFileIdentity)) {
                if (edge.node.color == StaticGlobalDependencyColor.GRAY) {
                    reportStaticGlobalInitializationCycle(edge, currentFileIdentity)
                }
                node.color = StaticGlobalDependencyColor.BLACK
                return false
            }
        }
        node.color = StaticGlobalDependencyColor.BLACK
        return true
    }

    /**
     * 上报闭合初始化依赖环的读取边。
     *
     * 假边没有源码位置；引用点不在当前文件时也跳过，保证诊断归属与仓库逐文件模型一致。
     */
    private fun reportStaticGlobalInitializationCycle(
        edge: StaticGlobalDependencyEdge,
        currentFileIdentity: String,
    ) {
        val source = edge.source ?: return
        if (edge.fileIdentity != currentFileIdentity) return
        with(context) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.USED_BEFORE_INITIALIZATION,
                a = edge.diagnosticName,
            )
        }
    }

    /**
     * 收集一个初始化根表达式内对 static/global 存储变量的全部读取。
     *
     * 与 [collectRecursiveStaticFunctionReads] 的区别是包含根表达式自身的直接读取：官方
     * `CollectVarUsageBFS`（`GlobalVarChecker.cpp:395-491`）不区分直接读取与经 callable
     * 可达的读取，两者都进入 def-use 图。
     */
    private fun collectStaticGlobalDependencyReads(
        root: CfirElement,
        userNode: StaticGlobalDependencyNode,
        nodes: Map<CfirBasedSymbol<*>, StaticGlobalDependencyNode>,
    ) {
        val visitedFunctions = linkedSetOf<CfirFunction>()
        lateinit var visitor: org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

        fun collectFromFunction(function: CfirFunction) {
            if (!visitedFunctions.add(function)) return
            val functionBody = function.body ?: return
            functionBody.accept(visitor, null)
        }

        visitor = object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun visitFunction(function: CfirFunction) = Unit

            override fun visitProperty(property: CfirProperty) = Unit

            override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

            override fun visitAssignment(assignment: CfirAssignment) {
                assignment.rValue.accept(this, null)
                visitWriteTarget(assignment.lValue)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                functionCall.resolvedInitializationCallableOrNull(InitializationAccessMode.READ)
                    ?.let(::collectFromFunction)
                val receiver = functionCall.explicitReceiver
                if (receiver is CfirAnonymousFunctionExpression) {
                    collectFromFunction(receiver.anonymousFunction)
                } else {
                    receiver?.accept(this, null)
                }
                functionCall.argumentList.arguments.forEach { argument -> argument.accept(this, null) }
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

            /** 读 property 进入 getter，写目标进入 setter；变量读目标形成 use edge。 */
            private fun visitAccess(
                access: CfirQualifiedAccessExpression,
                accessMode: InitializationAccessMode,
                visitReceiver: Boolean = true,
            ) {
                if (visitReceiver) {
                    access.explicitReceiver?.accept(this, null)
                }
                if (accessMode == InitializationAccessMode.READ) {
                    collectUseEdge(access)
                }
                access.resolvedInitializationCallableOrNull(accessMode)?.let(::collectFromFunction)
            }

            private fun collectUseEdge(access: CfirQualifiedAccessExpression) {
                val symbol = access.resolvedAccessSymbolOrNull()?.initializationSymbol() ?: return
                val usedNode = nodes[symbol] ?: return
                userNode.usage += StaticGlobalDependencyEdge(
                    node = usedNode,
                    source = access.calleeReference.source ?: access.source,
                    diagnosticName = access.calleeReference.referenceNameOrNull() ?: usedNode.variable.diagnosticName,
                    fileIdentity = userNode.fileIdentity,
                )
            }
        }

        root.accept(visitor, null)
    }

    /**
     * 当前文件所属包内参与 static/global 初始化分析的源文件，按官方 fileID 语义排序。
     */
    private fun CfirFile.staticGlobalInitializationPackageFiles(): List<CfirFile> {
        val packageFqName = packageDirective.packageFqName
        return context.session.cfirProvider
            .getCfirFilesByPackage(packageFqName)
            .sortedWith(
                compareBy(
                    { packageFile -> packageFile.sourceFile?.name ?: packageFile.name },
                    { packageFile -> packageFile.sourceFile?.path ?: packageFile.name },
                ),
            )
    }

    /**
     * 源文件身份，用于跨文件比较与「规范序第一个文件」触发判定。
     */
    private fun CfirFile.staticGlobalInitializationFileIdentity(): String =
        sourceFile?.path ?: sourceFile?.name ?: name

    /**
     * 检查 static 字段初始化器。
     *
     * 官方 `CollectToDeclsInfo` 会在遍历类成员时立即检查 static 非函数声明；
     * static 字段按声明顺序初始化，而实例字段在 static 初始化上下文中不可视为已初始化。
     */
    private fun checkClassLikeStaticMemberInitialization(classLike: CfirClassLikeDeclaration) {
        // static 字段初始化按下声明顺序推进；实例字段仍被跟踪，以在初始化器直接读取它们
        // 时报告 USED_BEFORE_INITIALIZATION（对齐 official CheckStaticVarAccessNonStatic + 
        // InitializationChecker 对实例字段未初始化读取的语义）。
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
                // static 字段初始化器不初始化任何实例成员：其体（收紧嵌套 lambda、预留
                // 直接读取）内对实例成员的访问由 STATIC_VARIABLE / STATIC_LAMBDA 检查负责，
                // 只在初始化器直接读取实例字段时报告 USED_BEFORE_INITIALIZATION；嵌套 lambda
                // 内的实例成员访问不产生 illegal / capture 诊断。
                state = analyzeExpression(
                    initializer,
                    state.withStaticFieldInitializerContext(),
                ).withoutStaticFieldInitializerContext()
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
                        source = constructor.constructorNameDiagnosticSource(),
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
            // 官方 `InitializationChecker::NotAssignableVariable` 只看字段「是否已初始化」与
            // 「是否在构造器内」，不区分字段属于当前类还是父类；继承字段必须进入跟踪集合，
            // 否则会被判为 NOT_TRACKED 而误报不可变。
            owner.instanceFieldInfos(context, includeInherited = true)
        } else {
            emptyList()
        }
        val preInitializedFields = if (function is CfirConstructor && function.isInstanceConstructor && owner != null) {
            buildSet<CfirBasedSymbol<*>> {
                owner.instanceFieldsWithInitializer().mapTo(this, CfirFieldVariable::symbol)
                owner.primaryConstructorInitializedPropertiesFor(function).mapTo(this, CfirProperty::symbol)
                // 父类字段一旦由其所属类完成初始化，子类构造器再写入即非法；未被父类初始化的
                // 父类字段则允许子类构造器首次赋值。
                addAll(owner.inheritedPreInitializedFieldSymbols(context, visitedClasses = linkedSetOf()))
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
        var unreachable = false
        for (statement in statements) {
            if (unreachable || currentState.terminated) {
                /*
                 * 中断之后的语句不可达：官方不再做定值检查（不报 UBI、不做重复赋值判定；
                 * cjc 实测 terminated_04_2：中断赋值之后的 `a = 2`、`println(a)` 均零诊断）。
                 * 但赋值合法性检查器读的是本分析登记的分类，漏登记会被当作 NOT_TRACKED
                 * 而误报 CANNOT_ASSIGN_TO_IMMUTABLE，因此这里只补登记 INITIALIZATION
                 * （“官方放弃检查”语义），不推进状态、不上报。
                 */
                unreachable = true
                recordUnreachableAssignmentClassifications(statement)
                continue
            }
            currentState = analyzeStatement(statement, currentState)
        }
        return currentState
    }

    /**
     * 为不可达语句区域内的赋值补登记 `INITIALIZATION` 分类。
     *
     * 诊断模式下分类表为 `null`，本方法整体退化为无操作，因此不会给不可达代码带来任何诊断。
     */
    private fun recordUnreachableAssignmentClassifications(element: CfirElement) {
        if (assignmentClassifications == null) return
        element.forEachExpressionElement { child ->
            if (child is CfirAssignment) {
                recordAssignmentClassification(child, CfirInitializationAssignmentKind.INITIALIZATION)
            }
        }
    }

    /**
     * 递归访问表达式子树内的元素，不进入嵌套函数 / lambda 体（它们由各自的分析独立处理）。
     */
    private fun CfirElement.forEachExpressionElement(action: (CfirElement) -> Unit) {
        action(this)
        acceptChildren(
            object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    element.forEachExpressionElement(action)
                }

                override fun visitFunction(function: CfirFunction) = Unit

                override fun visitAnonymousFunctionExpression(
                    anonymousFunctionExpression: CfirAnonymousFunctionExpression,
                ) = Unit
            },
            null,
        )
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
        is CfirContinueExpression -> analyzeLoopJump(expression, state, isContinue = true)
        is CfirBreakExpression -> analyzeLoopJump(expression, state, isContinue = false)
        is CfirThrowExpression -> analyzeExpression(expression.exception, state).terminate()
        is CfirThisReceiverExpression -> analyzeThisReceiver(expression, state)
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
     * 分析显式 receiver。
     *
     * `this` 是 qualified access 的 receiver 时，非法访问应归属于后续的成员名；
     * 只有作为独立表达式时才由初始化流报告裸 `this` 的非法捕获。这样可以避免
     * `this.member` 同时产生 receiver 与 member 两条相同根因的诊断。
     */
    private fun analyzeReceiver(
        receiver: CfirExpression,
        state: InitializationState,
    ): InitializationState = if (receiver is CfirThisReceiverExpression) {
        state
    } else {
        analyzeExpression(receiver, state)
    }

    /**
     * 构造器中的嵌套函数不能捕获尚未完成初始化的当前实例。
     *
     * 成员初始化器中的裸 `this` 由官方初始化检查保持为普通闭包值，不经过
     * `CheckIllegalMemberAccess`，因此这里只处理实例构造器嵌套函数语境。
     */
    private fun analyzeThisReceiver(
        expression: CfirThisReceiverExpression,
        state: InitializationState,
    ): InitializationState {
        if (!state.isIllegalThisCaptureContext()) return state
        reportIllegalMemberAccess(
            accessKind = NestedInitializerMemberAccessKind.CURRENT_MEMBER,
            diagnosticName = Name.identifier("this"),
            source = expression.calleeReference.source ?: expression.source,
        )
        return state
    }

    /**
     * 分析赋值表达式。
     */
    private fun analyzeAssignment(
        assignment: CfirAssignment,
        state: InitializationState,
    ): InitializationState {
        val diagnosticCountBeforeRightValue = reportedInitializationDiagnosticCount
        /*
         * 复合赋值在 raw CFIR 中解糖为 `lhs = lhs.op(rhs)`，而合成 operator
         * call 与 assignment 共享同一个 lhs 节点。初始化检查的左值路径随后还会
         * 单独处理该节点；如果这里照普通调用先分析 receiver，就会把同一次左值
         * 当成成员读取，额外产生 ILLEGAL_USAGE_OF_MEMBER。官方
         * InitializationChecker 对复合赋值先检查左值，再检查右值，因此合成
         * operator 的 receiver 不能再次进入读取路径，只需保留调用本身和实参分析。
         */
        val afterRightValue = if (state.inMemberInitializer && state.inNestedFunction) {
            assignment.compoundOperatorCallWithSyntheticReceiverOrNull()
                ?.let { operatorCall ->
                    analyzeFunctionCall(
                        expression = operatorCall,
                        state = state,
                        analyzeExplicitReceiver = false,
                    )
                }
                ?: analyzeExpression(assignment.rValue, state)
        } else {
            analyzeExpression(assignment.rValue, state)
        }
        val rightValueHasPriorityDiagnostic =
            reportedInitializationDiagnosticCount != diagnosticCountBeforeRightValue
        /*
         * 官方 `InitializationChecker`：赋值右值若中断（`c = throw Exception()`、
         * `a = Some(1) ?? throw Exception()`），该赋值不会完成，目标不进入定值集合
         * （cjc 实测 terminated_01/04：try-catch 之后 use 报 sema_used_before_initialization）。
         * 但**目标分析本身必须照常执行**：赋值合法性检查器读的是本分析登记的分类，
         * 漏登记会被当作 NOT_TRACKED 而误报 CANNOT_ASSIGN_TO_IMMUTABLE
         * （terminated_02/03/05 实测），因此中断只影响"是否推进定值状态"。
         */
        return analyzeAssignmentTarget(
            assignment = assignment,
            lValue = assignment.lValue,
            state = afterRightValue,
            priorityDiagnostic = rightValueHasPriorityDiagnostic,
            assignmentCompletes = !afterRightValue.terminated,
        )
    }

    /**
     * 取得复合赋值解糖生成的 operator call。
     *
     * 两条 raw-builder 路径通常保留同一 lhs 实例；树转换后也可能只保留相同源码
     * 节点，因此同时使用 identity 与 source 作为结构性判定，不把普通用户调用纳入
     * 该初始化流特殊路径。
     */
    private fun CfirAssignment.compoundOperatorCallWithSyntheticReceiverOrNull(): CfirFunctionCall? {
        if (augmentedOperation == null) return null
        val operatorCall = rValue as? CfirFunctionCall ?: return null
        if (operatorCall.origin != CfirFunctionCallOrigin.Operator) return null
        val receiver = operatorCall.explicitReceiver ?: return null
        if (receiver === lValue) return operatorCall
        if (receiver.source != null && receiver.source == lValue.source) return operatorCall
        return null
    }

    /**
     * 分析赋值左值。
     */
    private fun analyzeAssignmentTarget(
        assignment: CfirAssignment,
        lValue: CfirExpression,
        state: InitializationState,
        priorityDiagnostic: Boolean = false,
        assignmentCompletes: Boolean = true,
    ): InitializationState = when (lValue) {
        is CfirQualifiedAccessExpression -> analyzeAssignmentTargetAccess(
            assignment = assignment,
            access = lValue,
            state = state,
            priorityDiagnostic = priorityDiagnostic,
            assignmentCompletes = assignmentCompletes,
        )

        is CfirTupleLiteral -> lValue.elements.fold(state) { currentState, element ->
            analyzeAssignmentTarget(assignment, element, currentState, priorityDiagnostic, assignmentCompletes)
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
     * 按“赋值是否真正完成”推进定值状态。
     *
     * 右值中断（`c = throw ...`、`a = x ?? throw ...`）时赋值不会完成，目标不得进入定值集合；
     * 分类登记已由调用方完成，因此这里只在完成时标记。
     */
    private fun InitializationState.markAssignedIfCompletes(
        symbol: CfirBasedSymbol<*>,
        assignmentCompletes: Boolean,
    ): InitializationState = if (assignmentCompletes) markInitialized(symbol) else this

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
        assignmentCompletes: Boolean = true,
    ): InitializationState {
        val diagnosticCountBeforeReceiver = reportedInitializationDiagnosticCount
        val afterReceiver = access.explicitReceiver?.let { receiver ->
            analyzeReceiver(receiver, state)
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
                // 写入字段或可写主构造属性时，访问器只是存储槽的解析视图；它不能被当作
                // 普通实例成员函数访问。构造器内的直接写入应先推进 definite-assignment，
                // 嵌套函数中的首次写入则保留 CAPTURE_BEFORE_INITIALIZATION 分类。
                if (access.isTrackedStorageWrite(symbol, afterReceiver)) {
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
                    return afterReceiver.markAssignedIfCompletes(symbol, assignmentCompletes)
                }

                val nestedInitializerAccessKind =
                    afterReceiver.illegalMemberAccessKindFromInitialization(symbol)
                if (nestedInitializerAccessKind != null) {
                    reportIllegalMemberAccess(
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
                afterReceiver.markAssignedIfCompletes(symbol, assignmentCompletes)
            }

            access.shouldSkipIllegalMemberAccessInMemberInitializer(afterReceiver) -> afterReceiver

            else -> reportIllegalMemberAccessIfNeeded(
                access = access,
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
        // 官方 `CheckInitInTryExpr` 先分析 try 块再分析 catch 块：try 块中已全路径初始化的
        // `let` 变量在 catch 块中再次赋值属于重复赋值（CANNOT_ASSIGN_TO_IMMUTABLE），
        // 因此 catch 块的入口状态必须是 try 块分析后的状态，而不是 try 之前的状态。
        // try 体以 throw/中断收尾时结束状态是 terminated，但异常路径本身可达：
        // 若原样进入 catch，catch 体会被当成不可达整体跳过，catch 之后的语句也会被当成
        // 不可达而漏报（cjc 实测 terminated_01：`try { c = throw Exception() } catch {}`
        // 之后 use(c) 报 sema_used_before_initialization）。
        val catchEntryState = tryState.withoutTermination()
        val catchStates = expression.catches.map { catchClause ->
            analyzeScopedBlock(catchClause.body, catchEntryState)
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
        /*
         * 官方 `InitializationChecker::CheckInitInLoop`（Sema/LegalityOfUsage/
         * InitializationChecker.cpp:1360）把循环体内的 break/continue 视为控制流出口：
         * continue 到达条件求值入口，break 到达循环出口。没有这层帧时，
         * `do { if (true) { continue }; b = 1 } while (b == 0)` 的条件与循环后会
         * 错误地认为 `b` 已初始化。
         */
        val frame = LoopJumpFrame(expression)
        val previousFrame = loopJumpFrame
        loopJumpFrame = frame
        try {
            return if (expression.isDoWhile) {
                val repeatableBodyState = analyzeScopedBlock(
                    expression.body,
                    state.enterRepeatableRegion(),
                ).restoreRepeatableDepth(state.repeatableDepth)
                val conditionEntryState = frame.continueStates
                    .fold(repeatableBodyState) { accumulated, jumpState ->
                        mergeBranchStates(accumulated, jumpState.restoreRepeatableDepth(state.repeatableDepth))
                    }
                    .withoutTermination()
                frame.collectingCondition = true
                val afterCondition = try {
                    analyzeExpression(expression.condition, conditionEntryState)
                } finally {
                    frame.collectingCondition = false
                }
                val loopExitState = frame.breakStates
                    .fold(afterCondition) { accumulated, jumpState ->
                        mergeBranchStates(accumulated, jumpState.restoreRepeatableDepth(state.repeatableDepth))
                    }
                // 体全部终止且没有 continue 路径时，条件不可达；否则条件至少经 continue 可达。
                if (repeatableBodyState.terminated && frame.continueStates.isEmpty()) {
                    loopExitState.terminate()
                } else {
                    loopExitState
                }
            } else {
                val afterCondition = analyzeExpression(expression.condition, state)
                val bodyState = analyzeScopedBlock(
                    expression.body,
                    afterCondition.enterRepeatableRegion(),
                ).restoreRepeatableDepth(afterCondition.repeatableDepth)
                val conditionReentryState = frame.continueStates
                    .fold(bodyState) { accumulated, jumpState ->
                        mergeBranchStates(accumulated, jumpState.restoreRepeatableDepth(afterCondition.repeatableDepth))
                    }
                val mergedExitState = mergeBranchStates(afterCondition, conditionReentryState)
                frame.breakStates
                    .fold(mergedExitState) { accumulated, jumpState ->
                        mergeBranchStates(accumulated, jumpState.restoreRepeatableDepth(afterCondition.repeatableDepth))
                    }
            }
        } finally {
            loopJumpFrame = previousFrame
        }
    }

    /**
     * 分析循环跳转语句。
     *
     * 只有**合法**跳转才参与定值流：目标是当前最内层循环、不在条件表达式中、
     * 也不在嵌套函数/lambda 体内（后两者分别由 `collectingCondition` 与
     * `inNestedFunction` 拦截——lambda 体内的跳转延迟执行，条件里的跳转属非法
     * 循环控制）。非法跳转由其专属检查器上报，此处保持旧实现语义：不终止顺序流。
     */
    private fun analyzeLoopJump(
        jump: CfirLoopJump,
        state: InitializationState,
        isContinue: Boolean,
    ): InitializationState {
        val frame = loopJumpFrame
        // 非法跳转（循环外 break/continue）在 resolve 阶段不绑定 target，
        // 读取 labeledElement 会抛 UninitializedPropertyAccessException。
        val targetLoop = jump.target.takeIf { it.isBound }?.labeledElement
        val isValidJump = frame != null &&
                targetLoop === frame.loop &&
                !frame.collectingCondition &&
                !state.inNestedFunction
        if (isValidJump) {
            if (isContinue) frame.continueStates += state else frame.breakStates += state
            return state.terminate()
        }
        return state
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
        analyzeExplicitReceiver: Boolean = true,
    ): InitializationState {
        val diagnosticCountBeforeReceiver = reportedInitializationDiagnosticCount
        var currentState = if (analyzeExplicitReceiver) expression.explicitReceiver?.let { receiver ->
            analyzeReceiver(receiver, state)
        } ?: state else state
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
                    access = expression,
                    symbol = callableSymbol,
                    diagnosticName = diagnosticName,
                    source = expression.calleeReference.source ?: expression.source,
                    state = currentState,
                )
            }
        }

        if (expression.argumentList.arguments.any { it is CfirNamedArgumentExpression }) {
            /*
             * 官方对含命名实参的调用放弃定值分析：实参求值顺序未规定，读与写都不产生初始化诊断
             * （cjc 实测：`fseq(b: println(y), a: (y = 10))` 中未初始化的 `let y` 零诊断，
             * 而位置实参 `fs2(println(y), (y = 10))` 照常报 y 的 UBI）。
             * 但放弃分析不等于放弃登记：实参内赋值的分类必须按调用点状态写入，
             * 否则合法性检查器会因 NOT_TRACKED 误报 CANNOT_ASSIGN_TO_IMMUTABLE。
             */
            expression.argumentList.arguments.forEach { argument ->
                argument.forEachExpressionElement { child ->
                    if (child is CfirAssignment) {
                        val target = child.lValue as? CfirQualifiedAccessExpression
                        val symbol = target?.resolvedAccessSymbolOrNull()?.initializationSymbol()
                        if (symbol != null) {
                            recordAssignmentClassification(
                                child,
                                if (currentState.isInitialized(symbol)) {
                                    CfirInitializationAssignmentKind.REASSIGNMENT
                                } else {
                                    CfirInitializationAssignmentKind.INITIALIZATION
                                },
                            )
                        }
                    }
                }
            }
            if (expression.origin == CfirFunctionCallOrigin.ConstructorDelegationThis) {
                currentState = currentState.markAllInstanceFieldsInitialized()
            }
            return currentState
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
            analyzeReceiver(receiver, state)
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
                access = expression,
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
            analyzeReceiver(receiver, state)
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
                access = expression,
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
     * 判断赋值访问是否对应可参与初始化流的实际存储槽。
     *
     * 主构造参数属性在解析后可能以 property、setter 或字段变量符号出现；三者都表示
     * 写入同一个存储，但只有带 setter 的属性访问才可以作为 property 写目标处理。
     */
    private fun CfirQualifiedAccessExpression.isTrackedStorageWrite(
        symbol: CfirBasedSymbol<*>,
        state: InitializationState,
    ): Boolean = when (symbol) {
        is CfirPropertyAccessorSymbol -> symbol.isSetter
        is CfirPropertySymbol -> symbol.cfir.setter != null ||
                (
                    symbol.cfir.isPrimaryConstructorParameterProperty() &&
                            state.expectedReceiver != null
                    )
        is CfirVariableSymbol<*> -> true
        else -> false
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
        val nestedInitializerAccessKind = state.illegalMemberAccessKindFromInitialization(symbol)
        if (nestedInitializerAccessKind != null) {
            reportIllegalMemberAccess(
                accessKind = nestedInitializerAccessKind,
                diagnosticName = diagnosticName,
                source = source,
            )
            return state
        }

        // static 字段初始化器中的嵌套 lambda 对实例字段的访问属于延迟执行，缺少"正在初始化的
        // 实例"语义；由 STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC 检查负责，不报告
        // USED/CAPTURE_BEFORE_INITIALIZATION（直接读取仍按未初始化实例字段报告 USED）。
        if (
            state.inStaticFieldInitializer &&
            state.inNestedFunction &&
            state.trackedVariable(symbol)?.kind == TrackedVariableKind.INSTANCE_MEMBER
        ) {
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
     * 报告初始化上下文中当前实例或继承实例成员的非法访问。
     *
     * 该语义与普通未初始化读取不同：对象仍处于成员初始化阶段，闭包可能在完整对象
     * 构造前逃逸，因此必须保留官方独立诊断，不能降级为 USED_BEFORE_INITIALIZATION。
     */
    private fun reportIllegalMemberAccess(
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

    /** 报告 struct 构造器嵌套 callable 对当前实例成员函数/属性的捕获。 */
    private fun reportIllegalThisCapture(
        source: org.cangnova.cangjie.source.CjSourceElement?,
    ) {
        reportedInitializationDiagnosticCount++
        if (!reportReadDiagnostics) return

        with(context) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.ILLEGAL_CAPTURE_THIS,
                a = "struct",
            )
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
                        source = staticField.source,
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
            if (reportGlobalReferenceBeforeItsDefinition(edge.source, edge.usedVariable)) continue
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
     * 上报同一文件内早于声明点的顶层存储变量引用。
     *
     * 官方 `InitializationChecker::CheckInitInRefExpr`（`InitializationChecker.cpp:732-736`）与
     * `CheckInitInMemberAccess`（:783-788）规定：目标带 `Attribute::GLOBAL`（包级存储变量）、
     * 引用与目标在同一文件且引用位置早于目标声明位置时，报 `sema_undefined_variable`
     * （"used before being defined"），而不是初始化顺序诊断。名字查找本身照常绑定该目标
     * （`LookUpImpl.cpp:479-482` 把 `scopeLevel == 0` 的顶层声明排除在顺序过滤之外），
     * 因此该分支属于初始化检查器职责，与 `UNRESOLVED_REFERENCE` 的解析失败语义不同。
     *
     * class-like static 成员不走该分支：官方对它们的同类前向引用由 `GlobalVarChecker` 的
     * 同文件 `visitOrder` 规则报 `sema_global_var_used_before_initialization`。
     * 调用点都位于单文件初始化顺序分析内，同文件条件天然成立。
     *
     * @return 是否已经上报前向引用诊断；为 true 时调用方不得再报初始化顺序诊断。
     */
    private fun reportGlobalReferenceBeforeItsDefinition(
        referenceSource: org.cangnova.cangjie.source.CjSourceElement?,
        targetVariable: StaticGlobalInitializerVariable,
    ): Boolean {
        if (targetVariable.nominalOwnerClassId != null) return false
        // 没有源码位置的声明无法比较声明点，不能仅凭偏移量判定为前向引用。
        if (targetVariable.sourceOffset == UNKNOWN_SOURCE_OFFSET) return false
        val referenceOffset = referenceSource?.startOffset ?: return false
        if (referenceOffset >= targetVariable.sourceOffset) return false

        with(context) {
            reporter.reportOn(
                source = referenceSource,
                factory = CfirErrors.UNRESOLVED_REFERENCE,
                a = targetVariable.diagnosticName.asString(),
                b = null,
            )
        }
        return true
    }

    /**
     * 以访问表达式的引用名为位置上报顶层存储变量的前向引用。
     */
    private fun reportGlobalReferenceBeforeItsDefinition(
        access: CfirQualifiedAccessExpression,
        targetVariable: StaticGlobalInitializerVariable,
    ): Boolean = reportGlobalReferenceBeforeItsDefinition(
        referenceSource = access.calleeReference.source ?: access.source,
        targetVariable = targetVariable,
    )

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
     * 官方 `IsVarUsedBeforeDefinition` 对这种普通作用域前向引用报错，且具体诊断由
     * `InitializationChecker::CheckInitInRefExpr`（`InitializationChecker.cpp:732-736`）给出：
     * 目标带 `GLOBAL`、同文件、引用点早于声明点时是 `sema_undefined_variable`
     * （项目名 [CfirErrors.UNRESOLVED_REFERENCE]）。访问另一个 class-like 的 static 成员
     * 由 global/static 初始化图处理，不能按实例字段的源码偏移误判。
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
                if (reportGlobalReferenceBeforeItsDefinition(access, targetVariable)) return

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
    /**
     * 按官方语义检查并绑定一个顶层解构声明。
     *
     * 官方把元组解构按**分量下钻**增量绑定：元组目标与元组右值逐层配对，先求值第 i 个
     * 分量、绑定第 i 个目标，再求值第 i+1 个分量（cjc 实测：`let (a, (b, c)) = (f(), (g(), h()))`
     * 只报 f 中的 a；h 求值时 b 已绑定）。右值不是与目标同构的元组字面量时（含 `_` 丢弃位
     * 导致目标数与分量数不等），官方脱糖为先 `var $tmp = rhs` 再逐目标下标绑定，
     * 因此整个右值先求值、随后一次性绑定全部目标——读边按当前绑定状态判定。
     */
    private fun collectAndBindTopLevelDestructuring(
        targets: List<StaticGlobalInitializerVariable>,
        initializer: CfirExpression,
        currentDeclaration: StaticGlobalInitializerDeclaration,
        trackedBySymbol: Map<CfirBasedSymbol<*>, StaticGlobalInitializerVariable>,
        state: InitializationState,
        recursiveStaticFunctionReads: MutableList<StaticGlobalUseEdge>,
    ): InitializationState {
        val tupleElements = (initializer as? CfirTupleLiteral)?.elements
        if (tupleElements == null || tupleElements.size != targets.size || targets.size == 1) {
            val ownerVisitOrder = targets.minOfOrNull { variable -> variable.visitOrder }
                ?: currentDeclaration.visitOrder
            reportStaticGlobalReadsBeforeInitialization(
                currentDeclaration = currentDeclaration,
                initializer = initializer,
                trackedBySymbol = trackedBySymbol,
                initialized = state.initialized,
            )
            collectRecursiveStaticFunctionReads(
                root = initializer,
                ownerVisitOrder = ownerVisitOrder,
                trackedBySymbol = trackedBySymbol,
                destination = recursiveStaticFunctionReads,
            )
            var bound = state
            for (variable in targets) {
                variable.initialized = true
                bound = bound.markInitialized(variable.symbol)
            }
            return bound
        }

        var bound = state
        targets.forEachIndexed { index, variable ->
            bound = collectAndBindTopLevelDestructuring(
                targets = listOf(variable),
                initializer = tupleElements[index],
                currentDeclaration = currentDeclaration,
                trackedBySymbol = trackedBySymbol,
                state = bound,
                recursiveStaticFunctionReads = recursiveStaticFunctionReads,
            )
        }
        return bound
    }

    private fun reportStaticGlobalReadsBeforeInitialization(
        currentDeclaration: StaticGlobalInitializerDeclaration,
        initializer: CfirExpression,
        trackedBySymbol: Map<CfirBasedSymbol<*>, StaticGlobalInitializerVariable>,
        initialized: Set<CfirBasedSymbol<*>>,
    ) {        initializer.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
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
                if (reportGlobalReferenceBeforeItsDefinition(access, targetVariable)) return

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
     * 对齐官方 `CheckIllegalMemberAccess` 的实例成员访问规则。
     *
     * 成员初始化器与构造器嵌套 callable 的分类先由统一初始化上下文计算；普通构造器
     * 成员函数访问只在实例字段尚未全部初始化时报告。其余访问继续走变量初始化流。
     */
    private fun reportIllegalMemberAccessIfNeeded(
        access: CfirQualifiedAccessExpression,
        symbol: CfirBasedSymbol<*>,
        diagnosticName: Name,
        source: org.cangnova.cangjie.source.CjSourceElement?,
        state: InitializationState,
    ): InitializationState {
        if (!access.usesCurrentInstanceReceiver()) return state

        if (state.isStructConstructorCaptureContext() && symbol.isInstanceMemberFunctionOrProperty()) {
            reportIllegalThisCapture(source)
            return state
        }

        val illegalMemberAccessKind = state.illegalMemberAccessKindFromInitialization(symbol)
        if (illegalMemberAccessKind != null) {
            reportIllegalMemberAccess(
                accessKind = illegalMemberAccessKind,
                diagnosticName = diagnosticName,
                source = source,
            )
            return state
        }

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

    /**
     * 判断成员访问是否真正作用于当前正在初始化的实例。
     *
     * 初始化流中的未完成字段集合属于当前 `this`，不能传播到显式 receiver 的成员访问。
     * `DateTime.now().toUnixTimeStamp()` 等调用的 receiver 是另一个已构造对象；只有隐式
     * receiver、显式 `this` 或 `super` 才需要继续执行当前对象的成员初始化约束。
     */
    private fun CfirQualifiedAccessExpression.usesCurrentInstanceReceiver(): Boolean {
        val receiver = dispatchReceiver ?: explicitReceiver ?: return true
        return receiver.unwrapSmartcastExpression() is CfirThisReceiverExpression ||
                receiver.unwrapSmartcastExpression() is CfirSuperReceiverExpression
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
     * 当前是否处于 static 字段初始化器求值上下文。
     *
     * static 初始化上下文不初始化任何实例成员，其体内（尤其嵌套 lambda）对实例成员的
     * 访问由 `STATIC_VARIABLE/STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC` 检查负责，不产生
     * 实例成员未初始化 / 捕获语义，因此需要单独标记以免复用实例成员初始化检查。
     */
    val inStaticFieldInitializer: Boolean,

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
            inStaticFieldInitializer = false,
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
     * 计算初始化上下文中的成员访问诊断分类。
     *
     * 成员初始化器和实例构造器是两个不同的初始化阶段：成员初始化器中的直接成员
     * 函数访问本身就非法，嵌套 callable 对字段的访问则表示闭包捕获；构造器中只有
     * 在所有实例字段完成初始化前，成员函数访问以及嵌套 callable 的成员访问才非法。
     * 继承字段保留 `super-member` 分类，而函数/属性统一由官方规则归入 `member`。
     */
    fun illegalMemberAccessKindFromInitialization(
        symbol: CfirBasedSymbol<*>,
    ): NestedInitializerMemberAccessKind? {
        val normalizedSymbol = symbol.initializationSymbol()
        val variableInfo = tracked[normalizedSymbol]
        val isInstanceField = variableInfo?.kind == TrackedVariableKind.INSTANCE_MEMBER
        val isMemberFunctionOrProperty = symbol.isInstanceMemberFunctionOrProperty()
        if (!isInstanceField && !isMemberFunctionOrProperty) return null

        val currentOwnerClassIdMaybe = memberInitializerOwner?.symbol?.classId
            ?: (expectedReceiver as? CfirClassLikeSymbol<*>)?.classId
        val targetOwnerClassIdMaybe = normalizedSymbol.initializationMemberOwnerClassId()
        val isInheritedFieldInMemberInitializer =
            inMemberInitializer && !inNestedFunction && isInstanceField &&
                    currentOwnerClassIdMaybe != null && targetOwnerClassIdMaybe != null &&
                    targetOwnerClassIdMaybe != currentOwnerClassIdMaybe
        val hasUninitializedFields = hasUninitializedInstanceFields()
        val shouldReport = when {
            isInheritedFieldInMemberInitializer -> true
            inMemberInitializer && isMemberFunctionOrProperty -> true
            inMemberInitializer && inNestedFunction && isInstanceField -> hasUninitializedFields
            expectedReceiver != null && inNestedFunction -> hasUninitializedFields
            expectedReceiver != null && !inNestedFunction && isMemberFunctionOrProperty -> hasUninitializedFields
            else -> false
        }
        if (!shouldReport) return null

        // 函数/属性无论声明在当前类还是父类，都使用官方的 member 诊断；只有字段
        // 的声明 owner 决定是否属于 super-member。
        if (isMemberFunctionOrProperty) return NestedInitializerMemberAccessKind.CURRENT_MEMBER

        val currentOwnerClassId = currentOwnerClassIdMaybe ?: return null
        val targetOwnerClassId = targetOwnerClassIdMaybe ?: return null
        return if (targetOwnerClassId == currentOwnerClassId) {
            NestedInitializerMemberAccessKind.CURRENT_MEMBER
        } else {
            NestedInitializerMemberAccessKind.SUPER_MEMBER
        }
    }

    /** 判断裸 `this` 是否位于尚未完成初始化的构造器嵌套函数中。 */
    fun isIllegalThisCaptureContext(): Boolean =
        expectedReceiver != null && inNestedFunction &&
                (hasUninitializedInstanceFields() || expectedReceiver.cfir is CfirStruct)

    /** 判断当前是否为 struct 构造器中的嵌套 callable。该规则与字段初始化状态无关。 */
    fun isStructConstructorCaptureContext(): Boolean =
        expectedReceiver?.cfir is CfirStruct && inNestedFunction

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
     * 进入 static 字段初始化器求值上下文。
     */
    fun withStaticFieldInitializerContext(): InitializationState =
        if (inStaticFieldInitializer) this else copy(inStaticFieldInitializer = true)

    /**
     * 离开 static 字段初始化器求值上下文。
     */
    fun withoutStaticFieldInitializerContext(): InitializationState =
        if (!inStaticFieldInitializer) this else copy(inStaticFieldInitializer = false)

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
            inStaticFieldInitializer = inStaticFieldInitializer && other.inStaticFieldInitializer,
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
    override val requiresImplementation: Boolean get() = true

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

    override val requiresImplementation: Boolean get() = true

    /**
     * 检查同一文件中的顶层变量和 static 成员字段初始化顺序。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val analyzer = CfirInitializationFlowAnalyzer(context, reporter)
        analyzer.checkFileStaticGlobalInitialization(declaration)
        analyzer.checkCrossFileStaticGlobalInitializationCycle(declaration)
    }
}

/**
 * class-like 成员初始化器声明顺序检查器。
 */
object CfirClassLikeInitializationChecker : CfirClassLikeChecker() {
    override val requiresImplementation: Boolean get() = true

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
    override val requiresImplementation: Boolean get() = true

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
 * 跨文件初始化依赖图的节点。
 *
 * 对位官方 `DefNode`（`GlobalVarChecker.cpp:142-152`）：`usage` 是当前变量初始化期间
 * 可能读取到的其他 static/global 变量，`visitOrder` 反映声明/初始化点顺序。
 */
private class StaticGlobalDependencyNode(
    /**
     * 该节点对应的 static/global 存储变量。
     */
    val variable: StaticGlobalInitializerVariable,
    /**
     * 变量所在源文件身份，用于复刻官方 `IsInSameFile`。
     */
    val fileIdentity: String,
) {
    /**
     * 当前变量初始化期间读取到的其他 static/global 存储变量。
     */
    val usage = mutableListOf<StaticGlobalDependencyEdge>()

    /**
     * 官方 `DefNode::visitOrder`。
     */
    var visitOrder: Int = -1

    /**
     * 官方三色标记。
     */
    var color: StaticGlobalDependencyColor = StaticGlobalDependencyColor.WHITE
}

/**
 * 跨文件初始化依赖图的读取边。
 *
 * 对位官方 `UseEdge`（`GlobalVarChecker.cpp:108-119`）：假边没有 `refNode`，因此
 * [source] 为空，不参与诊断上报。
 */
private class StaticGlobalDependencyEdge(
    /**
     * 被读取的变量节点。
     */
    val node: StaticGlobalDependencyNode,
    /**
     * 触发读取的源码位置；假边为空。
     */
    val source: org.cangnova.cangjie.source.CjSourceElement?,
    /**
     * 报告诊断时展示的被读取变量名称。
     */
    val diagnosticName: Name,
    /**
     * 触发读取的初始化所在源文件身份；诊断只归属该文件。
     */
    val fileIdentity: String,
)

/**
 * 官方 `GlobalVarChecker` 三色标记（`GlobalVarChecker.cpp:130-134`）。
 */
private enum class StaticGlobalDependencyColor {
    WHITE,
    BLACK,
    GRAY,
}

/**
 * 缺失源码位置时使用的哨兵偏移量。
 *
 * 该值大于任何真实偏移量，因此不能直接参与“引用点是否早于声明点”的比较。
 */
private const val UNKNOWN_SOURCE_OFFSET: Int = Int.MAX_VALUE

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
                sourceOffset = source?.startOffset ?: UNKNOWN_SOURCE_OFFSET,
            )
        ),
        initializer = initializer,
        body = null,
        sourceOffset = source?.startOffset ?: UNKNOWN_SOURCE_OFFSET,
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
            sourceOffset = bindingVariable.source?.startOffset ?: source?.startOffset ?: UNKNOWN_SOURCE_OFFSET,
        )
    }

    return StaticGlobalInitializerDeclaration(
        variables = variables,
        initializer = initializer,
        body = null,
        sourceOffset = source?.startOffset ?: UNKNOWN_SOURCE_OFFSET,
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
        sourceOffset = source?.startOffset ?: UNKNOWN_SOURCE_OFFSET,
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
 * 收集可见父类中已由其所属类完成初始化的实例字段与主构造成员属性。
 *
 * 官方 `InitializationChecker::NotAssignableVariable` 的判定只看字段是否已初始化，
 * 不区分字段归属；因此父类中带初始化器的字段、以及父类主构造器参数对应的成员属性，
 * 在子类构造器进入时已处于已初始化状态，再次写入应报不可变。反之，父类中没有初始化器
 * 且未被主构造器初始化的字段在子类构造器内允许首次赋值。
 *
 * 遍历方式与 [inheritedInstanceFieldInfos] 保持一致，同样跳过 private 父类字段并防环。
 */
private fun CfirClassLikeDeclaration.inheritedPreInitializedFieldSymbols(
    context: CheckerContext,
    visitedClasses: MutableSet<ClassId>,
): List<CfirBasedSymbol<*>> = buildList {
    for (superTypeRef in superTypeRefs) {
        val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
        val superClassId = superType.classId
        if (!visitedClasses.add(superClassId)) continue

        val superClass = context.session.symbolProvider
            .getClassLikeSymbolByClassId(superClassId)
            ?.cfir as? CfirClass ?: continue

        // 与 `reportFieldsLeftUninitializedByDefaultConstructor` 采用同一判据：父类只要存在
        // 带函数体的实例构造器，就由它自己负责初始化全部实例字段（此时父类不会报
        // CLASS_UNINITIALIZED_FIELD）。子类构造器进入时这些字段已初始化，读取合法、再次写入非法。
        // 反之，父类既无初始化器又无构造器体的字段确实处于未初始化状态，允许子类构造器首次赋值。
        val superInitializesOwnFields = superClass.declarations
            .filterIsInstance<CfirConstructor>()
            .any { constructor -> constructor.isInstanceConstructor && constructor.body != null }
        if (superInitializesOwnFields) {
            superClass.declarations
                .filterIsInstance<CfirFieldVariable>()
                .filter { field -> !field.status.isStatic && field.status.visibility != Visibilities.Private }
                .mapTo(this, CfirFieldVariable::symbol)
        }
        superClass.instanceFieldsWithInitializer()
            .filter { field -> field.status.visibility != Visibilities.Private }
            .mapTo(this, CfirFieldVariable::symbol)
        superClass.primaryConstructorPropertyInfos()
            .filter { propertyInfo ->
                (propertyInfo.symbol as? CfirPropertySymbol)?.cfir?.status?.visibility != Visibilities.Private
            }
            .mapTo(this, TrackedVariableInfo::symbol)
        addAll(superClass.inheritedPreInitializedFieldSymbols(context, visitedClasses))
    }
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
 * 判断属性是否由主构造器的成员参数生成。
 *
 * 这类属性虽然对外仍是 `let`，但其 backing storage 的首次写入发生在构造器初始化流中；
 * 不能把它与普通只读属性混为一谈。唯一身份来自 raw builder 写入的 fake source kind，
 * 不使用属性名称或源码文本猜测。
 */
internal fun CfirProperty.isPrimaryConstructorParameterProperty(): Boolean =
    source?.kind == CjFakeSourceElementKind.PropertyFromParameter

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
