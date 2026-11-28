package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.cangnova.cangjie.type.model.TypeVariableMarker

/** 保留创建裸枚举类型变量的候选，供诊断区分构造器缺参与外层调用自身的推断失败。 */
class BareEnumConstructorInsufficientInformation(
    typeVariable: TypeVariableMarker,
    resolvedAtom: CfirStatement,
    val origin: AbstractCallCandidate<*>,
) : NotEnoughInformationForTypeParameter<CfirStatement>(typeVariable, resolvedAtom, false)
