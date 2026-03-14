package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintSystem
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 调用解析候选（Phase 3 版本）。
 *
 * 封装一个候选符号及其在验证管线中的状态：
 * - 参数映射（实参→形参的对应关系）
 * - 使用的默认值参数数量
 * - 类型参数替换器
 * - 适用性等级和诊断信息
 *
 * 对齐 K2 Candidate，去掉约束系统、postponedAtoms、SAM 转换等。
 */
class CfirCandidate(
    /** 候选符号 */
    val symbol: CfirCallableSymbol<*>,
    /** 关联的调用信息 */
    val callInfo: CfirCallInfo,
    /** 候选来源的 scope（用于重载消歧中的 override 过滤） */
    val originScope: CfirScope? = null,
) {
    /** 实参→形参映射（由 MapArguments 阶段填充） */
    var argumentMapping: Map<Int, Int> = emptyMap()

    /** 使用的默认值参数数量 */
    var numDefaults: Int = 0

    /** 类型参数替换器（显式类型参数的替换） */
    var substitutor: CfirTypeSubstitutor = CfirTypeSubstitutor.Empty

    /** 约束系统（泛型推断时使用，Phase 4） */
    var constraintSystem: CfirConstraintSystem? = null

    /** 当前最低适用性等级（验证阶段中取最差值） */
    var lowestApplicability: CfirCandidateApplicability = CfirCandidateApplicability.RESOLVED

    /** 诊断信息列表 */
    val diagnostics: MutableList<CfirResolutionDiagnostic> = mutableListOf()

    /** 添加诊断，同时更新 lowestApplicability */
    fun addDiagnostic(diagnostic: CfirResolutionDiagnostic) {
        diagnostics.add(diagnostic)
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    /** 是否为成功候选 */
    val isSuccessful: Boolean
        get() = lowestApplicability.isSuccess

    /**
     * 从候选符号中提取替换后的返回类型。
     *
     * 先从符号声明获取返回类型，再通过 substitutor 替换类型参数。
     */
    fun resolvedReturnType(): ConeCangjieType? {
        if (!symbol.isBound) return null
        val decl = symbol.cfir
        val typeRef = when (decl) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirConstructor -> decl.returnTypeRef
            else -> return null
        }
        val coneType = (typeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)?.coneType
            ?: return null
        return substitutor.substituteOrSelf(coneType)
    }
}
