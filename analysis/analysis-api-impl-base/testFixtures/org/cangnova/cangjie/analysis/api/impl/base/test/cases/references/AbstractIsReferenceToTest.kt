package org.cangnova.cangjie.analysis.api.impl.base.test.cases.references

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.isExtendMemberDeclaration
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceBehaviorTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedReferenceTargetClass
import org.cangnova.cangjie.analysis.api.impl.base.test.referenceBehaviorTargetKind
import org.cangnova.cangjie.analysis.api.impl.base.test.referenceBehaviorTargetName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * `PsiReference.isReferenceTo` 抽象测试。
 *
 * 测试同时校验引用可解析、解析目标 PSI 类名符合预期，并验证 `isReferenceTo(target)` 命中
 * testData 指定的真实目标声明。
 */
abstract class AbstractIsReferenceToTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前引用行为测试额外注册的指令集合。
     *
     * 该测试需要目标种类、目标名称和解析后 PSI 类名，公共组件指令负责提供基础目标字段。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiReferenceBehaviorTestDirectives

    /**
     * 执行 `PsiReference.isReferenceTo` 断言。
     *
     * 方法先按指令定位声明目标，再定位对应使用点引用，并比较解析结果与 `isReferenceTo` 语义。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val target = findTarget(mainFile, directives.referenceBehaviorTargetKind, directives.referenceBehaviorTargetName)
        val referenceExpression = findUsageSimpleName(mainFile, directives.referenceBehaviorTargetName)

        val reference = referenceExpression.mainReference
        val resolved = reference.resolve()

        assertNotNull(resolved, "引用必须可解析，才能验证 isReferenceTo")
        assertEquals(directives.expectedReferenceTargetClass, resolved!!::class.simpleName)
        assertTrue(reference.isReferenceTo(target), "reference.isReferenceTo(target) 应返回 true")
    }

    /**
     * 按 testData 中的目标种类与名称查找引用应命中的 PSI 目标。
     *
     * 该函数区分顶层函数、extend 成员、import alias 和 binding pattern，避免同名 PSI 被错误选中。
     */
    private fun findTarget(
        mainFile: CjFile,
        targetKind: String,
        targetName: String,
    ): PsiElement = when (targetKind) {
        "TOP_LEVEL_FUNCTION" -> PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == targetName && !it.isExtendMemberDeclaration() }

        "EXTEND_MEMBER" -> PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == targetName && it.isExtendMemberDeclaration() }

        "IMPORT_ALIAS" -> PsiTreeUtil.findChildrenOfType(mainFile, CjImportAlias::class.java)
            .single { it.name == targetName }

        "BINDING_PATTERN" -> PsiTreeUtil.findChildrenOfType(mainFile, CjBindingPattern::class.java)
            .single { it.name == targetName }

        else -> error("Unsupported reference target kind: $targetKind")
    }
}
