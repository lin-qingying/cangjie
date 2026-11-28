package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.parsing.CjMacroFragmentParseInfo
import org.cangnova.cangjie.parsing.CjParserSeed
import org.cangnova.cangjie.parsing.PARSED_FILE_RESULT_KEY
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.parsing.isMacroFragmentRootType

/** 宏输出的真实 code fragment；内容为直接节点序列，不创建任何声明宿主。 */
class CjMacroCodeFragment private constructor(
    project: Project,
    name: String,
    text: CharSequence,
    context: PsiElement?,
    imports: String?,
    seed: CjParserSeed,
) : CjCodeFragment(project, name, text, imports, CjNodeTypes.MACRO_CODE_FRAGMENT, context, seed) {

    constructor(project: Project, name: String, text: CharSequence, context: PsiElement?, seed: CjParserSeed) :
        this(project, name, text, context, null, seed)
    init {
        require(seed.macroFragment != null) { "Macro fragments require their original parsing entry." }
        require(seed.context.enableCustomAnnotation && seed.context.disableMacroParsing) {
            "Macro output must be reparsed with the native custom-annotation mode."
        }
    }

    override fun getContentElement(): CjMacroFragmentContents =
        checkNotNull(findChildByClass(CjMacroFragmentContents::class.java)) { "Missing macro fragment contents." }

    internal override fun createCompilationFragment(text: String, seed: CjParserSeed, targetContext: PsiElement?): CjCodeFragment =
        CjMacroCodeFragment(project, name, text, targetContext, importsToString(), seed)
}

/** 与 Kotlin block code fragment 的内容节点同层，保留 ParseNodes 的全部直接 roots。 */
class CjMacroFragmentContents(node: ASTNode) : CjElementImpl(node) {
    val roots: List<CjElement>
        get() = children.filter { it.node.elementType.isMacroFragmentRootType() }.map {
            check(it is CjElement) { "A macro syntax root must have its language PSI: ${it.node.elementType}" }
            it
        }

    val fileFacts: ParsingFileFacts
        get() = checkNotNull(node.getCopyableUserData(PARSED_FILE_RESULT_KEY)).fileFacts

    val parsingInfo: CjMacroFragmentParseInfo
        get() = checkNotNull(checkNotNull(node.getCopyableUserData(PARSED_FILE_RESULT_KEY)).macroFragmentInfo)
}
