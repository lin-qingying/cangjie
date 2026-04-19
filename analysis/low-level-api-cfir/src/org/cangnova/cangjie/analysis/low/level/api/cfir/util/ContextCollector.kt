/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.withCfirDesignationEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.getNonLocalContainingOrThisDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.isAutonomousElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CfirElementsRecorder.Companion.anchorPsi
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLPartialBodyElementMapper
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector.Context
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector.ContextKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector.FilterResponse
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.resolve.codeFragmentContext
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.resolve.SessionHolderImpl
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.util.PrivateForInline
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 收集 PSI 位置对应的 tower 上下文。
 *
 * Kotlin `ContextCollector` 还会拼接 CFG / smart-cast 状态。
 * 当前仓颉主干没有接入那套完整 DFA，因此这里只保留已经在主干中成立的
 * `CfirTowerDataContext` 收集逻辑，不再伪造不存在的 smart-cast 快照。
 */
object ContextCollector {
    enum class ContextKind {
        /** 声明或表达式本身所在的位置上下文。 */
        SELF,

        /** 进入 body 之后的位置上下文。 */
        BODY,
    }

    class Context(
        val towerDataContext: CfirTowerDataContext,
    )

    enum class FilterResponse {
        CONTINUE,
        STOP,
        SKIP,
    }

    fun process(
        resolutionFacade: LLResolutionFacade,
        file: CfirFile,
        targetElement: PsiElement,
        preferBodyContext: Boolean = true,
    ): Context? {
        val designation = computeDesignation(file, targetElement)
        val shouldTriggerBodyAnalysis = !partiallyResolveTargetElementIfPossible(resolutionFacade, designation, targetElement)
        val acceptedElements = targetElement.parentsWithSelf.toSet()

        val contextProvider = process(file, designation, preferBodyContext, shouldTriggerBodyAnalysis) { candidate ->
            when (candidate) {
                targetElement -> FilterResponse.STOP
                in acceptedElements -> FilterResponse.CONTINUE
                else -> FilterResponse.SKIP
            }
        }

        for (acceptedElement in acceptedElements) {
            if (preferBodyContext && acceptedElement === targetElement) {
                val bodyContext = contextProvider[acceptedElement, ContextKind.BODY]
                if (bodyContext != null) {
                    return bodyContext
                }
            }

            val elementContext = contextProvider[acceptedElement, ContextKind.SELF]
            if (elementContext != null) {
                return elementContext
            }
        }

        return null
    }

    private fun partiallyResolveTargetElementIfPossible(
        resolutionFacade: LLResolutionFacade,
        designation: CfirDesignation?,
        targetElement: PsiElement,
    ): Boolean {
        val declaration = designation?.target?.realPsi as? CjDeclaration ?: return false
        val resolvedElement = targetElement
            .getParentOfType<CjElement>(strict = false)
            ?.takeIf { LLPartialBodyElementMapper.isPartiallyAnalyzable(it, declaration) }
            ?: return false

        return resolutionFacade.getOrBuildCfirFor(resolvedElement) != null
    }

    fun computeDesignation(file: CfirFile, targetElement: PsiElement): CfirDesignation? {
        val contextCjDeclaration = targetElement.getNonLocalContainingOrThisDeclaration(::isValidTarget)
        if (contextCjDeclaration != null) {
            return CfirElementFinder.collectDesignationPath(file, contextCjDeclaration)
        }

        return null
    }

    private fun isValidTarget(declaration: CjDeclaration): Boolean {
        return declaration.isAutonomousElement
    }

    fun process(
        file: CfirFile,
        designation: CfirDesignation?,
        preferBodyContext: Boolean,
        shouldTriggerBodyAnalysis: Boolean,
        filter: (PsiElement) -> FilterResponse,
    ): ContextProvider {
        val fileSession = file.llCfirSession
        val holder = SessionHolderImpl(fileSession, fileSession.getScopeSession())
        val interceptor = designation?.let(::DesignationInterceptor)
        val visitor = ContextCollectorVisitor(holder, preferBodyContext, shouldTriggerBodyAnalysis, filter, interceptor)

        visitor.collect(file)

        return ContextProvider { element, kind -> visitor[element, kind] }
    }

    fun interface ContextProvider {
        operator fun get(element: PsiElement, kind: ContextKind): Context?
    }
}

private class DesignationInterceptor(val designation: CfirDesignation) : () -> CfirElement? {
    private val targetIterator = iterator {
        yieldAll(designation.path)
        yield(designation.target)
    }

    override fun invoke(): CfirElement? = if (targetIterator.hasNext()) targetIterator.next() else null
}

private class ContextCollectorVisitor(
    private val bodyHolder: SessionAndScopeSessionHolder,
    private val shouldCollectBodyContext: Boolean,
    private val shouldTriggerBodyAnalysis: Boolean,
    private val filter: (PsiElement) -> FilterResponse,
    private val designationPathInterceptor: DesignationInterceptor?,
) : CfirDefaultVisitorVoid() {
    private data class ContextKey(val element: PsiElement, val kind: ContextKind)

    private val result = HashMap<ContextKey, Context>()
    private val parents = ArrayList<CfirElement>()

    private val context = BodyResolveContext(
        returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
        dataFlowAnalyzerContext = CfirDataFlowAnalyzerContext(),
        isContextCollectorMode = true,
    )

    private var isActive = true

    fun collect(file: CfirFile) {
        context.withFile(file) {
            if (designationPathInterceptor != null) {
                withInterceptor {
                    errorWithAttachment("Designation path is empty") {
                        withCfirEntry("file", file)
                        withCfirDesignationEntry("designation", designationPathInterceptor.designation)
                    }
                }
            } else {
                file.accept(this)
            }
        }
    }

    operator fun get(element: PsiElement, kind: ContextKind): Context? {
        return result[ContextKey(element, kind)]
    }

    private fun getSessionHolder(declaration: CfirDeclaration): SessionAndScopeSessionHolder {
        return when (val session = declaration.moduleData.session) {
            bodyHolder.session -> bodyHolder
            else -> SessionHolderImpl(session, bodyHolder.scopeSession)
        }
    }

    override fun visitElement(element: CfirElement) {
        dumpContext(element, ContextKind.SELF)

        withParent(element) {
            dumpContext(element, ContextKind.BODY)
            element.acceptChildren(this)
        }
    }

    override fun visitFile(file: CfirFile) = withProcessor(file) {
        dumpContext(file, ContextKind.SELF)

        process(file.packageDirective)
        processList(file.imports)
        processAnnotations(file)

        onActive {
            dumpContext(file, ContextKind.BODY)
            withInterceptor {
                processChildren(file)
            }
        }
    }

    override fun visitCodeFragment(codeFragment: CfirCodeFragment) = withProcessor(codeFragment) {
        dumpContext(codeFragment, ContextKind.SELF)

        codeFragment.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        onActive {
            val holder = getSessionHolder(codeFragment)
            context.withCodeFragmentCompat(codeFragment, holder) {
                dumpContext(codeFragment, ContextKind.BODY)
                process(codeFragment.block)
            }
        }
    }

    override fun visitClass(klass: CfirClass) {
        handleClassLikeDeclaration(klass)
    }

    override fun visitInterface(`interface`: CfirInterface) {
        handleClassLikeDeclaration(`interface`)
    }

    override fun visitStruct(struct: CfirStruct) {
        handleClassLikeDeclaration(struct)
    }

    override fun visitEnum(enum: CfirEnum) {
        handleClassLikeDeclaration(enum)
    }

    private fun handleClassLikeDeclaration(declaration: CfirClassLikeDeclaration) = withProcessor(declaration) {
        dumpContext(declaration, ContextKind.SELF)
        processAnnotations(declaration)

        context.withContainer(declaration) {
            processList(declaration.typeParameters)
            processList(declaration.superTypeRefs)
        }

        onActive {
            declaration.lazyResolveToPhase(CfirResolvePhase.STATUS)

            val holder = getSessionHolder(declaration)
            context.withClassLikeBodyCompat(declaration, holder) {
                dumpContext(declaration, ContextKind.BODY)
                withInterceptor {
                    processChildren(declaration)
                }
            }
        }
    }

    override fun visitExtend(extend: CfirExtend) = withProcessor(extend) {
        dumpContext(extend, ContextKind.SELF)
        processAnnotations(extend)

        context.withTypeParametersCompat(extend) {
            process(extend.extendedTypeRef)
            processList(extend.superTypeRefs)
        }

        onActive {
            context.withContainer(extend) {
                dumpContext(extend, ContextKind.BODY)
                processChildren(extend)
            }
        }
    }

    override fun visitTypeAlias(typeAlias: CfirTypeAlias) = withProcessor(typeAlias) {
        dumpContext(typeAlias, ContextKind.SELF)
        processAnnotations(typeAlias)

        context.withTypeParametersCompat(typeAlias) {
            processList(typeAlias.typeParameters)
            process(typeAlias.expandedTypeRef)
        }

        dumpContext(typeAlias, ContextKind.BODY)
    }

    override fun visitConstructor(constructor: CfirConstructor) = withProcessor(constructor) {
        dumpContext(constructor, ContextKind.SELF)
        processAnnotations(constructor)

        onActive {
            constructor.performBodyAnalysis()

            val holder = getSessionHolder(constructor)
            context.withConstructorCompat(constructor, holder) {
                processList(constructor.valueParameters)

                onActive {
                    context.forConstructorBodyCompat(constructor) {
                        dumpContext(constructor, ContextKind.BODY)
                        processDeclarationBody(constructor, constructor.body)
                    }
                }
            }
        }
    }

    override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
        visitFunctionLike(namedFunction)
    }

    override fun visitMainFunction(mainFunction: CfirMainFunction) {
        visitFunctionLike(mainFunction)
    }

    override fun visitMacroDeclaration(macroDeclaration: CfirMacroDeclaration) {
        visitFunctionLike(macroDeclaration)
    }

    override fun visitFinalizer(finalizer: CfirFinalizer) {
        visitFunctionLike(finalizer)
    }

    override fun visitPropertyAccessor(propertyAccessor: CfirPropertyAccessor) = withProcessor(propertyAccessor) {
        dumpContext(propertyAccessor, ContextKind.SELF)
        processAnnotations(propertyAccessor)

        onActive {
            val holder = getSessionHolder(propertyAccessor)
            context.withPropertyAccessorCompat(propertyAccessor, holder) {
                processList(propertyAccessor.valueParameters)
                dumpContext(propertyAccessor, ContextKind.BODY)
                processDeclarationBody(propertyAccessor, propertyAccessor.body)
            }
        }
    }

    private fun visitFunctionLike(function: CfirFunction) = withProcessor(function) {
        dumpContext(function, ContextKind.SELF)
        processAnnotations(function)

        onActive {
            function.performBodyAnalysis()

            val holder = getSessionHolder(function)
            context.withFunctionDeclarationCompat(function) {
                processList(function.typeParameters)
                process(function.returnTypeRef)
                processList(function.valueParameters)

                onActive {
                    context.forFunctionBodyCompat(function) {
                        dumpContext(function, ContextKind.BODY)
                        processDeclarationBody(function, function.body)
                    }
                }
            }
        }
    }

    override fun visitProperty(property: CfirProperty) = withProcessor(property) {
        dumpContext(property, ContextKind.SELF)
        processAnnotations(property)

        onActive {
            property.performBodyAnalysis()

            context.withPropertyCompat(property) {
                processList(property.typeParameters)
                process(property.returnTypeRef)

                dumpContext(property, ContextKind.BODY)
                process(property.getter)
                process(property.setter)
            }
        }
    }

    override fun visitFieldVariable(fieldVariable: CfirFieldVariable) {
        visitVariableLike(fieldVariable, fieldVariable.symbol, fieldVariable.initializer)
    }

    override fun visitPatternVariable(patternVariable: CfirPatternVariable) {
        visitVariableLike(patternVariable, patternVariable.symbol, patternVariable.initializer)
    }

    override fun visitPatternBindingVariable(patternBindingVariable: CfirPatternBindingVariable) {
        visitVariableLike(patternBindingVariable, patternBindingVariable.symbol, patternBindingVariable.initializer)
    }

    private fun visitVariableLike(
        variable: CfirDeclaration,
        symbol: CfirCallableSymbol<*>,
        initializer: CfirElement?,
    ) = withProcessor(variable) {
        dumpContext(variable, ContextKind.SELF)

        if (variable is CfirFieldVariable) {
            processAnnotations(variable)
            process(variable.returnTypeRef)
        }

        onActive {
            context.withLocalVariableBodyCompat {
                dumpContext(variable, ContextKind.BODY)
                process(initializer)
            }
        }

        context.storeVariable(symbol.name, symbol)
    }

    override fun visitValueParameter(valueParameter: CfirValueParameter) = withProcessor(valueParameter) {
        dumpContext(valueParameter, ContextKind.SELF)
        processAnnotations(valueParameter)
        process(valueParameter.returnTypeRef)

        onActive {
            context.withValueParameterCompat(valueParameter) {
                dumpContext(valueParameter, ContextKind.BODY)
                process(valueParameter.defaultValue)
            }
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: CfirAnonymousFunction) = withProcessor(anonymousFunction) {
        dumpContext(anonymousFunction, ContextKind.SELF)
        processAnnotations(anonymousFunction)

        onActive {
            anonymousFunction.performBodyAnalysis()

            context.withAnonymousFunctionCompat(anonymousFunction) {
                processList(anonymousFunction.typeParameters)
                process(anonymousFunction.returnTypeRef)
                processList(anonymousFunction.valueParameters)

                dumpContext(anonymousFunction, ContextKind.BODY)
                processDeclarationBody(anonymousFunction, anonymousFunction.body)
            }
        }
    }

    override fun visitBlock(block: org.cangnova.cangjie.cfir.expressions.CfirBlock) {
        doVisitBlock(block)
    }

    private fun doVisitBlock(
        block: org.cangnova.cangjie.cfir.expressions.CfirBlock,
        isolateBlock: Boolean = true,
    ) = withProcessor(block) {
        dumpContext(block, ContextKind.SELF)

        onActive {
            if (isolateBlock) {
                context.withBlockScopeCompat {
                    processBlockBody(block)
                }
            } else {
                processBlockBody(block)
            }
        }
    }

    private fun Processor.processBlockBody(block: org.cangnova.cangjie.cfir.expressions.CfirBlock) {
        processChildren(block, checkIsActive = false)
        dumpContext(block, ContextKind.BODY)
    }

    private fun dumpContext(element: CfirElement, kind: ContextKind, hasBodyContext: Boolean = true) {
        ProgressManager.checkCanceled()

        if (kind == ContextKind.BODY && !shouldCollectBodyContext) {
            return
        }

        val psi = element.anchorPsi ?: return
        val key = ContextKey(psi, kind)
        if (key in result) {
            return
        }

        val response = filter(psi)
        if (response != FilterResponse.SKIP) {
            result[key] = Context(context.towerDataContext.createSnapshot(keepMutable = true))
        }

        if (response == FilterResponse.STOP) {
            if (kind == ContextKind.BODY || !(hasBodyContext && shouldCollectBodyContext)) {
                isActive = false
            }
        }
    }

    private fun Processor.processDeclarationBody(
        declaration: CfirDeclaration,
        body: org.cangnova.cangjie.cfir.expressions.CfirBlock?,
    ) {
        if (!isActive) {
            return
        }

        val snapshot = declaration.partialBodyAnalysisState?.analysisStateSnapshot
        if (snapshot != null) {
            context.withBlockScopeCompat {
                for (statement in snapshot.result.statements) {
                    statement.accept(this@ContextCollectorVisitor)
                    if (!isActive) {
                        break
                    }
                }
            }
            return
        }

        process(body)
    }

    private fun Processor.processAnnotations(declaration: CfirDeclaration) {
        @OptIn(PrivateForInline::class)
        context.withContainer(declaration) {
            for (annotation in declaration.annotations) {
                process(annotation)
            }
        }
    }

    private inline fun withProcessor(parent: CfirElement, block: Processor.() -> Unit) {
        withParent(parent) {
            Processor(this).block()
        }
    }

    private inner class Processor(private val delegate: CfirVisitorVoid) {
        private val elementsToSkip = HashSet<CfirElement>()

        fun process(element: CfirElement?) {
            if (isActive && element != null) {
                element.accept(delegate)
                elementsToSkip += element
            }
        }

        fun processList(elements: Collection<CfirElement>) {
            for (element in elements) {
                if (!isActive) {
                    break
                }

                process(element)
            }
        }

        fun processChildren(element: CfirElement, checkIsActive: Boolean = true) {
            if (checkIsActive && !isActive) {
                return
            }

            val visitor = FilteringVisitor(delegate, elementsToSkip, checkIsActive)
            element.acceptChildren(visitor)
        }
    }

    private inner class FilteringVisitor(
        private val delegate: CfirVisitorVoid,
        private val elementsToSkip: Set<CfirElement>,
        private val checkIsActive: Boolean,
    ) : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (checkIsActive && !isActive) {
                return
            }

            if (element !in elementsToSkip) {
                element.accept(delegate)
            }
        }
    }

    private fun CfirDeclaration.performBodyAnalysis() {
        if (!shouldTriggerBodyAnalysis && partialBodyAnalysisState != null) {
            return
        }

        lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
    }

    private fun withInterceptor(block: () -> Unit) {
        val target = designationPathInterceptor?.invoke()
        if (target != null) {
            target.accept(this)
        } else {
            block()
        }
    }

    private inline fun withParent(parent: CfirElement, block: () -> Unit) {
        parents.add(parent)
        try {
            block()
        } finally {
            parents.removeLast()
        }
    }

    private inline fun onActive(block: () -> Unit) {
        if (isActive) {
            block()
        }
    }
}

/**
 * 以下兼容层只服务 low-level context 收集。
 *
 * Kotlin `BodyResolveContext` 提供了大量针对脚本、CFG、receiver DSL 的 helper；
 * 当前仓颉主干尚未补齐这些能力，因此这里只把 low-level 实际需要的上下文边界
 * 收紧到已经存在的主干结构。
 */
private inline fun BodyResolveContext.withTypeParametersCompat(
    declaration: CfirTypeParameterRefsOwner,
    block: () -> Unit,
) {
    val typeParameters = declaration.typeParameters.filterIsInstance<CfirTypeParameter>()
    if (typeParameters.isEmpty()) {
        block()
        return
    }

    withTowerDataCleanup {
        addNonLocalScope(CfirTypeParameterScopeImpl(typeParameters))
        block()
    }
}

private inline fun BodyResolveContext.withClassLikeBodyCompat(
    declaration: CfirClassLikeDeclaration,
    holder: SessionAndScopeSessionHolder,
    crossinline block: () -> Unit,
) {
    withScopesForClass(declaration, holder) {
        withContainer(declaration) {
            block()
        }
    }
}

private inline fun BodyResolveContext.withConstructorCompat(
    constructor: CfirConstructor,
    holder: SessionAndScopeSessionHolder,
    block: () -> Unit,
) {
    withTowerDataMode(org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirTowerDataMode.CONSTRUCTOR_HEADER) {
        withContainer(constructor) {
            withTowerDataCleanup {
                addLocalScope(CfirLocalScopeImpl())
                for (valueParameter in constructor.valueParameters) {
                    storeVariable(valueParameter.name, valueParameter.symbol)
                }
                block()
            }
        }
    }
}

private inline fun BodyResolveContext.forConstructorBodyCompat(
    constructor: CfirConstructor,
    block: () -> Unit,
) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScopeImpl())
        for (valueParameter in constructor.valueParameters) {
            storeVariable(valueParameter.name, valueParameter.symbol)
        }
        block()
    }
}

private inline fun BodyResolveContext.withFunctionDeclarationCompat(
    function: CfirFunction,
    block: () -> Unit,
) {
    if (containerIfAny !is CfirClassLikeDeclaration && containerIfAny !is CfirExtend) {
        val symbol = function.symbol as? CfirFunctionSymbol<*>
        if (symbol != null) {
            storeFunction(symbol.name, symbol)
        }
    }

    withTypeParametersCompat(function) {
        withContainer(function, block)
    }
}

private inline fun BodyResolveContext.forFunctionBodyCompat(
    function: CfirFunction,
    block: () -> Unit,
) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScopeImpl())
        for (valueParameter in function.valueParameters) {
            storeVariable(valueParameter.name, valueParameter.symbol)
        }
        block()
    }
}

private inline fun BodyResolveContext.withPropertyCompat(
    property: CfirProperty,
    block: () -> Unit,
) {
    withTypeParametersCompat(property) {
        withContainer(property, block)
    }
}

private inline fun BodyResolveContext.withPropertyAccessorCompat(
    propertyAccessor: CfirPropertyAccessor,
    holder: SessionAndScopeSessionHolder,
    block: () -> Unit,
) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScopeImpl())
        for (valueParameter in propertyAccessor.valueParameters) {
            storeVariable(valueParameter.name, valueParameter.symbol)
        }

        withPublicApiInlineFunction(propertyAccessor) {
            withContainer(propertyAccessor, block)
        }
    }
}

private inline fun BodyResolveContext.withValueParameterCompat(
    valueParameter: CfirValueParameter,
    block: () -> Unit,
) {
    storeVariable(valueParameter.name, valueParameter.symbol)
    withContainer(valueParameter, block)
}

private inline fun BodyResolveContext.withAnonymousFunctionCompat(
    anonymousFunction: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction,
    block: () -> Unit,
) {
    withTypeParametersCompat(anonymousFunction) {
        withTowerDataCleanup {
            addLocalScope(CfirLocalScopeImpl())
            withContainer(anonymousFunction) {
                block()
            }
        }
    }
}

private inline fun BodyResolveContext.withBlockScopeCompat(block: () -> Unit) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScopeImpl())
        block()
    }
}

private inline fun BodyResolveContext.withLocalVariableBodyCompat(block: () -> Unit) {
    withTowerDataCleanup(block)
}

private inline fun BodyResolveContext.withCodeFragmentCompat(
    codeFragment: CfirCodeFragment,
    holder: SessionAndScopeSessionHolder,
    block: () -> Unit,
) {
    val codeFragmentContext = codeFragment.codeFragmentContext
    if (codeFragmentContext == null) {
        withContainer(codeFragment, block)
        return
    }

    withTowerDataContext(codeFragmentContext.towerDataContext.createSnapshot(keepMutable = true)) {
        withContainer(codeFragment, block)
    }
}
