@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.body

import com.intellij.openapi.diagnostic.ControlFlowException
import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.DeepCopyCfirTreeWithSymbols
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.hasUncertainExpectedTypeCompatibilityShape
import org.cangnova.cangjie.cfir.resolve.calls.isClosedOperatorInputType
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceSession
import org.cangnova.cangjie.cfir.resolve.inference.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.firstErrorTypeOrNull
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.scopes.deferredCallableCopyReturnType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/**
 * body transformer 持有的首次 RAW 调用模板，覆盖完成与目标类型重入。
 * 每个目标重新取得当前词法入口及父系统快照，构造独立树、候选、CS 和 FULL writer。
 * 源声明依赖由其真实 owner 在试算环境退出后准备，试算仅消费只读返回类型事实。
 */
internal class CfirIndependentCallTrial private constructor(
    private val template: CfirExpression,
    private val session: CfirSession,
) {
    /** 按需准备真实源签名后重新执行独立目标检查，每个声明身份只请求一次。 */
    fun resolveType(
        resolutionMode: ResolutionMode,
        components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
        expressionContext: (CfirExpressionsResolveTransformer) -> Unit,
    ): ConeCangJieType? {
        val returnTypes = IdentityHashMap<CfirCallableDeclaration, CfirResolvedTypeRef>()
        while (true) {
            try {
                return resolveWithPreparedSignatures(resolutionMode, components, expressionContext, returnTypes)
            } catch (dependency: SignatureDependencyRequired) {
                check(!returnTypes.containsKey(dependency.declaration)) {
                    "An independent trial requested an already prepared source signature."
                }
                returnTypes[dependency.declaration] = components.returnTypeCalculator
                    .tryCalculateReturnType(dependency.declaration)
            }
        }
    }

    /** 本次环境独立持有全部可变完成状态，源签名请求也必须先退出该环境。 */
    private fun resolveWithPreparedSignatures(
        resolutionMode: ResolutionMode,
        components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
        expressionContext: (CfirExpressionsResolveTransformer) -> Unit,
        returnTypes: Map<CfirCallableDeclaration, CfirResolvedTypeRef>,
    ): ConeCangJieType? {
        val originalContext = components.context
        val parent = (originalContext.inferenceSession as? CfirPCLAInferenceSession)?.currentCommonSystem
            ?: originalContext.implicitTypeComputationScope?.graph?.constraintSystemOwner
        val anchor = session.inferenceComponents.createConstraintSystem().apply {
            parent?.captureStorage()?.let(::setBaseSystem)
            prepareForGlobalCompletion()
        }
        val baseStorage = anchor.captureStorage()
        val context = originalContext.createLocalDeclarationContext(ReturnTypeCalculator.Default)
        context.implicitTypeComputationScope = null
        context.inferenceSession = object : CfirInferenceSession() {
            override fun baseConstraintStorageForCandidate(
                candidate: Candidate,
                bodyResolveContext: BodyResolveContext,
            ): ConstraintStorage = baseStorage

            override fun <T> processPartiallyResolvedCall(
                call: T,
                resolutionMode: ResolutionMode,
                completionMode: ConstraintSystemCompletionMode,
            ) where T : CfirResolvable, T : CfirExpression {
                // 普通调用树的 partial atom 由本次根调用的正常 FULL 完成器消费。
            }
        }
        val copyContext = createRawExpressionCopyContext(template, context, baseStorage.allTypeVariables.keys)
        val root = DeepCopyCfirTreeWithSymbols(copyContext).copyRoot(template)
        val ownedDeclarations = Collections.newSetFromMap(
            IdentityHashMap<CfirCallableDeclaration, Boolean>(),
        ).apply {
            addAll(copyContext.state.elementMapping().values.filterIsInstance<CfirCallableDeclaration>())
        }
        val returnTypeCalculator = TrialReturnTypeCalculator(IdentityHashMap(returnTypes), ownedDeclarations)
        context.returnTypeCalculator = returnTypeCalculator
        val transformer = CfirBodyResolveTransformer(session, ScopeSession(), returnTypeCalculator, context)
        expressionContext(transformer.expressionsTransformer)
        return context.dataFlowAnalyzerContext.withIsolatedContext {
            val resolved = root.transformSingle(transformer, resolutionMode)
            if (resolved.firstErrorTypeOrNull(session) != null) return@withIsolatedContext null
            resolved.coneTypeOrNull?.takeIf {
                it.isClosedOperatorInputType() && !it.hasUncertainExpectedTypeCompatibilityShape()
            }
        }
    }

    /** 标准控制流异常使签名依赖穿过 whileAnalysing，真实编译异常继续原样传播。 */
    private class SignatureDependencyRequired(
        val declaration: CfirCallableDeclaration,
    ) : RuntimeException(null, null, false, false), ControlFlowException

    /** 借用声明只能读取事实，拥有的 RAW 声明由本次正常 body resolve 完成。 */
    private class TrialReturnTypeCalculator(
        private val returnTypes: Map<CfirCallableDeclaration, CfirResolvedTypeRef>,
        private val ownedDeclarations: Set<CfirCallableDeclaration>,
    ) : ReturnTypeCalculator() {
        override val callableCopyTypeCalculator: CallableCopyTypeCalculator = object : CallableCopyTypeCalculator() {
            override fun computeReturnType(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? =
                readReturnType(declaration)
        }

        override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? =
            readReturnType(declaration)

        private fun readReturnType(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? {
            returnTypes[declaration]?.let { return it }
            (declaration.returnTypeRef as? CfirResolvedTypeRef)?.let { return it }
            declaration.attributes.deferredCallableCopyReturnType?.let { deferred ->
                val type = deferred.computeReturnType(callableCopyTypeCalculator) ?: return null
                return declaration.returnTypeRef.resolvedTypeFromPrototype(
                    type, declaration.source?.fakeElement(CjFakeSourceElementKind.ImplicitTypeRef),
                )
            }
            if (declaration in ownedDeclarations) {
                return ReturnTypeCalculator.Default.tryCalculateReturnTypeOrNull(declaration)
            }
            throw SignatureDependencyRequired(declaration)
        }
    }

    companion object {
        /** 首次变换前复制 RAW 输入；缓存中不保存任何旧候选、父 CS 或词法上下文。 */
        fun prepare(
            expression: CfirExpression,
            components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
        ): CfirIndependentCallTrial {
            val context = components.context
            val parent = (context.inferenceSession as? CfirPCLAInferenceSession)?.currentCommonSystem
                ?: context.implicitTypeComputationScope?.graph?.constraintSystemOwner
            val incoming = parent?.captureStorage()?.allTypeVariables?.keys.orEmpty()
            val copyContext = createRawExpressionCopyContext(expression, context, incoming)
            val template = DeepCopyCfirTreeWithSymbols(copyContext).copyRoot(expression)
            return CfirIndependentCallTrial(template, components.session)
        }
    }
}
