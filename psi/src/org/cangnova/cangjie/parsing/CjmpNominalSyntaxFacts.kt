package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import java.util.Collections

/** 当前文法解析出的直接成员语法事实；角色保留修饰符源码拼写。 */
data class CjmpDirectMemberSyntaxFacts(
    val declarationType: IElementType,
    val isCommon: Boolean,
    val isSpecific: Boolean,
    val isPlatform: Boolean,
    val hasFunctionBody: Boolean,
    val hasPropertyAccessor: Boolean,
    val hasInitializer: Boolean,
)

/** 一个容器完成解析时的直接成员快照，不随后续宏展开和反糖改变。 */
class CjmpNominalSyntaxFacts(members: List<CjmpDirectMemberSyntaxFacts>) {
    val members: List<CjmpDirectMemberSyntaxFacts> = Collections.unmodifiableList(members.toList())

    override fun equals(other: Any?): Boolean = other is CjmpNominalSyntaxFacts && members == other.members
    override fun hashCode(): Int = members.hashCode()
}

/** 读取容器关键字的解析期事实；无事实与已解析的空成员列表保持区分。 */
fun ASTNode.cjmpNominalSyntaxFactsOrNull(): CjmpNominalSyntaxFacts? =
    getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY)
