package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol

/**
 * 函数体数据流分析得到的 match subject 构造器 tag。
 *
 * 该事实属于具体程序点，必须由 BODY_RESOLVE/DFA 产生；checker 只能消费，不能从声明
 * initializer 反推。键使用实际 [CfirMatchExpression] 节点，值是该点唯一可知的 enum constructor。
 */
data class CfirMatchSubjectValueDomainData(
    /** match 节点到唯一 enum constructor tag 的映射。 */
    val enumConstructorTags: Map<CfirMatchExpression, CfirEnumConstructorSymbol>,
)

/** match subject value-domain 在函数声明属性表中的键。 */
private object MatchSubjectValueDomainDataKey : CfirDeclarationDataKey()

/** BODY_RESOLVE 写入、checker 读取的函数体 match value-domain。 */
var CfirFunction.matchSubjectValueDomainData: CfirMatchSubjectValueDomainData? by
    CfirDeclarationDataRegistry.data(MatchSubjectValueDomainDataKey)
