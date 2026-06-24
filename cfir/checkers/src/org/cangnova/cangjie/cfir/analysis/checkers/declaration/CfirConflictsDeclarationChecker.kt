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
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.patterns.bindingOccurrences
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.utils.SmartSet

/**
 * 平台可替换的声明冲突诊断分发器。
 *
 * 不同平台可以根据冲突符号类型选择专门诊断；默认实现覆盖函数重载冲突、classifier 重声明和
 * 通用 redeclaration。
 */
interface CfirPlatformConflictDeclarationsDiagnosticDispatcher : CfirSessionComponent {
    /**
     * 为给定冲突声明和冲突符号集合选择诊断工厂。
     */
    context(context: CheckerContext)
    fun getDiagnostic(
        conflictingDeclaration: CfirBasedSymbol<*>,
        symbols: SmartSet<CfirBasedSymbol<*>>,
    ): CjDiagnosticFactory1<Collection<String>>?

    /**
     * 默认声明冲突诊断分发器。
     */
    object DEFAULT : CfirPlatformConflictDeclarationsDiagnosticDispatcher {
        /**
         * 根据冲突符号类型选择默认诊断。
         */
        context(context: CheckerContext)
        override fun getDiagnostic(
            conflictingDeclaration: CfirBasedSymbol<*>,
            symbols: SmartSet<CfirBasedSymbol<*>>,
        ): CjDiagnosticFactory1<Collection<String>> {
            return when (conflictingDeclaration) {
                is CfirConstructorSymbol,
                is CfirFunctionSymbol<*>,

                is CfirEnumConstructorSymbol,
                -> if (symbols.any { it.isFunctionLikeRedeclaration() }) {
                    CfirErrors.CONFLICTING_OVERLOADS
                } else {
                    CfirErrors.REDECLARATION
                }

                is CfirClassLikeSymbol<*>
                    if symbols.any { it is CfirClassLikeSymbol<*> } ->
                    CfirErrors.CLASSIFIER_REDECLARATION

                else -> CfirErrors.REDECLARATION
            }
        }
    }
}

/**
 * 当前 session 中可选的平台声明冲突诊断分发器。
 */
val CfirSession.conflictDeclarationsDiagnosticDispatcher: CfirPlatformConflictDeclarationsDiagnosticDispatcher?
    by CfirSession.nullableSessionComponentAccessor()

/**
 * 声明冲突检查器。
 *
 * 该检查器在文件、class-like 和局部函数体三个层级收集同名/同签名冲突，并统一报告重声明或
 * 函数重载冲突诊断。
 */
object CfirConflictsDeclarationChecker : CfirBasicDeclarationChecker() {
    /**
     * 按声明类型分发冲突检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        when (declaration) {
            is CfirFile -> {
                val inspector = CfirDeclarationCollector<CfirBasedSymbol<*>>(context)
                checkFile(declaration, inspector)
                reportConflicts(inspector.declarationConflictingSymbols, declaration)
            }

            is CfirClassLikeDeclaration -> {
                if (declaration.source?.kind !is CjFakeSourceElementKind) {
                    checkForLocalRedeclarations(declaration.typeParameters)
                }

                val inspector = CfirDeclarationCollector<CfirBasedSymbol<*>>(context)
                inspector.collectClassMembers(declaration)
                reportConflicts(inspector.declarationConflictingSymbols, declaration)
            }

            else -> {
                if (declaration.source?.kind !is CjFakeSourceElementKind && declaration is CfirTypeParameterRefsOwner) {
                    checkForLocalRedeclarations(declaration.typeParameters)
                }

                if (declaration is CfirFunction) {
                    checkForLocalRedeclarations(declaration.valueParameters)
                    checkLocalRedeclarationsInFunctionBody(declaration)
                }

                if (declaration is CfirProperty) {
                    declaration.setter?.let { setter ->
                        checkForLocalRedeclarations(setter.valueParameters)
                    }
                }
            }
        }
    }

    /**
     * 根据冲突图报告声明冲突诊断。
     */
    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun reportConflicts(
        declarationConflictingSymbols: Map<CfirBasedSymbol<*>, SmartSet<CfirBasedSymbol<*>>>,
        container: CfirDeclaration,
    ) {
        declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            if (conflictingDeclaration.hasLaterFunctionConflictRepresentative(declarationConflictingSymbols)) {
                return@forEach
            }

            val finalizer = (conflictingDeclaration as? CfirCallableSymbol<*>)
                ?.takeIf { it.isBound }
                ?.cfir as? CfirFinalizer
            val source = when {
                conflictingDeclaration !is CfirCallableSymbol<*> -> conflictingDeclaration.boundSourceOrNull()
                !conflictingDeclaration.isBound -> container.source
                finalizer != null -> finalizer.finalizerNameDiagnosticSource()
                conflictingDeclaration.origin == CfirDeclarationOrigin.Source -> conflictingDeclaration.boundSourceOrNull()
                conflictingDeclaration.origin == CfirDeclarationOrigin.Library -> return@forEach
                else -> container.source
            }

            if (
                symbols.isEmpty() ||
                (conflictingDeclaration as? CfirConstructorSymbol)?.cfir?.isPrimary == true &&
                symbols.all { (it as? CfirConstructorSymbol)?.cfir?.isPrimary == true }
            ) {
                return@forEach
            }

            val dispatcher = context.session.conflictDeclarationsDiagnosticDispatcher
                ?: CfirPlatformConflictDeclarationsDiagnosticDispatcher.DEFAULT
            val factory = dispatcher.getDiagnostic(conflictingDeclaration, symbols) ?: return@forEach
            val renderedNames = symbols.renderNames()
            val patternVariable = (conflictingDeclaration as? CfirCallableSymbol<*>)?.cfir as? CfirPatternVariable
            if (patternVariable != null) {
                val conflictingNameSet = renderedNames.toSet()
                val bindingSources = patternVariable.pattern.bindingOccurrences()
                    .asSequence()
                    .filter { it.name.asString() in conflictingNameSet }
                    .mapNotNull { it.source }
                    .distinct()
                    .toList()
                if (bindingSources.isNotEmpty()) {
                    bindingSources.forEach { bindingSource ->
                        reporter.reportOn(bindingSource, factory, renderedNames)
                    }
                    return@forEach
                }
            }

            source ?: return@forEach
            reporter.reportOn(source, factory, renderedNames)
        }
    }

    /**
     * 收集单个文件的顶层声明冲突。
     */
    context(context: CheckerContext)
    private fun checkFile(file: CfirFile, inspector: CfirDeclarationCollector<CfirBasedSymbol<*>>) {
        val packageMemberScope = context.session.cangjieScopeProvider.getPackageMemberScope(
            packageFqName = file.packageDirective.packageFqName,
            symbolProvider = context.session.symbolProvider,
            useSiteSession = context.session,
            scopeSession = context.scopeSession,
        )
        inspector.collectTopLevel(file, packageMemberScope)
    }
}

/**
 * 检查函数体内部的局部重声明。
 */
context(reporter: DiagnosticReporter, context: CheckerContext)
private fun checkLocalRedeclarationsInFunctionBody(function: CfirFunction) {
    val body = function.body ?: return
    val visitor = LocalRedeclarationVisitor(reporter, context)
    visitor.withFunctionBodyScope(function) {
        body.statements.forEach { it.accept(visitor) }
    }
}

/**
 * 获取已绑定符号的源码位置。
 */
private fun CfirBasedSymbol<*>.boundSourceOrNull(): CjSourceElement? =
    if (isBound) cfir.source else null

/**
 * 仓颉官方编译器对同签名函数冲突只选择冲突簇中的一个声明承载
 * `sema_overload_conflicts`，其他声明作为 note。这里保留冲突图用于渲染，
 * 但避免把同一函数签名簇中的较早声明也报告为独立诊断。
 */
private fun CfirBasedSymbol<*>.hasLaterFunctionConflictRepresentative(
    declarationConflictingSymbols: Map<CfirBasedSymbol<*>, SmartSet<CfirBasedSymbol<*>>>,
): Boolean {
    if (!isFunctionLikeRedeclaration()) return false

    val sourceOffset = boundSourceOrNull()?.startOffset ?: return false
    val presentation = CfirRedeclarationPresenter.represent(this) ?: return false

    return declarationConflictingSymbols.any { (otherDeclaration, otherConflicts) ->
        otherDeclaration != this &&
            otherDeclaration.isFunctionLikeRedeclaration() &&
            otherConflicts.contains(this) &&
            otherDeclaration.boundSourceOrNull()?.startOffset?.let { it > sourceOffset } == true &&
            CfirRedeclarationPresenter.represent(otherDeclaration) == presentation
    }
}

/**
 * 渲染符号集合在 redeclaration 诊断中的名称列表。
 */
private fun Collection<CfirBasedSymbol<*>>.renderNames(): List<String> =
    asSequence().mapNotNull(CfirRedeclarationPresenter::diagnosticName).distinct().sorted().toList()

/**
 * 判断符号是否属于函数式重声明类别。
 */
private fun CfirBasedSymbol<*>.isFunctionLikeRedeclaration(): Boolean =
    this is CfirConstructorSymbol || this is CfirFunctionSymbol<*> || this is CfirEnumConstructorSymbol

/**
 * 函数体局部重声明 visitor。
 *
 * @property reporter 诊断报告器。
 * @property context 当前检查上下文。
 */
private class LocalRedeclarationVisitor(
    /**
     * 诊断报告器。
     */
    private val reporter: DiagnosticReporter,

    /**
     * 当前检查上下文。
     */
    private val context: CheckerContext,
) : CfirDefaultVisitorVoid() {
    /**
     * 局部声明作用域栈。
     */
    private val scopes = ArrayDeque<MutableMap<Name, MutableList<CfirBasedSymbol<*>>>>()

    /**
     * 以函数体作用域执行局部重声明检查。
     */
    fun withFunctionBodyScope(function: CfirFunction, body: () -> Unit) {
        withScope {
            function.valueParameters.forEach { declare(it, report = false) }
            body()
        }
    }

    /**
     * 默认访问子节点。
     */
    override fun visitElement(element: CfirElement) {
        element.acceptChildren(this)
    }

    /**
     * block 引入新的局部作用域。
     */
    override fun visitBlock(block: CfirBlock) {
        withScope {
            block.statements.forEach { it.accept(this) }
        }
    }

    /**
     * for-in 变量在循环体作用域中声明。
     */
    override fun visitForInExpression(forInExpression: CfirForInExpression) {
        forInExpression.iterable.accept(this)
        withScope {
            declarePatternVariable(forInExpression.variable)
            forInExpression.body.statements.forEach { it.accept(this) }
        }
    }

    /**
     * match branch 的 pattern binding 在分支作用域中声明。
     */
    override fun visitMatchBranch(matchBranch: CfirMatchBranch) {
        withScope {
            declarePattern(matchBranch.pattern)
            matchBranch.guard?.accept(this)
            matchBranch.body.statements.forEach { it.accept(this) }
        }
    }

    /**
     * 记录局部函数声明。
     */
    override fun visitFunction(function: CfirFunction) {
        if (function is CfirAnonymousFunction) {
            function.acceptChildren(this)
            return
        }
        if (function.isLocal) {
            declare(function.symbol)
        }
    }

    /**
     * 记录局部属性声明。
     */
    override fun visitProperty(property: CfirProperty) {
        if (property.isLocal) {
            declare(property.symbol)
        }
    }

    /**
     * 记录局部变量或 pattern 变量声明。
     */
    override fun visitVariable(variable: CfirVariable) {
        variable.initializer?.accept(this)
        if (variable is CfirPatternVariable) {
            declarePatternVariable(variable)
        } else {
            declare(variable)
        }
    }

    /**
     * 声明普通变量符号。
     */
    private fun declare(variable: CfirVariable, report: Boolean = true) {
        declare(variable.symbol, report)
    }

    /**
     * 声明 pattern variable 中可见的 binding 变量。
     */
    private fun declarePatternVariable(variable: CfirPatternVariable, report: Boolean = true) {
        declarePattern(variable.pattern, report)
    }

    /**
     * 声明 pattern 中所有可见 binding 变量。
     */
    private fun declarePattern(pattern: org.cangnova.cangjie.cfir.patterns.CfirPattern, report: Boolean = true) {
        pattern.visibleBindingVariables().forEach { bindingVariable ->
            declare(bindingVariable.symbol, report)
        }
    }

    /**
     * 在当前作用域注册符号并报告同作用域冲突。
     */
    private fun declare(symbol: CfirBasedSymbol<*>, report: Boolean = true) {
        val name = CfirRedeclarationPresenter.diagnosticName(symbol)?.let(Name::identifier) ?: return
        if (name.isSpecial) return

        val currentScope = scopes.lastOrNull() ?: return
        val declarations = currentScope.getOrPut(name, ::mutableListOf)
        val previousConflicts = declarations.filter { symbol.conflictsWithLocalDeclaration(it) }
        declarations += symbol
        if (previousConflicts.isNotEmpty() && report) {
            val source = symbol.boundSourceOrNull() ?: return
            val conflicts = previousConflicts + symbol
            val factory = if (symbol.isFunctionLikeRedeclaration() && previousConflicts.any { it.isFunctionLikeRedeclaration() }) {
                CfirErrors.CONFLICTING_OVERLOADS
            } else {
                CfirErrors.REDECLARATION
            }
            reporter.reportOn(source, factory, conflicts.renderNames(), context)
        }
    }

    /**
     * 判断两个局部声明符号是否互相冲突。
     */
    private fun CfirBasedSymbol<*>.conflictsWithLocalDeclaration(other: CfirBasedSymbol<*>): Boolean {
        if (this is CfirFunctionSymbol<*> && other is CfirFunctionSymbol<*>) {
            return CfirRedeclarationPresenter.represent(this) == CfirRedeclarationPresenter.represent(other)
        }
        return true
    }

    /**
     * 建立并释放一个局部作用域。
     */
    private inline fun withScope(body: () -> Unit) {
        scopes.addLast(linkedMapOf())
        try {
            body()
        } finally {
            scopes.removeLast()
        }
    }
}
