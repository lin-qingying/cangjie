package org.cangnova.cangjie.lexer

import com.intellij.lang.ASTNode
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.ILeafElementType
import org.cangnova.cangjie.lang.CangJieLanguage

/** 官方 NL 是独立词元，但在 PSI 树中仍以标准空白叶节点表示。 */
class CjNewlineElementType : IElementType("NL", CangJieLanguage), ILeafElementType {
    init {
        // 注册到平台的跨语言空白叶类型集合，不进入 ParserDefinition 的游标跳过集合。
        PsiBuilderImpl.registerWhitespaceToken(this)
    }

    override fun createLeafNode(leafText: CharSequence): ASTNode = PsiWhiteSpaceImpl(leafText)
}
