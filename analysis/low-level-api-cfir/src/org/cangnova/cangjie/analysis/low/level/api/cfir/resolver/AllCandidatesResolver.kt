

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolver

import com.intellij.openapi.diagnostic.logger
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector
import org.cangnova.cangjie.analysis.utils.printer.parentsOfType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunctionCopy
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidate
import org.cangnova.cangjie.cfir.resolve.body.AllCandidatesCollector
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.body.CfirExpressionsResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerResolver
import org.cangnova.cangjie.cfir.resolve.body.OverloadCandidate
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.stages.fullyProcessCandidate
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.util.PrivateForInline
import org.cangnova.cangjie.utils.exceptions.logErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 所有重载候选解析器。
 * 负责收集并后处理某次调用的全部重载候选项，用于 IDE 的重载提示等场景。
 */
class AllCandidatesResolver(private val firSession: CfirSession) {
    /**
     * 候选收集期间使用的作用域会话。
     */
    private val scopeSession = ScopeSession()

    // 该 transformer 仅用于访问解析组件，不实际参与树的变换
    /**
     * 用于构造 body resolve 组件的占位 transformer。
     *
     * 该 transformer 不直接变换 CFIR 树，只提供 resolver、context 和 call completer 等组件入口。
     */
    private val stubBodyResolveTransformer = CfirBodyResolveTransformer(
        session = firSession,
        phase = CfirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    /**
     * 用于收集全部候选的 body resolve 组件集合。
     *
     * 这里替换 call resolver，使调用解析阶段保留所有 overload candidate，而不是只保留最终选择结果。
     */
    private val bodyResolveComponents = object : StubBodyResolveTransformerComponents(
        firSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    ) {
        // 用于收集全部候选项的收集器
        val collector = AllCandidatesCollector(this, resolutionStageRunner)
        // Tower 解析器，负责在作用域链中查找候选项
        val towerResolver = CfirTowerResolver(this, resolutionStageRunner, collector)
        override val callResolver = CfirCallResolver(this, towerResolver)

        init {
            callResolver.initTransformer(CfirExpressionsResolveTransformer(stubBodyResolveTransformer))
        }
    }

    /**
     * 候选后处理阶段使用的解析上下文。
     */
    private val resolutionContext = ResolutionContext(firSession, bodyResolveComponents, bodyResolveComponents.transformer.context)

    /**
     * 获取指定调用表达式的所有重载候选项。
     *
     * @param resolutionFacade 解析门面，提供文件级别的解析能力
     * @param qualifiedAccess  待解析的限定访问表达式（函数调用或属性访问）
     * @param calleeName       被调用符号的名称
     * @param element          对应的 PSI 元素，用于定位上下文
     * @param resolutionMode   解析模式
     * @return 所有匹配的重载候选列表
     */
    fun getAllCandidates(
        resolutionFacade: LLResolutionFacade,
        qualifiedAccess: CfirQualifiedAccessExpression,
        calleeName: Name,
        element: CjElement,
        resolutionMode: ResolutionMode,
    ): List<OverloadCandidate> {
        // 初始化函数体解析上下文（作用域、容器声明等）
        initializeBodyResolveContext(resolutionFacade, element)

        // 复制调用表达式，避免对原始树造成副作用
        val copiedAccess = copyQualifiedAccess(qualifiedAccess, element) ?: return emptyList()
        return run {
            bodyResolveComponents.callResolver
                .collectAllCandidates(
                    copiedAccess,
                    calleeName,
                    bodyResolveComponents.context.containers,
                    resolutionContext,
                    resolutionMode,
                )
                .apply { postProcessCandidates(copiedAccess) }
        }
    }

    /**
     * 初始化函数体解析上下文。
     * 通过 [ContextCollector] 获取 Tower 数据上下文，并填充容器声明列表。
     */
    @OptIn(PrivateForInline::class)
    private fun initializeBodyResolveContext(resolutionFacade: LLResolutionFacade, element: CjElement) {
        val firFile = element.containingCjFile.getOrBuildCfirFile(resolutionFacade)

        // 获取元素所在位置的 Tower 数据上下文（包含所有可见作用域）
        val towerContext = ContextCollector.process(resolutionFacade, firFile, element)?.towerDataContext
        towerContext?.let { bodyResolveComponents.context.replaceTowerDataContext(it) }

        // 收集从外到内的所有包含声明（类、函数等），并按从外到内的顺序放入容器
        val containingDeclarations =
            element.parentsOfType<CjDeclaration>().map { declaration: CjDeclaration ->
                declaration.resolveToCfirSymbol(resolutionFacade).cfir
            }.toList().asReversed()
        bodyResolveComponents.context.containers.addAll(containingDeclarations)

        // towerContext 已包含文件级别的所有作用域，直接赋值而无需调用 withFile
        bodyResolveComponents.context.file = firFile
    }

    /**
     * 对所有候选项进行后处理：
     * 1. 运行解析阶段以初始化类型约束；
     * 2. 运行约束系统补全（仅处理到第一个 Lambda，避免修改原始树）；
     * 3. 将被调用方的不适用性传播到候选项自身。
     */
    @OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
    private fun <T> List<OverloadCandidate>.postProcessCandidates(call: T) where T : CfirExpression, T : CfirResolvable {
        val callCompleter = bodyResolveComponents.callCompleter
        val analyzer = callCompleter.createPostponedArgumentsAnalyzer(resolutionContext)
        val components = resolutionContext.bodyResolveComponents

        forEach { overloadCandidate ->
            val candidate = overloadCandidate.candidate

            // 运行解析阶段，触发类型约束的初始化
            components.resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)

            // 运行约束系统补全。
            // Lambda 的处理逻辑会修改原始语法树，因此此处仅补全到第一个 Lambda 为止。
            callCompleter.runCompletionForCall(
                candidate = candidate,
                completionMode = ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA,
                call = call,
                initialType = components.initialTypeOfCandidate(candidate),
                analyzer = analyzer,
            )

            // 将被调用方的不适用性信息传播到候选项
            overloadCandidate.preserveCalleeInapplicability()
        }
    }

    /**
     * 将被调用方（callee）的不适用性传播到候选项自身。
     *
     * 问题背景：[getAllCandidates] 在构建 firFile 时，对于不适用的调用（如缺少类型参数），
     * 会将缺失的类型参数推断为错误类型，导致后续 [CandidateFactory] 产生"表面上适用"
     * 但实际不适用的候选项。此方法通过检查 callee 引用上的诊断信息来补正这一问题。
     */
    private fun OverloadCandidate.preserveCalleeInapplicability() {
        val callSite = candidate.callInfo.callSite
        val calleeReference = callSite.toReference(firSession) as? CfirDiagnosticHolder ?: return
        val diagnostic = calleeReference.diagnostic as? ConeInapplicableCandidateError ?: return
        if (diagnostic.applicability != CandidateApplicability.INAPPLICABLE) return

        candidate.addDiagnostic(InapplicableCandidate)
    }
}

/**
 * 复制限定访问表达式，以防止对原始语法树的修改。
 *
 * [CfirOverloadByLambdaReturnTypeResolver] 在处理过程中可能会修改传入的表达式树
 * （包括 callee 引用和 Lambda 参数），因此需要先进行浅拷贝。
 * 此处无需进行完整的深拷贝，仅覆盖已知会被修改的部分即可。
 */
private fun copyQualifiedAccess(
    qualifiedAccess: CfirQualifiedAccessExpression,
    element: CjElement,
): CfirQualifiedAccessExpression? = when (qualifiedAccess) {
    is CfirFunctionCall -> buildFunctionCallCopy(qualifiedAccess) {
        argumentList = when (val argumentListToCopy = qualifiedAccess.argumentList) {
            is CfirEmptyArgumentList -> argumentListToCopy
            is CfirResolvedArgumentList -> {
                // 复制参数映射表的键（即各参数表达式）
                val newMapping = argumentListToCopy.mapping.mapKeysTo(LinkedHashMap()) { copyArgument(it.key) }

                // 同时复制原始参数列表，因为 CallInfo.arguments 会直接引用它
                val originalArgumentList = argumentListToCopy.originalArgumentList
                val newOriginalList = if (originalArgumentList != null) {
                    buildArgumentListCopy(originalArgumentList) {
                        arguments.replaceAll(::copyArgument)
                    }
                } else {
                    null
                }

                buildResolvedArgumentList(
                    original = newOriginalList,
                    mapping = newMapping,
                )
            }

            else -> {
                // 遇到未知的参数列表类型，记录错误日志并返回 null
                logger<AllCandidatesResolver>().logErrorWithAttachment("Unexpected argument list ${argumentListToCopy::class.simpleName}") {
                    withCfirEntry("argumentList", argumentListToCopy)
                    withPsiEntry("psi", element)
                }

                return null
            }
        }
    }
    is CfirNamedAccessExpression -> buildNamedAccessExpressionCopy(qualifiedAccess) {}
    else -> {
        // 遇到不支持的限定访问类型，记录错误日志并返回 null
        logger<AllCandidatesResolver>().logErrorWithAttachment("Unsupported qualified access ${qualifiedAccess::class.simpleName}") {
            withCfirEntry("qualifiedAccess", qualifiedAccess)
            withPsiEntry("psi", element)
        }

        null
    }
}

/**
 * 复制单个参数表达式。
 * 目前仅处理匿名函数表达式（[CfirAnonymousFunctionExpression]），
 * 为其生成新的函数符号，以避免符号共享导致的副作用。
 * 其他类型的参数直接返回原对象（不可变，无需复制）。
 */
private fun copyArgument(argument: CfirExpression): CfirExpression = when (argument) {
    is CfirAnonymousFunctionExpression -> {
        buildAnonymousFunctionExpressionCopy(argument) {
            anonymousFunction = buildAnonymousFunctionCopy(argument.anonymousFunction) {
                // 为拷贝的匿名函数分配新的符号，避免与原函数共享符号
                symbol = CfirAnonymousFunctionSymbol()
            }
        }
    }
    else -> argument
}
