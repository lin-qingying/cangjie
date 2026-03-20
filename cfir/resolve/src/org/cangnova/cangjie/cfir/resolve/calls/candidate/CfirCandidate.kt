package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintSystem
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

/**
 * 调用解析候选，面向 Phase 3。
 * 它封装一个候选符号以及该候选在验证管线中的状态，包括：
 * - 参数映射关系
 * - 使用到的默认值参数个数
 * - 类型参数替换器
 * - 适用性等级和诊断信息
 * 对齐 K2 `Candidate`，但去掉 postponed atoms、SAM 转换等 Kotlin 特有状态。
 */
class CfirCandidate(
    /** 候选符号。 */
    val symbol: CfirCallableSymbol<*>,
    /** 关联的调用信息。 */
    val callInfo: CfirCallInfo,
    /** 候选来源的 scope，可用于重载消歧中的 override 过滤。 */
    val originScope: CfirScope? = null,
) {
    /** 实参到形参的映射，由 `MapArguments` 阶段填充。 */
    var argumentMapping: Map<Int, Int> = emptyMap()

    /** 使用到的默认值参数个数。 */
    var numDefaults: Int = 0

    /** 类型参数替换器。 */
    var substitutor: CfirTypeSubstitutor = CfirTypeSubstitutor.Empty

    /** 约束系统，泛型推断时使用。 */
    var constraintSystem: CfirConstraintSystem? = null

    /** 当前最低适用性等级，在各验证阶段中不断下调。 */
    var lowestApplicability: CfirCandidateApplicability = CfirCandidateApplicability.RESOLVED

    /** 候选上的诊断信息列表。 */
    val diagnostics: MutableList<CfirResolutionDiagnostic> = mutableListOf()

    /** 添加诊断，并同步更新 `lowestApplicability`。 */
    fun addDiagnostic(diagnostic: CfirResolutionDiagnostic) {
        diagnostics.add(diagnostic)
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    /** 当前候选是否仍可视为成功。 */
    val isSuccessful: Boolean
        get() = lowestApplicability.isSuccess

    /**
     * 读取候选在应用类型替换之后的返回类型。
     * 它会先从符号绑定的声明里提取返回类型，再通过 `substitutor`
     * 替换其中的类型参数。
     */
    fun resolvedReturnType(): ConeCangjieType? {
        if (!symbol.isBound) return null
        val decl = symbol.cfir
        val typeRef = when (decl) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirConstructor -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> {
                val enumSymbol = decl.symbol as? org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
                    ?: return null
                val classId = callInfo.session.symbolProvider.getEnumConstructorOwnerClassId(enumSymbol)
                    ?: callInfo.session.cfirProvider.getEnumConstructorOwnerClassId(enumSymbol)
                    ?: return null
                val typeArgs = decl.typeParameters.map {
                    ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
                }
                return substitutor.substituteOrSelf(ConeEnumType(ConeClassLookupTagImpl(classId), typeArgs))
            }
            else -> return null
        }
        val coneType = (typeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)?.coneType
            ?: return null
        return substitutor.substituteOrSelf(coneType)
    }
}

