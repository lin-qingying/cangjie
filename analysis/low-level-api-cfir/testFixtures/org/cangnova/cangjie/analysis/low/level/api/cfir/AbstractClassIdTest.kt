package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 对齐 Kotlin `AbstractClassIdTest`，直接把 PSI 计算出的 `ClassId`
 * 回写成 inline block comment，source file 本身就是 golden。
 */
abstract class AbstractClassIdTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val text = buildString {
            mainFile.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is CjClassLikeDeclaration) {
                        append("/* ClassId: ${element.getClassId()} */")
                    }

                    if (element is LeafPsiElement) {
                        append(element.text)
                    }

                    element.acceptChildren(this)
                }

                override fun visitComment(comment: PsiComment) {
                    if (comment.tokenType == CjTokens.BLOCK_COMMENT) {
                        return
                    }

                    super.visitComment(comment)
                }
            })
        }

        testServices.assertions.assertEqualsToTestOutputFile(text, extension = ".cj")
    }
}

abstract class AbstractSourceClassIdTest : AbstractClassIdTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
