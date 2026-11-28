package org.cangnova.cangjie.analysis.low.level.api.cfir.test

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.parsing.compilationParsingResultOrNull
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame

/**
 * 以当前 CFIR 文件实际使用的 Parsed 产品验收原树与目标树的唯一对象对应。
 * 原 PSI 用于公开导航，低层源码锚点必须属于独立目标树；缺失对应直接使测试失败。
 */
internal class TargetSourceTestSupport(
    private val cfirFile: CfirFile,
    val originFile: CjFile,
) {
    private val sourceFile = (requireNotNull(cfirFile.source) as CjPsiSourceElement).psi as CjFile
    private val product = requireNotNull(sourceFile.compilationParsingResultOrNull()) {
        "The CFIR source file must carry its actual Parsed product."
    }

    val targetFile: CjFile
        get() = product.withTargetFile { target ->
            assertFileIdentity(target)
            target
        }

    init {
        product.withTargetFile { target ->
            assertFileIdentity(target)
            assertCorrespondence(originFile, target, target)
        }
    }

    /** 仅通过本代产品定位原对象的目标对象，并验收反向导航仍是同一个原对象。 */
    fun targetElement(origin: PsiElement): PsiElement = product.withTargetFile { targetFile ->
        assertFileIdentity(targetFile)
        assertSame(originFile, origin.containingFile)
        val target = requireNotNull(product.correspondingElementOrNull(origin)) {
            "The original PSI must have a unique target in the current Parsed product."
        }
        assertCorrespondence(origin, target, targetFile)
        target
    }

    /** 反查目标对象的真实来源；不接受另一棵树的对象或名称、文本相同的替代对象。 */
    fun originalElement(target: PsiElement): PsiElement = product.withTargetFile { targetFile ->
        assertFileIdentity(targetFile)
        assertSame(targetFile, target.containingFile)
        val origin = requireNotNull(product.originalElementOrNull(target)) {
            "The target PSI must navigate to its actual original object."
        }
        assertCorrespondence(origin, target, targetFile)
        origin
    }

    /** 将实际低层 source 锚点与原对象的唯一 target 做 identity 比较。 */
    fun assertTargetElement(origin: PsiElement, actualTarget: PsiElement?) {
        assertSame(targetElement(origin), actualTarget)
    }

    private fun assertFileIdentity(target: CjFile) {
        assertSame(sourceFile, target)
        assertSame(target, (requireNotNull(cfirFile.source) as CjPsiSourceElement).psi)
        assertSame(originFile, product.originFile)
        assertNotSame(originFile, target)
        assertNotSame(originFile.node, target.node)
    }

    private fun assertCorrespondence(origin: PsiElement, target: PsiElement, targetFile: CjFile) {
        assertSame(originFile, origin.containingFile)
        assertSame(targetFile, target.containingFile)
        assertNotSame(origin, target)
        assertSame(target, product.correspondingElementOrNull(origin))
        assertSame(origin, product.originalElementOrNull(target))
    }
}
