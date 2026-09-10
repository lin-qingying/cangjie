package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType

/** 元组模式的形状检查结果，供绑定解析和诊断共用。 */
sealed interface CfirTuplePatternShape {
    /** 类型仍待推断，或已有根错误，不产生新的形状结论。 */
    data object Unresolved : CfirTuplePatternShape
    /** 已知输入类型不是元组。 */
    data class NotTuple(val actualType: ConeCangJieType) : CfirTuplePatternShape
    /** 已知输入元组与模式的元数不同。 */
    data class WrongSize(val tupleType: ConeTupleType) : CfirTuplePatternShape
    /** 形状匹配，可以按位置投影子模式类型。 */
    data class Matched(val tupleType: ConeTupleType) : CfirTuplePatternShape
}

/** 展开类型别名后判定元组形状；推断占位不能提前当作非元组。 */
fun CfirTuplePattern.resolveTupleShape(expectedType: ConeCangJieType?, session: CfirSession): CfirTuplePatternShape {
    val type = expectedType?.fullyExpandedType(session) ?: return CfirTuplePatternShape.Unresolved
    if (type is ConeErrorType || type is ConeTypeVariableType) return CfirTuplePatternShape.Unresolved
    if (type !is ConeTupleType) return CfirTuplePatternShape.NotTuple(type)
    if (elements.size != type.elementTypes.size) return CfirTuplePatternShape.WrongSize(type)
    return CfirTuplePatternShape.Matched(type)
}
