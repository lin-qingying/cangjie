package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.semantics.ConeCyclicTypeBound

/** 描述类型解析阶段已经识别并从类型系统上界图中切断的递归根因。 */
internal sealed class CfirGenericUpperBoundRecursionIssue {
    abstract val parameter: CfirTypeParameter

    /** 上界链里出现泛型参数环，对应 `sema_generic_param_directly_recursive`。 */
    data class Direct(
        override val parameter: CfirTypeParameter,
        val recursiveWith: CfirTypeParameterSymbol,
    ) : CfirGenericUpperBoundRecursionIssue()

    /** 非 class/interface 上界的类型实参递归引用当前参数。 */
    data class ClassIrrelevant(
        override val parameter: CfirTypeParameter,
        val upperBound: ConeCangJieType,
    ) : CfirGenericUpperBoundRecursionIssue()
}

/** 从结构化 error-bound 载荷读取 owner 级递归根因，供语义 checker 统一消费。 */
internal fun CfirTypeParameter.findFirstGenericUpperBoundRecursionIssueInOwner(): CfirGenericUpperBoundRecursionIssue? {
    val owner = containingDeclarationSymbol.cfir as? CfirTypeParameterRefsOwner ?: return null
    for (typeParameter in owner.typeParameters) {
        val parameter = typeParameter as? CfirTypeParameter ?: continue
        for (bound in parameter.bounds) {
            val marker = (bound as? CfirErrorTypeRef)?.diagnostic as? ConeCyclicTypeBound ?: continue
            val diagnosedParameter = marker.issue.parameterSymbol.cfir as? CfirTypeParameter ?: continue
            return when (val issue = marker.issue) {
                is org.cangnova.cangjie.cfir.semantics.CfirGenericUpperBoundRecursionIssue.Direct ->
                    CfirGenericUpperBoundRecursionIssue.Direct(diagnosedParameter, issue.recursiveWith)
                is org.cangnova.cangjie.cfir.semantics.CfirGenericUpperBoundRecursionIssue.ClassIrrelevant ->
                    CfirGenericUpperBoundRecursionIssue.ClassIrrelevant(diagnosedParameter, issue.upperBound)
            }
        }
    }
    return null
}
