package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.CustomLanguageASTComparator
import com.intellij.util.ThreeState
import com.intellij.util.diff.FlyweightCapableTreeStructure

/**
 * 已构建 AST 之间的 diff 也必须比较解析含义；文本相同并不表示 annotation/macro 模式相同。
 * 本扩展没有解析中的可变状态，不修改旧树。AST→LT 比较由当前 builder 的冻结数据负责。
 */
class CangJieParsingDataComparator : CustomLanguageASTComparator {
    override fun compareAST(
        oldNode: ASTNode,
        newNode: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
    ): ThreeState = ThreeState.UNSURE

    override fun compareAST(oldNode: ASTNode, newNode: ASTNode): ThreeState =
        if (oldNode.parserSeedOrNull() != newNode.parserSeedOrNull() ||
            oldNode.getCopyableUserData(PARSED_FILE_RESULT_KEY) !== newNode.getCopyableUserData(PARSED_FILE_RESULT_KEY) ||
            oldNode.getCopyableUserData(PARSED_ROLE_REGION_KEY) !== newNode.getCopyableUserData(PARSED_ROLE_REGION_KEY) ||
            oldNode.getCopyableUserData(CJMP_DECLARATION_ROLE_EVENTS_KEY) !== newNode.getCopyableUserData(CJMP_DECLARATION_ROLE_EVENTS_KEY) ||
            oldNode.getCopyableUserData(MODIFIER_SOURCE_FACT_KEY) !== newNode.getCopyableUserData(MODIFIER_SOURCE_FACT_KEY) ||
            oldNode.getCopyableUserData(ANNOTATION_SOURCE_FACT_KEY) !== newNode.getCopyableUserData(ANNOTATION_SOURCE_FACT_KEY) ||
            oldNode.getCopyableUserData(DECLARATION_PREFIX_KEY) !== newNode.getCopyableUserData(DECLARATION_PREFIX_KEY) ||
            oldNode.getCopyableUserData(DECLARATION_MODIFIERS_KEY) !== newNode.getCopyableUserData(DECLARATION_MODIFIERS_KEY) ||
            oldNode.getCopyableUserData(PARSER_SOURCE_TOKEN_KEY) != newNode.getCopyableUserData(PARSER_SOURCE_TOKEN_KEY) ||
            oldNode.getCopyableUserData(MACRO_PARSING_FACTS_KEY) != newNode.getCopyableUserData(MACRO_PARSING_FACTS_KEY)
        ) ThreeState.NO else ThreeState.UNSURE
}
