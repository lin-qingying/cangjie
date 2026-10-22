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

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.patterns.bindingOccurrences
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.utils.SmartSet
import java.util.IdentityHashMap

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

            is CfirExtend -> {
                if (declaration.source?.kind !is CjFakeSourceElementKind) {
                    checkForLocalRedeclarations(declaration.typeParameters)
                }

                val inspector = CfirDeclarationCollector<CfirBasedSymbol<*>>(context)
                inspector.collectExtendMembers(declaration)
                reportConflicts(
                    declarationConflictingSymbols = inspector.declarationConflictingSymbols,
                    container = declaration,
                    reportAllFunctionConflictParticipants = true,
                )
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
    @OptIn(InternalDiagnosticFactoryMethod::class)
    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun reportConflicts(
        declarationConflictingSymbols: Map<CfirBasedSymbol<*>, SmartSet<CfirBasedSymbol<*>>>,
        container: CfirDeclaration,
        reportAllFunctionConflictParticipants: Boolean = false,
    ) {
        val sourceOrder = CfirDeclarationConflictSourceOrder(context.session)
        val functionConflictGroups = CfirFunctionConflictGroups(declarationConflictingSymbols)
        val groupRepresentativesByGroup = IdentityHashMap<CfirFunctionConflictGroup, Set<CfirBasedSymbol<*>>>()
        val functionConflictReportState = reporter.getOrCreateCollectionState(CfirFunctionConflictReportState.KEY) {
            CfirFunctionConflictReportState()
        }
        functionConflictGroups.groups.forEach { group ->
            selectFunctionConflictRepresentative(
                group = group,
                container = container,
                conflictMapKeys = declarationConflictingSymbols.keys,
                sourceOrder = sourceOrder,
            )?.let { groupRepresentativesByGroup[group] = it }
        }

        declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            // Duplicate intrinsic declarations are owned by the parser-level
            // intrinsic checker; suppress the generic conflict graph result.
            if (conflictingDeclaration.isIntrinsicFunction() && symbols.any { it.isIntrinsicFunction() }) {
                return@forEach
            }

            val functionConflictGroup = functionConflictGroups.groupFor(conflictingDeclaration)
            val functionConflictRepresentatives = functionConflictGroup?.let(groupRepresentativesByGroup::get)
            val hasAmbiguousFunctionConflictRepresentative =
                !reportAllFunctionConflictParticipants && functionConflictRepresentatives.orEmpty().size > 1
            val isAmbiguousFunctionConflictRepresentative =
                hasAmbiguousFunctionConflictRepresentative &&
                    conflictingDeclaration in functionConflictRepresentatives.orEmpty()
            val isOverloadClusterPredecessor =
                functionConflictGroup != null &&
                    !reportAllFunctionConflictParticipants &&
                    conflictingDeclaration !in functionConflictRepresentatives.orEmpty()
            val reportSymbols = when {
                functionConflictGroup == null -> symbols
                isOverloadClusterPredecessor || functionConflictRepresentatives == null ->
                    SmartSet.create<CfirBasedSymbol<*>>().also { remaining ->
                        symbols.filterNot { functionConflictGroups.groupFor(it) === functionConflictGroup }
                            .forEach(remaining::add)
                    }
                conflictingDeclaration in functionConflictRepresentatives ||
                    reportAllFunctionConflictParticipants ->
                    SmartSet.create<CfirBasedSymbol<*>>().also { completeCluster ->
                        symbols.forEach(completeCluster::add)
                        functionConflictGroup.members.asSequence()
                            .filter { it !== conflictingDeclaration }
                            .forEach(completeCluster::add)
                    }
                else -> SmartSet.create()
            }
            if (reportSymbols.isEmpty()) return@forEach
            if (
                conflictingDeclaration.isPrimaryConstructor() &&
                reportSymbols.all { it.isPrimaryConstructor() }
            ) {
                return@forEach
            }

            val dispatcher = context.session.conflictDeclarationsDiagnosticDispatcher
                ?: CfirPlatformConflictDeclarationsDiagnosticDispatcher.DEFAULT
            val factory = if (isOverloadClusterPredecessor) {
                // 重载簇诊断已由较晚的同签名声明承载，本声明此处只剩与非函数式声明的同名冲突。
                CfirErrors.REDECLARATION
            } else {
                dispatcher.getDiagnostic(conflictingDeclaration, reportSymbols) ?: return@forEach
            }
            val renderedNames = reportSymbols.renderNames()
            val patternVariable = (conflictingDeclaration as? CfirCallableSymbol<*>)
                ?.takeIf { it.isBound }
                ?.cfir as? CfirPatternVariable
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

            val diagnosticSource = if (isAmbiguousFunctionConflictRepresentative) {
                // 多个候选缺少可证明的相对源码顺序时，在共同容器位置报告一次，避免哈希遍历
                // 决定诊断锚点或对同一完整冲突簇重复报告。
                container.source
            } else {
                conflictingDeclaration.conflictDiagnosticSource(container)
            }
            val source = diagnosticSource ?: return@forEach
            val diagnostic = factory.on(source, renderedNames, null, context) ?: return@forEach
            if (context.isDiagnosticSuppressed(diagnostic)) return@forEach
            if (
                isAmbiguousFunctionConflictRepresentative &&
                functionConflictGroup != null &&
                !functionConflictReportState.tryMarkReported(functionConflictGroup.members)
            ) {
                return@forEach
            }
            reporter.report(diagnostic, context)
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
 * 从完整同签名冲突簇中选择唯一诊断代表。
 *
 * Source 声明优先承载主诊断；synthetic 与继承成员继续留在簇中作为语义参与者和 notes。
 * 只有簇内没有源码 owner 时，当前 collector 自己的 synthetic key 才能用容器 source 报告。
 */
private fun selectFunctionConflictRepresentative(
    group: CfirFunctionConflictGroup,
    container: CfirDeclaration,
    conflictMapKeys: Set<CfirBasedSymbol<*>>,
    sourceOrder: CfirDeclarationConflictSourceOrder,
): Set<CfirBasedSymbol<*>>? {
    val scopeOwners = group.members.filter { it.isFunctionConflictOwner(container) }
    val sourceOwners = scopeOwners.filter { it.hasSourceConflictDeclaration() }
    val candidates = when {
        sourceOwners.isNotEmpty() -> sourceOwners
        scopeOwners.isNotEmpty() -> scopeOwners.filter { it.conflictDiagnosticSource(container) != null }
        else -> group.members.filter { member ->
            conflictMapKeys.any { it === member } &&
                !member.isBound &&
                member.conflictDiagnosticSource(container) != null
        }
    }
    if (candidates.isEmpty()) return null

    val representatives = identitySymbolSet()
    candidates.forEach { candidate ->
        val hasLaterCandidate = candidates.any { other ->
            other !== candidate && sourceOrder.compareForDiagnosticRepresentative(candidate, other) < 0
        }
        if (!hasLaterCandidate) representatives += candidate
    }
    if (representatives.isEmpty()) representatives.addAll(candidates)
    return representatives
}

/**
 * 判断函数簇成员是否属于当前声明容器的 owner 范围。
 *
 * 同名 ClassId 不能替代 class symbol identity；嵌套 class 构造器只在该精确嵌套声明由当前
 * class 直接拥有时作为报告候选。继承成员保留为冲突参与者，但不能代表当前 class 的源码成员。
 */
private fun CfirBasedSymbol<*>.isFunctionConflictOwner(container: CfirDeclaration): Boolean {
    if (!isFunctionLikeRedeclaration() || !isBound || origin == CfirDeclarationOrigin.Library) return false
    val callable = this as? CfirCallableSymbol<*> ?: return false
    val sourceDeclaration = sourceDeclarationForConflict()

    return when (container) {
        is CfirFile -> {
            val ownerFile = sourceDeclaration.getContainingFile() ?: getContainingFile() ?: return false
            val hasTopLevelOwner = if (sourceDeclaration !== this) {
                true
            } else {
                callable.getContainingClass() == null && callable.getContainingExtend() == null
            }
            hasTopLevelOwner &&
                ownerFile.moduleData.session === container.moduleData.session &&
                ownerFile.packageDirective.packageFqName == container.packageDirective.packageFqName
        }

        is CfirClassLikeDeclaration -> {
            if (sourceDeclaration !== this) return false
            val expectedOwner = container.symbol as? CfirClassLikeSymbol<*> ?: return false
            val actualOwner = callable.getContainingClass() ?: return false
            val sameClassOwner = actualOwner === expectedOwner
            val nestedConstructorOwner =
                (this is CfirConstructorSymbol || this is CfirEnumConstructorSymbol) &&
                    container.declarations.any { it === actualOwner.cfir }
            (sameClassOwner || nestedConstructorOwner) && callable.getContainingExtend() == null
        }

        is CfirExtend ->
            sourceDeclaration === this && callable.getContainingExtend() === container

        else -> false
    }
}

/** Source-backed declaration associated with a synthesized type-alias constructor. */
private fun CfirBasedSymbol<*>.sourceDeclarationForConflict(): CfirBasedSymbol<*> =
    if (isBound) {
        (this as? CfirConstructorSymbol)?.typeAliasConstructorInfo?.typeAliasSymbol ?: this
    } else {
        this
    }

/** Only source-origin declarations can outrank synthetic conflict participants as diagnostic owners. */
private fun CfirBasedSymbol<*>.hasSourceConflictDeclaration(): Boolean {
    val sourceDeclaration = sourceDeclarationForConflict()
    return sourceDeclaration.origin == CfirDeclarationOrigin.Source &&
        sourceDeclaration.isBound &&
        sourceDeclaration.cfir.source != null
}

/** Apply the CFIR reporter's source mapping after the cluster has selected its one report owner. */
private fun CfirBasedSymbol<*>.conflictDiagnosticSource(container: CfirDeclaration): AbstractCjSourceElement? {
    if (this !is CfirCallableSymbol<*>) return boundSourceOrNull()
    if (!isBound) return container.source

    val finalizer = cfir as? CfirFinalizer
    if (finalizer != null) return finalizer.finalizerNameDiagnosticSource()

    val typeAliasSource = (this as? CfirConstructorSymbol)
        ?.typeAliasConstructorInfo
        ?.typeAliasSymbol
        ?.cfir
        ?.source
    if (typeAliasSource != null) return typeAliasSource

    return when (origin) {
        CfirDeclarationOrigin.Source -> boundSourceOrNull()
        CfirDeclarationOrigin.Library -> null
        else -> container.source
    }
}

/** 未绑定构造器没有 primary 状态，不能通过 cfir 读取它。 */
private fun CfirBasedSymbol<*>.isPrimaryConstructor(): Boolean =
    this is CfirConstructorSymbol && isBound && cfir.isPrimary

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

/** Intrinsic duplicates have a dedicated parser-level diagnostic owner. */
private fun CfirBasedSymbol<*>.isIntrinsicFunction(): Boolean =
    (this as? CfirFunctionSymbol<*>)
        ?.takeIf { it.isBound }
        ?.cfir
        ?.hasBuiltinAnnotation(BuiltInAnnotationKind.INTRINSIC) == true

/** Symbols retain identity semantics in conflict groups and diagnostic representative sets. */
private fun identitySymbolSet(): MutableSet<CfirBasedSymbol<*>> =
    java.util.Collections.newSetFromMap(IdentityHashMap<CfirBasedSymbol<*>, Boolean>())

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
     * try 资源变量与 tryBlock 顶层声明共享一个局部作用域。
     *
     * 该作用域到 try 语句结束为止；catch / handler 子句各自有独立作用域，
     * finallyBlock 也是独立块作用域，均在 try 作用域之外遍历。
     */
    override fun visitTryExpression(tryExpression: CfirTryExpression) {
        withScope {
            tryExpression.resources.forEach { it.accept(this) }
            tryExpression.tryBlock.statements.forEach { it.accept(this) }
        }
        tryExpression.catches.forEach { it.accept(this) }
        tryExpression.handlers.forEach { it.accept(this) }
        tryExpression.finallyBlock?.accept(this)
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
     * while-let 的条件 binding 与循环体顶层共享一个局部作用域。
     *
     * 该作用域不与外层函数/块参数冲突，但循环体顶层声明会与条件引入的变量冲突；
     * body statements 直接遍历，避免 body block 再引入额外隔离层。
     */
    override fun visitLoopExpression(loopExpression: CfirLoopExpression) {
        if (loopExpression.isDoWhile || !loopExpression.condition.hasLetPatternCondition()) {
            loopExpression.acceptChildren(this)
            return
        }

        withScope {
            declareLetConditionBindings(loopExpression.condition)
            loopExpression.body.statements.forEach { it.accept(this) }
        }
    }

    /**
     * if-let 的条件 binding 与 then 分支顶层共享一个局部作用域。
     *
     * 该作用域对外层声明只做遮蔽，不做重声明冲突；then 分支顶层声明仍会与条件
     * 引入的 binding 冲突。else/else-if 按独立分支遍历，不能复用 then 侧作用域。
     */
    override fun visitIfExpression(ifExpression: CfirIfExpression) {
        if (!ifExpression.condition.hasLetPatternCondition()) {
            ifExpression.acceptChildren(this)
            return
        }

        withScope {
            declareLetConditionBindings(ifExpression.condition)
            ifExpression.thenBranch.statements.forEach { it.accept(this) }
        }
        ifExpression.elseBranch?.accept(this)
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
     * catch 参数只在当前 catch 子句内可见。
     *
     * 多个 catch 子句允许复用同名参数；参数与 catch body 顶层共享作用域，
     * 因此 body 内同名局部声明仍应报告重声明。
     */
    override fun visitCatch(catch: CfirCatch) {
        withScope {
            catch.pattern.bindingVariable?.let { declare(it.symbol) }
            catch.body.statements.forEach { it.accept(this) }
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
     * 判断条件树是否包含 let-pattern 条件。
     */
    private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.hasLetPatternCondition(): Boolean = when (this) {
        is CfirLetPatternExpression -> true
        is CfirBinaryOp -> (kind == CfirBinaryOpKind.AND || kind == CfirBinaryOpKind.OR) &&
            (left.hasLetPatternCondition() || right.hasLetPatternCondition())
        else -> false
    }

    /**
     * 收集条件中会进入循环体作用域的 pattern binding。
     */
    private fun declareLetConditionBindings(condition: org.cangnova.cangjie.cfir.expressions.CfirExpression) {
        when (condition) {
            is CfirLetPatternExpression -> declarePattern(condition.pattern)
            is CfirBinaryOp if condition.kind == CfirBinaryOpKind.AND || condition.kind == CfirBinaryOpKind.OR -> {
                declareLetConditionBindings(condition.left)
                declareLetConditionBindings(condition.right)
            }
            else -> condition.accept(this)
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
            // Intrinsic duplicates have a dedicated parser-level diagnostic
            // owner; do not add a second overload diagnostic for the same pair.
            if (symbol.isIntrinsicFunction() && previousConflicts.any { it.isIntrinsicFunction() }) return
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
