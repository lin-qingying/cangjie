package org.cangnova.cangjie.cfir.semantics

import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * 已解析类型参数上界中的递归根因，供类型解析切断循环、声明检查器报告语义诊断。
 */
sealed interface CfirGenericUpperBoundRecursionIssue {
    /** 产生递归上界诊断的类型参数。 */
    val parameterSymbol: CfirTypeParameterSymbol

    /** 上界沿类型参数直接引用形成递归环。 */
    data class Direct(
        override val parameterSymbol: CfirTypeParameterSymbol,
        val recursiveWith: CfirTypeParameterSymbol,
    ) : CfirGenericUpperBoundRecursionIssue

    /** 非 class-like 上界的组成类型通过泛型参数上界再次引用当前参数。 */
    data class ClassIrrelevant(
        override val parameterSymbol: CfirTypeParameterSymbol,
        val upperBound: ConeCangJieType,
    ) : CfirGenericUpperBoundRecursionIssue
}

/**
 * 类型参数循环上界的内部错误标记。
 *
 * [originalBounds] 保留切断前的已解析上界，供语义诊断与错误恢复读取；类型系统只看见
 * 当前错误上界，不会再次沿原始引用展开循环。
 */
class ConeCyclicTypeBound(
    val symbol: CfirTypeParameterSymbol,
    originalBounds: List<CfirTypeRef>,
    val issue: CfirGenericUpperBoundRecursionIssue,
) : ConeDiagnostic {
    /** 切断前的完整已解析上界快照。 */
    val originalBounds: List<CfirTypeRef> = originalBounds.toPersistentList()

    override val reason: String
        get() = "Type parameter ${symbol.name} has cyclic upper bounds"
}
