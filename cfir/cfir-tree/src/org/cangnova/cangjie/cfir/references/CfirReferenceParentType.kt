package org.cangnova.cangjie.cfir.references

import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 已完成引用的父类型状态，对应仓颉 NameReferenceExpr.matchedParentTy。
 *
 * Absent 表示该匹配没有发布父类型；Invalid 是实例化过程实际产生的非空 InvalidTy，
 * 不携带类型错误诊断。上界或接口匹配得到的真实父类型由 Instantiated 保留。
 */
sealed interface CfirReferenceParentType {
    data object Absent : CfirReferenceParentType
    data object Invalid : CfirReferenceParentType
    data class Instantiated(val type: ConeCangJieType) : CfirReferenceParentType
}
