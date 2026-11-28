package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.parsing.createCangJiePsiBuilder
import org.cangnova.cangjie.parsing.requireParserSeed

/** SDK 的 DummyHolder 重解析同样只读取 chameleon 上的完整 seed。 */
class CjMacroCodeFragmentType : ICodeFragmentElementType("cangjie.MACRO_CODE_FRAGMENT", CangJieLanguage) {
    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? {
        val builder = createCangJiePsiBuilder(psi.project, chameleon, getLanguageForParser(psi))
        return CangJieParser.parseMacroCodeFragment(builder, chameleon.requireParserSeed(), psi.containingFile as? CjCodeFragment).firstChildNode
    }
}
