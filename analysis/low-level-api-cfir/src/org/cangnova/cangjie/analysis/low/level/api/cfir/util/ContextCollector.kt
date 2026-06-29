

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
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
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
    /**
     * 可收集的上下文位置类型。
     */
    enum class ContextKind {
        /** 声明或表达式本身所在的位置上下文。 */
        SELF,

        /** 进入 body 之后的位置上下文。 */
        BODY,
    }

    /**
     * 收集到的上下文快照。
     */
    class Context(
        /**
         * 指定 PSI 位置可见的 tower data context。
         */
        val towerDataContext: CfirTowerDataContext,
    )

    /**
     * PSI 过滤器对候选元素的处理决定。
     */
    enum class FilterResponse {
        /**
         * 继续向内访问该 PSI 对应的 CFIR 子树。
         */
        CONTINUE,
        /**
         * 接受当前上下文并停止继续收集。
         */
        STOP,
        /**
         * 跳过当前 PSI 对应的上下文记录。
         */
        SKIP,
    }

    /**
     * 为 [targetElement] 在 [file] 中收集最合适的上下文。
     */
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

    /**
     * 如果 [targetElement] 支持局部 body 分析，则先触发对应 CFIR 构建。
     */
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

    /**
     * 计算 [targetElement] 所在非局部声明的 designation。
     */
    fun computeDesignation(file: CfirFile, targetElement: PsiElement): CfirDesignation? {
        val contextCjDeclaration = targetElement.getNonLocalContainingOrThisDeclaration(::isValidTarget)
        if (contextCjDeclaration != null) {
            return CfirElementFinder.collectDesignationPath(file, contextCjDeclaration)
        }

        return null
    }

    /**
     * 判断 PSI 声明是否可作为上下文收集的非局部目标。
     */
    private fun isValidTarget(declaration: CjDeclaration): Boolean {
        return declaration.isAutonomousElement
    }

    /**
     * 使用 [filter] 遍历 [file] 并返回可按 PSI 查询上下文的 provider。
     */
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

    /**
     * 按 PSI 与上下文类型查询收集结果的 provider。
     */
    fun interface ContextProvider {
        /**
         * 返回 [element] 在 [kind] 位置的上下文快照。
         */
        operator fun get(element: PsiElement, kind: ContextKind): Context?
    }
}

/**
 * 将访问路径限制到给定 [designation] 的拦截器。
 */
private class DesignationInterceptor(val designation: CfirDesignation) : () -> CfirElement? {
    /**
     * designation 路径和目标组成的顺序迭代器。
     */
    private val targetIterator = iterator {
        yieldAll(designation.path)
        yield(designation.target)
    }

    /**
     * 返回下一个应访问的 designation 元素；没有剩余元素时返回 `null`。
     */
    override fun invoke(): CfirElement? = if (targetIterator.hasNext()) targetIterator.next() else null
}

/**
 * 实际遍历 CFIR 树并记录 tower context 的 visitor。
 */
private class ContextCollectorVisitor(
    /**
     * 文件会话与 scope session holder。
     */
    private val bodyHolder: SessionAndScopeSessionHolder,
    /**
     * 是否收集 BODY 上下文。
     */
    private val shouldCollectBodyContext: Boolean,
    /**
     * 是否允许访问过程中触发 BODY_RESOLVE。
     */
    private val shouldTriggerBodyAnalysis: Boolean,
    /**
     * PSI 候选过滤器。
     */
    private val filter: (PsiElement) -> FilterResponse,
    /**
     * 可选 designation 路径拦截器。
     */
    private val designationPathInterceptor: DesignationInterceptor?,
) : CfirDefaultVisitorVoid() {
    /**
     * 上下文结果表的键。
     */
    private data class ContextKey(val element: PsiElement, val kind: ContextKind)

    /**
     * 已收集的上下文结果。
     */
    private val result = HashMap<ContextKey, Context>()
    /**
     * 当前访问路径中的 CFIR 父元素栈。
     */
    private val parents = ArrayList<CfirElement>()

    /**
     * 当前遍历使用的 body resolve context。
     */
    private val context = BodyResolveContext(
        returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
        dataFlowAnalyzerContext = CfirDataFlowAnalyzerContext(),
        isContextCollectorMode = true,
    )

    /**
     * 标记 visitor 是否仍需要继续收集。
     */
    private var isActive = true

    /**
     * 从 [file] 根开始收集上下文。
     */
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

    /**
     * 查询 [element] 与 [kind] 对应的上下文快照。
     */
    operator fun get(element: PsiElement, kind: ContextKind): Context? {
        return result[ContextKey(element, kind)]
    }

    /**
     * 返回 [declaration] 所属会话对应的 holder。
     */
    private fun getSessionHolder(declaration: CfirDeclaration): SessionAndScopeSessionHolder {
        return when (val session = declaration.moduleData.session) {
            bodyHolder.session -> bodyHolder
            else -> SessionHolderImpl(session, bodyHolder.scopeSession)
        }
    }

    /**
     * 默认元素访问：记录 SELF/BODY 上下文并继续访问子元素。
     */
    override fun visitElement(element: CfirElement) {
        dumpContext(element, ContextKind.SELF)

        withParent(element) {
            dumpContext(element, ContextKind.BODY)
            element.acceptChildren(this)
        }
    }

    /**
     * 访问文件并收集包、导入、注解和文件 body 上下文。
     */
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

    /**
     * 访问 code fragment 并在其专用上下文中收集 block 上下文。
     */
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

    /**
     * 访问 class 声明。
     */
    override fun visitClass(klass: CfirClass) {
        handleClassLikeDeclaration(klass)
    }

    /**
     * 访问 interface 声明。
     */
    override fun visitInterface(`interface`: CfirInterface) {
        handleClassLikeDeclaration(`interface`)
    }

    /**
     * 访问 struct 声明。
     */
    override fun visitStruct(struct: CfirStruct) {
        handleClassLikeDeclaration(struct)
    }

    /**
     * 访问 enum 声明。
     */
    override fun visitEnum(enum: CfirEnum) {
        handleClassLikeDeclaration(enum)
    }

    /**
     * 处理 class-like 声明的头部和 body 上下文。
     */
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

    /**
     * 访问 extend 声明并收集其扩展类型、父类型和成员上下文。
     */
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

    /**
     * 访问 typealias 声明并收集展开类型上下文。
     */
    override fun visitTypeAlias(typeAlias: CfirTypeAlias) = withProcessor(typeAlias) {
        dumpContext(typeAlias, ContextKind.SELF)
        processAnnotations(typeAlias)

        context.withTypeParametersCompat(typeAlias) {
            processList(typeAlias.typeParameters)
            process(typeAlias.expandedTypeRef)
        }

        dumpContext(typeAlias, ContextKind.BODY)
    }

    /**
     * 访问构造器并收集参数、body 和构造器局部作用域上下文。
     */
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

    /**
     * 访问具名函数。
     */
    override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
        visitFunctionLike(namedFunction)
    }

    /**
     * 访问 main 函数。
     */
    override fun visitMainFunction(mainFunction: CfirMainFunction) {
        visitFunctionLike(mainFunction)
    }

    /**
     * 访问宏声明。
     */
    override fun visitMacroDeclaration(macroDeclaration: CfirMacroDeclaration) {
        visitFunctionLike(macroDeclaration)
    }

    /**
     * 访问 finalizer 声明。
     */
    override fun visitFinalizer(finalizer: CfirFinalizer) {
        visitFunctionLike(finalizer)
    }

    /**
     * 访问属性访问器并收集参数和 body 上下文。
     */
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

    /**
     * 处理函数类声明的通用上下文收集流程。
     */
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

    /**
     * 访问属性并收集 getter/setter 与属性 body 上下文。
     */
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

    /**
     * 访问字段变量。
     */
    override fun visitFieldVariable(fieldVariable: CfirFieldVariable) {
        visitVariableLike(fieldVariable, fieldVariable.symbol, fieldVariable.initializer)
    }

    /**
     * 访问 pattern 变量并收集 pattern binding 上下文。
     */
    override fun visitPatternVariable(patternVariable: CfirPatternVariable) {
        withProcessor(patternVariable) {
            dumpContext(patternVariable, ContextKind.SELF)
            process(patternVariable.returnTypeRef)

            onActive {
                context.withLocalVariableBodyCompat {
                    dumpContext(patternVariable, ContextKind.BODY)
                    process(patternVariable.initializer)
                    // PatternVariable 只是模式声明容器，真正进入局部作用域的是 pattern 内的 binding variable。
                    process(patternVariable.pattern)
                }
            }
        }
    }

    /**
     * 访问 pattern binding 变量。
     */
    override fun visitPatternBindingVariable(patternBindingVariable: CfirPatternBindingVariable) {
        visitVariableLike(patternBindingVariable, patternBindingVariable.symbol, patternBindingVariable.initializer)
    }

    /**
     * 处理局部变量、字段变量和 pattern binding 变量的通用上下文收集流程。
     */
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

        if (variable is org.cangnova.cangjie.cfir.declarations.CfirVariable) {
            context.storeVariable(variable, variable.moduleData.session)
        }
    }

    /**
     * 访问值参数并收集默认值上下文。
     */
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

    /**
     * 访问匿名函数并收集参数与 body 上下文。
     */
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

    /**
     * 访问 block 表达式。
     */
    override fun visitBlock(block: org.cangnova.cangjie.cfir.expressions.CfirBlock) {
        doVisitBlock(block)
    }

    /**
     * 收集 [block] 的 SELF/BODY 上下文。
     */
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

    /**
     * 处理 block 子元素并在结束时记录 BODY 上下文。
     */
    private fun Processor.processBlockBody(block: org.cangnova.cangjie.cfir.expressions.CfirBlock) {
        processChildren(block, checkIsActive = false)
        dumpContext(block, ContextKind.BODY)
    }

    /**
     * 为 [element] 当前 [kind] 位置保存 tower context 快照。
     */
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

    /**
     * 处理声明 [body]，并在存在局部 body 快照时只回放已分析语句。
     */
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

    /**
     * 在声明容器上下文中处理 [declaration] 的注解。
     */
    private fun Processor.processAnnotations(declaration: CfirDeclaration) {
        @OptIn(PrivateForInline::class)
        context.withContainer(declaration) {
            for (annotation in declaration.annotations) {
                process(annotation)
            }
        }
    }

    /**
     * 创建 [parent] 父栈上下文下的 [Processor] 并执行 [block]。
     */
    private inline fun withProcessor(parent: CfirElement, block: Processor.() -> Unit) {
        withParent(parent) {
            Processor(this).block()
        }
    }

    /**
     * 受控处理 CFIR 元素的辅助对象。
     */
    private inner class Processor(private val delegate: CfirVisitorVoid) {
        /**
         * 已由显式 process 调用处理过、后续子遍历应跳过的元素集合。
         */
        private val elementsToSkip = HashSet<CfirElement>()

        /**
         * 处理单个可空 [element]。
         */
        fun process(element: CfirElement?) {
            if (isActive && element != null) {
                element.accept(delegate)
                elementsToSkip += element
            }
        }

        /**
         * 顺序处理 [elements]，收集停止后立即退出。
         */
        fun processList(elements: Collection<CfirElement>) {
            for (element in elements) {
                if (!isActive) {
                    break
                }

                process(element)
            }
        }

        /**
         * 处理 [element] 的子元素，并跳过已经显式处理过的元素。
         */
        fun processChildren(element: CfirElement, checkIsActive: Boolean = true) {
            if (checkIsActive && !isActive) {
                return
            }

            val visitor = FilteringVisitor(delegate, elementsToSkip, checkIsActive)
            element.acceptChildren(visitor)
        }
    }

    /**
     * 跳过指定元素集合的 visitor 包装器。
     */
    private inner class FilteringVisitor(
        /**
         * 真正执行访问的委托 visitor。
         */
        private val delegate: CfirVisitorVoid,
        /**
         * 不再重复访问的元素集合。
         */
        private val elementsToSkip: Set<CfirElement>,
        /**
         * 是否在每次访问前检查 collector 是否仍处于活动状态。
         */
        private val checkIsActive: Boolean,
    ) : CfirVisitorVoid() {
        /**
         * 访问单个 [element]，必要时转交给 [delegate]。
         */
        override fun visitElement(element: CfirElement) {
            if (checkIsActive && !isActive) {
                return
            }

            if (element !in elementsToSkip) {
                element.accept(delegate)
            }
        }
    }

    /**
     * 如有需要，触发 [CfirDeclaration] 的 BODY_RESOLVE。
     */
    private fun CfirDeclaration.performBodyAnalysis() {
        if (!shouldTriggerBodyAnalysis && partialBodyAnalysisState != null) {
            return
        }

        lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
    }

    /**
     * 按 designation 拦截器访问下一个目标，否则执行 [block]。
     */
    private fun withInterceptor(block: () -> Unit) {
        val target = designationPathInterceptor?.invoke()
        if (target != null) {
            target.accept(this)
        } else {
            block()
        }
    }

    /**
     * 在 [parent] 入栈期间执行 [block]。
     */
    private inline fun withParent(parent: CfirElement, block: () -> Unit) {
        parents.add(parent)
        try {
            block()
        } finally {
            parents.removeLast()
        }
    }

    /**
     * 仅在 collector 仍处于活动状态时执行 [block]。
     */
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

/**
 * 在 class-like body 上下文中执行 [block]。
 */
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

/**
 * 在构造器头部上下文中执行 [block]。
 */
private inline fun BodyResolveContext.withConstructorCompat(
    constructor: CfirConstructor,
    holder: SessionAndScopeSessionHolder,
    block: () -> Unit,
) {
    withTowerDataMode(org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirTowerDataMode.CONSTRUCTOR_HEADER) {
        withContainer(constructor) {
            withTowerDataCleanup {
                addLocalScope(CfirLocalScope(holder.session))
                for (valueParameter in constructor.valueParameters) {
                    storeValueParameterIfNeeded(valueParameter, holder.session)
                }
                block()
            }
        }
    }
}

/**
 * 在构造器 body 局部作用域中执行 [block]。
 */
private inline fun BodyResolveContext.forConstructorBodyCompat(
    constructor: CfirConstructor,
    block: () -> Unit,
) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScope(constructor.moduleData.session))
        for (valueParameter in constructor.valueParameters) {
            storeValueParameterIfNeeded(valueParameter, constructor.moduleData.session)
        }
        block()
    }
}

/**
 * 在函数声明头部上下文中执行 [block]。
 */
private inline fun BodyResolveContext.withFunctionDeclarationCompat(
    function: CfirFunction,
    block: () -> Unit,
) {
    if (containerIfAny !is CfirClassLikeDeclaration && containerIfAny !is CfirExtend && function is CfirNamedFunction) {
        val symbol = function.symbol as? CfirFunctionSymbol<*>
        if (symbol != null) {
            storeFunction(function, function.moduleData.session)
        }
    }

    withTypeParametersCompat(function) {
        withContainer(function, block)
    }
}

/**
 * 在函数 body 局部作用域中执行 [block]。
 */
private inline fun BodyResolveContext.forFunctionBodyCompat(
    function: CfirFunction,
    block: () -> Unit,
) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScope(function.moduleData.session))
        for (valueParameter in function.valueParameters) {
            storeValueParameterIfNeeded(valueParameter, function.moduleData.session)
        }
        block()
    }
}

/**
 * 在属性声明上下文中执行 [block]。
 */
private inline fun BodyResolveContext.withPropertyCompat(
    property: CfirProperty,
    block: () -> Unit,
) {
    withTypeParametersCompat(property) {
        withContainer(property, block)
    }
}

/**
 * 在属性访问器局部作用域中执行 [block]。
 */
private inline fun BodyResolveContext.withPropertyAccessorCompat(
    propertyAccessor: CfirPropertyAccessor,
    holder: SessionAndScopeSessionHolder,
    block: () -> Unit,
) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScope(propertyAccessor.moduleData.session))
        for (valueParameter in propertyAccessor.valueParameters) {
            storeValueParameterIfNeeded(valueParameter, propertyAccessor.moduleData.session)
        }

        withPublicApiInlineFunction(propertyAccessor) {
            withContainer(propertyAccessor, block)
        }
    }
}

/**
 * 在值参数上下文中执行 [block]。
 */
private inline fun BodyResolveContext.withValueParameterCompat(
    valueParameter: CfirValueParameter,
    block: () -> Unit,
) {
    storeValueParameterIfNeeded(valueParameter, valueParameter.moduleData.session)
    withContainer(valueParameter, block)
}

/**
 * 在匿名函数上下文中执行 [block]。
 */
private inline fun BodyResolveContext.withAnonymousFunctionCompat(
    anonymousFunction: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction,
    block: () -> Unit,
) {
    withTypeParametersCompat(anonymousFunction) {
        withTowerDataCleanup {
            addLocalScope(CfirLocalScope(anonymousFunction.moduleData.session))
            withContainer(anonymousFunction) {
                for (valueParameter in anonymousFunction.valueParameters) {
                    storeValueParameterIfNeeded(valueParameter, anonymousFunction.moduleData.session)
                }
                block()
            }
        }
    }
}

/**
 * 在普通 block 局部作用域中执行 [block]。
 */
private inline fun BodyResolveContext.withBlockScopeCompat(block: () -> Unit) {
    withTowerDataCleanup {
        addLocalScope(CfirLocalScope(file.moduleData.session))
        block()
    }
}

/**
 * 在局部变量 body 上下文中执行 [block]。
 */
private inline fun BodyResolveContext.withLocalVariableBodyCompat(block: () -> Unit) {
    withTowerDataCleanup(block)
}

/**
 * 在 code fragment 上下文中执行 [block]。
 */
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
