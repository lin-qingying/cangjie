package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid

/**
 * 对齐 Kotlin `FirUnusedExpressionChecker/FirUnusedCheckerBase` 的声明级 visitor 入口。
 *
 * 当前 analysis-tests 只注册 `CommonDeclarationCheckers`，因此这里直接挂在 common
 * declaration checker 流里，让 `try/finally` 中“结果被丢弃”的纯表达式走统一的 unused
 * 诊断路径，而不是在具体 expression checker 里做特判。
 */
object CfirUnusedExpressionChecker : CfirBasicDeclarationChecker() {
    /**
     * 从声明入口驱动 unused expression 和 unused local variable 检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val visitor = UsageVisitor(context, reporter, declaration)
        when (declaration) {
            is CfirCodeFragment -> declaration.block.accept(visitor, UsageState.Used)
            is CfirAnonymousFunction -> Unit
            is CfirFunction -> {
                reportUnusedLocalVariables(declaration)
                declaration.body?.accept(visitor, declaration.bodyUsageState())
            }
            is CfirVariable -> declaration.initializer?.accept(visitor, UsageState.Used)
            else -> Unit
        }
    }

    /**
     * 根据函数返回类型决定函数体最后一个表达式的使用状态。
     */
    private fun CfirFunction.bodyUsageState(): UsageState {
        val returnType = returnTypeRef.coneTypeOrNull
        return if (returnType == ConePrimitiveType.UNIT) UsageState.UnusedUnitReturn else UsageState.Used
    }

    /**
     * 表达式结果使用状态。
     */
    private enum class UsageState {
        /**
         * 表达式结果被使用。
         */
        Used,

        /**
         * 表达式结果被丢弃。
         */
        Unused,

        /**
         * Unit 返回位置的表达式结果被丢弃。
         */
        UnusedUnitReturn,

        /**
         * 表达式位于当前 CFIR 可确认没有执行前驱的分支中。
         */
        Unreachable,
        ;

        /**
         * 判断当前状态是否表示结果未被使用。
         */
        fun isUnused(): Boolean = this == Unused || this == UnusedUnitReturn
    }

    /**
     * 报告函数体内未使用的局部 pattern binding 变量。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnusedLocalVariables(function: CfirFunction) {
        val body = function.body ?: return
        val rootFunction = function
        val declaredVariables = linkedMapOf<CfirVariableSymbol<*>, CfirPatternBindingVariable>()
        val usedVariables = linkedSetOf<CfirVariableSymbol<*>>()

        body.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunction(function: CfirFunction) {
                if (function === rootFunction) {
                    function.acceptChildren(this)
                }
            }

            override fun visitPatternVariable(patternVariable: CfirPatternVariable) {
                val skipUnusedVariableReport = patternVariable.initializer?.isDceSkippedThisInitializer() == true
                for (bindingVariable in patternVariable.pattern.bindingVariables()) {
                    if (!bindingVariable.isLocal) continue
                    if (bindingVariable.name.asString().startsWith("_")) continue
                    if (skipUnusedVariableReport) continue
                    declaredVariables[bindingVariable.symbol] = bindingVariable
                }
                patternVariable.initializer?.accept(this)
            }

            override fun visitPatternBindingVariable(patternBindingVariable: CfirPatternBindingVariable) {
                // Binding initializer facts are mirrored from the outer pattern variable.
                // The outer declaration owns traversal to avoid counting the same initializer twice.
            }

            override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                qualifiedAccessExpression.resolvedVariableSymbolOrNull()?.let { usedVariables += it }
                qualifiedAccessExpression.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                functionCall.resolvedVariableSymbolOrNull()?.let { usedVariables += it }
                functionCall.acceptChildren(this)
            }

            override fun visitTryExpression(tryExpression: CfirTryExpression) {
                tryExpression.resources.forEach { it.accept(this) }
                tryExpression.tryBlock.accept(this)
                if (tryExpression.tryBlock.mayThrowException()) {
                    tryExpression.catches.forEach { it.accept(this) }
                }
                tryExpression.handlers.forEach { it.accept(this) }
                tryExpression.finallyBlock?.accept(this)
            }
        })

        for ((symbol, variable) in declaredVariables) {
            if (symbol in usedVariables) continue
            reporter.reportOn(variable.source, CfirErrors.UNUSED_VARIABLE)
        }
    }

    /**
     * unused expression 递归 visitor。
     *
     * @property context 当前检查上下文。
     * @property reporter 诊断报告器。
     * @property declaration 当前被检查的声明，用于区分 finalizer 等特殊语义。
     */
    private class UsageVisitor(
        /**
         * 当前检查上下文。
         */
        private val context: CheckerContext,

        /**
         * 诊断报告器。
         */
        private val reporter: DiagnosticReporter,

        /**
         * 当前被检查的声明。
         */
        private val declaration: CfirDeclaration,
    ) : CfirDefaultVisitor<Unit, UsageState>() {
        /**
         * 跳过嵌套声明，嵌套声明会由声明检查流程单独处理。
         */
        override fun visitDeclaration(declaration: CfirDeclaration, data: UsageState) {
            // 嵌套声明由诊断收集器单独驱动对应的 declaration checker，不在这里重复扫描。
        }

        /**
         * 默认元素访问：先检查当前表达式，再按 used 状态访问子节点。
         */
        override fun visitElement(element: CfirElement, data: UsageState) {
            if (element is CfirExpression && element.source != null) {
                checkExpression(element, data)
            }
            val childUsage = if (data == UsageState.Unreachable) UsageState.Unreachable else UsageState.Used
            element.acceptChildren(this, childUsage)
        }

        /**
         * 检查匿名函数表达式自身，并按 lambda/非 lambda 决定 body 的结果使用状态。
         */
        override fun visitAnonymousFunctionExpression(
            anonymousFunctionExpression: CfirAnonymousFunctionExpression,
            data: UsageState,
        ) {
            checkExpression(anonymousFunctionExpression, data)
            val bodyUsage = if (anonymousFunctionExpression.anonymousFunction.isLambda) {
                UsageState.Used
            } else {
                UsageState.Unused
            }
            anonymousFunctionExpression.anonymousFunction.body?.accept(this, bodyUsage)
        }

        /**
         * return 表达式的结果始终按 used 访问。
         */
        override fun visitReturnExpression(returnExpression: CfirReturnExpression, data: UsageState) {
            returnExpression.result.accept(this, UsageState.Used)
        }

        /**
         * if 表达式按当前结果使用状态传播到 then/else 分支。
         */
        override fun visitIfExpression(ifExpression: CfirIfExpression, data: UsageState) {
            checkExpression(ifExpression, data)
            ifExpression.condition.accept(this, UsageState.Used)
            ifExpression.thenBranch.accept(this, data)
            ifExpression.elseBranch?.accept(this, data)
        }

        /**
         * match 表达式按当前结果使用状态传播到各分支 body。
         */
        override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: UsageState) {
            checkExpression(matchExpression, data)
            matchExpression.subject?.accept(this, UsageState.Used)
            matchExpression.branches.forEach { branch ->
                branch.guard?.accept(this, UsageState.Used)
                if (!branch.body.isPureUnitBranchResult()) {
                    branch.body.accept(this, data.branchResultUsage(branch.body))
                }
            }
        }

        /**
         * try 表达式按当前结果使用状态传播到 try/catch，finally 始终按 unused 访问。
         */
        override fun visitTryExpression(tryExpression: CfirTryExpression, data: UsageState) {
            checkExpression(tryExpression, data)
            tryExpression.tryBlock.accept(this, data)
            val catchUsage = if (tryExpression.tryBlock.mayThrowException()) data else UsageState.Unreachable
            tryExpression.catches.forEach { catchClause ->
                catchClause.body.accept(this, catchUsage)
            }
            tryExpression.finallyBlock?.accept(this, UsageState.Unused)
        }

        /**
         * loop 条件按 used 访问，循环体结果按 unused 访问。
         */
        override fun visitLoopExpression(loopExpression: CfirLoopExpression, data: UsageState) {
            checkExpression(loopExpression, data)
            loopExpression.condition.accept(this, UsageState.Used)
            loopExpression.body.accept(this, UsageState.Unused)
        }

        /**
         * block 中只有最后一条语句继承外部使用状态，其余语句按 unused 访问。
         */
        override fun visitBlock(block: CfirBlock, data: UsageState) {
            checkExpression(block, data)
            val lastIndex = block.statements.lastIndex
            for (index in block.statements.indices) {
                val usage = if (index == lastIndex) data else UsageState.Unused
                block.statements[index].accept(this, usage)
            }
        }

        /**
         * 检查单个表达式在未使用位置是否应报告 unused expression。
         */
        private fun checkExpression(expression: CfirExpression, data: UsageState) {
            if (!data.isUnused()) return
            // 官方 cjc 只在 Unit 返回位置的非 Unit 表达式上报 unused expression，尾部 `()` 是有效返回值。
            if (data == UsageState.UnusedUnitReturn && expression.coneTypeOrNull == ConePrimitiveType.UNIT) return
            if (expression.hasSideEffect()) return
            if (expression is CfirAnonymousFunctionExpression) return
            if (expression is CfirThisReceiverExpression && declaration is CfirFinalizer) return
            with(context) {
                reporter.reportOn(expression.source, CfirErrors.UNUSED_EXPRESSION)
            }
        }

        /**
         * 计算 match/if 分支结果的使用状态。
         */
        private fun UsageState.branchResultUsage(body: CfirExpression): UsageState =
            if (isUnused() && body.coneTypeOrNull == ConePrimitiveType.UNIT) UsageState.UnusedUnitReturn else this

        /**
         * 判断表达式求值是否可能产生副作用。
         */
        private fun CfirExpression.hasSideEffect(): Boolean {
            return when (this) {
                is CfirLiteralExpression,
                is CfirThisReceiverExpression,
                is CfirAnonymousFunctionExpression,
                    -> false

                is CfirWrappedExpression -> expression.hasSideEffect()
                is CfirOptionalExpression -> expression.hasSideEffect()
                is CfirSmartCastExpression -> originalExpression.hasSideEffect()
                is CfirTupleLiteral -> elements.any { it.hasSideEffect() }

                is CfirFunctionCall -> true

                is CfirQualifiedAccessExpression -> {
                    /*
                     * 官方 unused 诊断来自 CHIR DCE：LOAD/FIELD/GET_ELEMENT_REF 这类取值
                     * 表达式在结果无用户时可被报告。CFIR 中非调用的 qualified access
                     * 对应这类取值；只有 receiver 求值本身有副作用时才阻止报告。
                     */
                    hasAccessReceiverSideEffect()
                }

                else -> true
            }
        }

        /**
         * 判断 qualified access 的接收者求值是否有副作用。
         */
        private fun CfirQualifiedAccessExpression.hasAccessReceiverSideEffect(): Boolean {
            if (calleeReference is CfirDiagnosticHolder) return true
            if (!isValueLikeAccess()) return true
            if (explicitReceiver?.hasSideEffect() == true) return true
            return dispatchReceiver !== explicitReceiver && dispatchReceiver?.hasSideEffect() == true
        }

        /**
         * 判断 qualified access 是否只是取值访问。
         */
        private fun CfirQualifiedAccessExpression.isValueLikeAccess(): Boolean {
            val symbol = (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol ?: return false
            return symbol is CfirVariableSymbol<*> ||
                    symbol is CfirPropertySymbol ||
                    symbol is CfirEnumConstructorSymbol
        }

        /**
         * 对齐 Kotlin `FirUnusedCheckerBase.isUnitBlock`：match/when 分支中的纯 `Unit` 结果
         * 是分支占位结果，不作为被丢弃的普通表达式报告。
         */
        private fun CfirExpression.isPureUnitBranchResult(): Boolean {
            val singleResult = when (this) {
                is CfirBlock -> statements.singleOrNull() as? CfirExpression
                else -> this
            }
            return singleResult is CfirLiteralExpression && singleResult.coneTypeOrNull == ConePrimitiveType.UNIT
        }
    }
}

/**
 * 对齐官方 CHIR DCE 的声明级 warning 入口。
 *
 * 当前 CFIR 不构造 CHIR user graph，因此这里在文件级先收集已解析函数/参数引用，
 * 再只对不可导出的静态函数声明及其形参模拟官方 DCE warning。
 */
object CfirDceUnusedDeclarationChecker : CfirFileChecker() {
    /**
     * 对单个文件执行 DCE 风格的 unused function/parameter 检查。
     *
     * 先收集文件内所有可报告的静态函数及引用集合，再只对没有被引用的函数和参数报告
     * warning，避免把导出 API 或泛型上下文中的声明误判为死代码。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val usage = DceUsageCollector.collect(declaration)

        for (functionInfo in usage.functions) {
            val function = functionInfo.function
            if (!function.isDceReportableStaticFunction(functionInfo)) continue

            if (function.symbol !in usage.usedFunctionSymbols) {
                reporter.reportOn(function.source, CfirErrors.UNUSED_FUNCTION)
            }

            for (parameter in function.valueParameters) {
                if (parameter.name.asString().startsWith("_")) continue
                if (parameter.symbol in usage.usedVariableSymbols) continue
                reporter.reportOn(parameter.valueParameterNameDiagnosticSource(), CfirErrors.UNUSED_VARIABLE)
            }
        }
    }

    /**
     * 判断静态函数是否符合当前 DCE unused 诊断的报告条件。
     *
     * 只有有函数体、非抽象/foreign/override、非 public API、且不处于泛型声明环境中的
     * static 函数才会被纳入候选。
     */
    private fun CfirNamedFunction.isDceReportableStaticFunction(info: FunctionDceInfo): Boolean {
        if (body == null) return false
        if (!status.isStatic) return false
        if (status.isAbstract || status.isForeign || status.isOverride) return false
        if (status.visibility.isPublicAPI) return false
        if (typeParameters.isNotEmpty() || info.isInsideGenericOwner) return false
        if (info.hasStaticNonStaticOverloadConflict) return false
        return true
    }

    /**
     * DCE 候选函数及其所在泛型上下文信息。
     */
    private data class FunctionDceInfo(
        /**
         * 被收集到的命名函数声明。
         */
        val function: CfirNamedFunction,

        /**
         * 函数是否位于带类型参数的类、接口、结构、枚举、扩展或函数内部。
         */
        val isInsideGenericOwner: Boolean,

        /**
         * 同一声明作用域内是否存在同名 non-static 函数。
         */
        val hasStaticNonStaticOverloadConflict: Boolean,
    )

    /**
     * 文件级 DCE 使用关系快照。
     */
    private data class DceUsage(
        /**
         * 文件中所有可进一步筛选的函数候选。
         */
        val functions: List<FunctionDceInfo>,

        /**
         * 已经被 qualified access 或 call 引用到的函数符号集合。
         */
        val usedFunctionSymbols: Set<CfirNamedFunctionSymbol>,

        /**
         * 已经被 qualified access 或 call 引用到的变量符号集合。
         */
        val usedVariableSymbols: Set<CfirVariableSymbol<*>>,
    )

    /**
     * 文件级 DCE 引用收集 visitor。
     *
     * visitor 同时记录声明候选和实际引用，并用 genericOwnerDepth 标记当前遍历位置是否
     * 处于泛型所有者内部。
     */
    private class DceUsageCollector : CfirDefaultVisitorVoid() {
        /**
         * 遍历过程中收集到的函数候选列表。
         */
        private val functions = mutableListOf<FunctionDceInfo>()

        /**
         * 遍历过程中发现的已使用函数符号。
         */
        private val usedFunctionSymbols = linkedSetOf<CfirNamedFunctionSymbol>()

        /**
         * 遍历过程中发现的已使用变量符号。
         */
        private val usedVariableSymbols = linkedSetOf<CfirVariableSymbol<*>>()

        /**
         * 当前所在泛型所有者嵌套深度。
         */
        private var genericOwnerDepth = 0

        /**
         * 当前声明作用域内的直接成员函数栈。extend 成员必须按 extend 自身作用域分组。
         */
        private val ownerFunctionStack = mutableListOf<List<CfirNamedFunction>>()

        /**
         * 默认遍历当前元素的所有子元素。
         */
        override fun visitElement(element: CfirElement) {
            element.acceptChildren(this)
        }

        /**
         * 访问类声明并在带类型参数时进入泛型所有者上下文。
         */
        override fun visitClass(klass: CfirClass) =
            visitDeclarationOwner(klass.typeParameters.isNotEmpty(), klass.declarations, klass)

        /**
         * 访问接口声明并在带类型参数时进入泛型所有者上下文。
         */
        override fun visitInterface(`interface`: CfirInterface) =
            visitDeclarationOwner(`interface`.typeParameters.isNotEmpty(), `interface`.declarations, `interface`)

        /**
         * 访问结构声明并在带类型参数时进入泛型所有者上下文。
         */
        override fun visitStruct(struct: CfirStruct) =
            visitDeclarationOwner(struct.typeParameters.isNotEmpty(), struct.declarations, struct)

        /**
         * 访问枚举声明并在带类型参数时进入泛型所有者上下文。
         */
        override fun visitEnum(enum: CfirEnum) =
            visitDeclarationOwner(enum.typeParameters.isNotEmpty(), enum.declarations, enum)

        /**
         * 访问扩展声明并在带类型参数时进入泛型所有者上下文。
         */
        override fun visitExtend(extend: CfirExtend) =
            visitDeclarationOwner(extend.typeParameters.isNotEmpty(), extend.declarations, extend)

        /**
         * 记录命名函数候选，并继续遍历函数体中的使用关系。
         */
        override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
            functions += FunctionDceInfo(
                function = namedFunction,
                isInsideGenericOwner = genericOwnerDepth > 0,
                hasStaticNonStaticOverloadConflict = namedFunction.hasStaticNonStaticOverloadConflictInCurrentOwner(),
            )
            visitGenericOwner(namedFunction.typeParameters.isNotEmpty(), namedFunction)
        }

        /**
         * 从 qualified access 中收集函数和变量使用。
         */
        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
            collectUsage(qualifiedAccessExpression)
            qualifiedAccessExpression.acceptChildren(this)
        }

        /**
         * 从函数调用中收集函数和变量使用。
         */
        override fun visitFunctionCall(functionCall: CfirFunctionCall) {
            collectUsage(functionCall)
            functionCall.acceptChildren(this)
        }

        /**
         * 收集单个 qualified access 表达式解析到的函数或变量符号。
         */
        private fun collectUsage(expression: CfirQualifiedAccessExpression) {
            expression.resolvedFunctionSymbolOrNull()?.let { usedFunctionSymbols += it }
            expression.resolvedVariableSymbolOrNull()?.let { usedVariableSymbols += it }
            expression.ambiguousFunctionSymbolsOrEmpty().forEach { usedFunctionSymbols += it }
        }

        /**
         * 在可选的泛型所有者上下文中遍历元素子树。
         */
        private fun visitGenericOwner(isGenericOwner: Boolean, element: CfirElement) {
            if (isGenericOwner) genericOwnerDepth++
            try {
                element.acceptChildren(this)
            } finally {
                if (isGenericOwner) genericOwnerDepth--
            }
        }

        /**
         * 在声明 owner 作用域中遍历，同时保存直接成员函数列表供 PreCheck 冲突分组复用。
         */
        private fun visitDeclarationOwner(
            isGenericOwner: Boolean,
            declarations: List<CfirDeclaration>,
            element: CfirElement,
        ) {
            ownerFunctionStack += declarations.filterIsInstance<CfirNamedFunction>()
            try {
                visitGenericOwner(isGenericOwner, element)
            } finally {
                ownerFunctionStack.removeAt(ownerFunctionStack.lastIndex)
            }
        }

        /**
         * 当前直接 owner 内同名 static/non-static 混用判断。
         */
        private fun CfirNamedFunction.hasStaticNonStaticOverloadConflictInCurrentOwner(): Boolean {
            if (!status.isStatic) return false
            val ownerFunctions = ownerFunctionStack.lastOrNull() ?: return false
            return ownerFunctions.any { sibling ->
                sibling.name == name && !sibling.status.isStatic
            }
        }

        /**
         * 构造最终的 DCE 使用关系快照。
         */
        fun result(): DceUsage = DceUsage(
            functions = functions,
            usedFunctionSymbols = usedFunctionSymbols,
            usedVariableSymbols = usedVariableSymbols,
        )

        /**
         * DCE 使用关系收集器的工厂入口。
         */
        companion object {
            /**
             * 从完整 CFIR 文件收集 DCE 使用关系。
             */
            fun collect(file: CfirFile): DceUsage = DceUsageCollector().apply {
                file.accept(this, null)
            }.result()
        }
    }
}

/**
 * 从 qualified access 中解析已绑定变量符号。
 */
private fun CfirQualifiedAccessExpression.resolvedVariableSymbolOrNull(): CfirVariableSymbol<*>? =
    when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirVariableSymbol<*>
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirVariableSymbol<*>
        else -> null
    }?.takeIf { it.isBound }

/**
 * 从 qualified access 中解析已绑定命名函数符号。
 */
private fun CfirQualifiedAccessExpression.resolvedFunctionSymbolOrNull(): CfirNamedFunctionSymbol? =
    when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirNamedFunctionSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirNamedFunctionSymbol
        else -> null
    }?.takeIf { it.isBound }

/**
 * 从歧义引用中收集全部函数候选。被引用但未能消解的函数不应再作为 unused-function 报告。
 */
private fun CfirQualifiedAccessExpression.ambiguousFunctionSymbolsOrEmpty(): List<CfirNamedFunctionSymbol> {
    val diagnostic = (calleeReference as? CfirDiagnosticHolder)?.diagnostic as? ConeAmbiguityError ?: return emptyList()
    return diagnostic.candidateSymbols.filterIsInstance<CfirNamedFunctionSymbol>()
}

/**
 * 判断表达式是否是 DCE 阶段跳过 unused variable 诊断的 `this` 初始化器形态。
 */
private fun CfirExpression.isDceSkippedThisInitializer(): Boolean =
    when (this) {
        is CfirThisReceiverExpression -> true
        is CfirWrappedExpression -> expression.isDceSkippedThisInitializer()
        is CfirSmartCastExpression -> originalExpression.isDceSkippedThisInitializer()
        else -> false
    }

/**
 * 保守判断表达式求值是否可能抛出异常。
 *
 * 官方 unused 诊断来自 CHIR DCE，catch 块只有在 try 主体存在可抛异常路径时才有
 * 可执行前驱。CFIR 这里只识别确定不抛的基础表达式；调用、运算和未知节点一律按
 * 可能抛处理，避免把真实可执行的 catch 误判为不可达。
 */
private fun CfirExpression.mayThrowException(): Boolean =
    when (this) {
        is CfirLiteralExpression,
        is CfirThisReceiverExpression,
        is CfirAnonymousFunctionExpression,
            -> false

        is CfirWrappedExpression -> expression.mayThrowException()
        is CfirOptionalExpression -> expression.mayThrowException()
        is CfirSmartCastExpression -> originalExpression.mayThrowException()
        is CfirTupleLiteral -> elements.any { it.mayThrowException() }

        is CfirBlock -> statements.any { statement ->
            (statement as? CfirExpression)?.mayThrowException() == true
        }

        is CfirReturnExpression -> result.mayThrowException()
        is CfirBreakExpression,
        is CfirContinueExpression,
            -> false

        is CfirThrowExpression -> true
        is CfirIfExpression ->
            condition.mayThrowException() ||
                thenBranch.mayThrowException() ||
                elseBranch?.mayThrowException() == true

        is CfirMatchExpression ->
            subject?.mayThrowException() == true ||
                branches.any { branch ->
                    branch.guard?.mayThrowException() == true || branch.body.mayThrowException()
                }

        is CfirTryExpression ->
            resources.any { it.initializer?.mayThrowException() == true } ||
                tryBlock.mayThrowException() ||
                catches.any { it.body.mayThrowException() } ||
                handlers.any { it.body.mayThrowException() } ||
                finallyBlock?.mayThrowException() == true

        else -> true
    }
