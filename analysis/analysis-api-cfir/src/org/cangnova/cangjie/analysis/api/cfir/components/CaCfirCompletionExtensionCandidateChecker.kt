package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.completion.CaCompletionExtensionCandidateChecker
import org.cangnova.cangjie.analysis.api.completion.CaExtensionApplicabilityResult
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitutionForConstraintDerivation
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirGenericSubstitutor
import org.cangnova.cangjie.psi.CjElement

/**
 * 补全场景的 extend 适用性检查。
 *
 * 实现完全复用 CFIR providers 的 extend 匹配入口，不在 Analysis 层复制类型匹配规则：
 * 1. 先用**带约束**的 [createExtendDeclarationSubstitution] 判定是否成立；
 * 2. 目标类型可匹配但约束不满足时，再确认不是类型模式不匹配，从而给出精确结论；
 * 3. 接收者或 extend 本身信息不足时返回 [CaExtensionApplicabilityResult.Unknown]。
 *
 * 绝不使用 [createExtendDeclarationSubstitutionForConstraintDerivation] 作为放行依据：
 * 该入口只供约束系统派生初始约束，用它判定候选会放过约束不成立的 extend。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class, CaPlatformInterface::class)
internal class CaCfirCompletionExtensionCandidateChecker(
    /**
     * 延迟取得当前 CFIR Analysis 会话，适用性判定复用其中的 extend provider 与类型构建器。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(),
    CaCompletionExtensionCandidateChecker,
    CaCfirSessionComponent {

    /**
     * 判断 extend 符号对给定接收者是否适用。
     */
    override fun checkExtensionApplicability(
        extendSymbol: CaSymbol,
        receiver: CaType,
        position: CjElement,
    ): CaExtensionApplicabilityResult = withValidityAssertion {
        val extend = (extendSymbol as? CaCfirExtendSymbol)?.cfirSymbol?.cfir
            ?: return@withValidityAssertion CaExtensionApplicabilityResult.Unknown(token)
        val extendDeclaration = extend as? CfirExtend
            ?: return@withValidityAssertion CaExtensionApplicabilityResult.Unknown(token)
        // extend 目标类型是惰性引用：不推进到 TYPES 阶段就读不到目标模式，
        // 任何匹配都会退化成 Unknown。这里直接推进 CFIR 元素，
        // 因此 provider 派生（可能无 PSI）的 extend 同样可用。
        extendDeclaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val receiverType = (receiver as? CaCfirType)?.coneType
            ?: return@withValidityAssertion CaExtensionApplicabilityResult.Unknown(token)
        if (receiverType.isUndetermined()) return@withValidityAssertion CaExtensionApplicabilityResult.Unknown(token)
        checkExtend(extendDeclaration, receiverType)
    }

    /**
     * 对确定的 extend 与接收者执行目标匹配与约束校验。
     */
    private fun checkExtend(
        extend: CfirExtend,
        receiverType: ConeCangJieType,
    ): CaExtensionApplicabilityResult {
        val session = analysisSession.cfirSession

        // 带约束入口是唯一可用于候选放行的路径：它在 receiver 的上界与直接父类型链上
        // 匹配目标模式，并校验 extend 自身的泛型约束。
        findExtendDeclarationSubstitution(session, extend, receiverType)?.let { applicable ->
            return CaExtensionApplicabilityResult.Applicable(applicable.toPublicSubstitutor())
        }

        // 失败分类需要目标模式；模式尚未解析时无法区分“不匹配”和“约束不成立”，只能判为未知。
        val targetPattern = extend.extendedTypeRef.coneTypeOrNull
            ?: return CaExtensionApplicabilityResult.Unknown(token)

        // 宽松入口只回答目标类型能否匹配，不回答 extend 约束。
        val targetMatches = createExtendDeclarationSubstitutionForConstraintDerivation(
            session,
            extend,
            targetPattern,
            receiverType,
        ) != null

        return if (targetMatches) {
            CaExtensionApplicabilityResult.ConstraintUnsatisfied(token)
        } else {
            CaExtensionApplicabilityResult.ReceiverMismatch(token)
        }
    }

    /**
     * 把 compiler 侧替换结果投影为公开 substitutor。
     *
     * 当前 compiler 替换结果不保证暴露类型参数映射，因此统一投影为通用替换器：
     * 它只提供 substitute 行为，调用方不能据此读回 extend 类型参数的实例化结果。
     */
    private fun CfirExtendDeclarationSubstitution.toPublicSubstitutor() =
        CaCfirGenericSubstitutor(substitutor, analysisSession.cfirSymbolBuilder)

    /**
     * 判断接收者类型是否处于未确定状态。
     *
     * 错误类型、问号类型和存根类型都不能作为 extend 匹配依据；
     * 它们既不是「不匹配」，也不是「匹配」，只能判为未知。
     */
    private fun ConeCangJieType.isUndetermined(): Boolean =
        this is ConeErrorType || this is ConeQuestType || this is ConeStubType
}