package org.cangnova.cangjie.analysis.api.impl.base.test.cases.references

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceBehaviorTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedAliasName
import org.cangnova.cangjie.analysis.api.impl.base.test.referenceBehaviorTargetName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * import alias 引用行为抽象测试。
 *
 * 测试验证 alias 名称本身可以作为引用目标被 `isReferenceTo(alias)` 命中，而不是只解析到被导入声明。
 */
abstract class AbstractReferenceImportAliasTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前 alias 引用测试额外注册的指令集合。
     *
     * 该测试复用公共目标名称，并通过 reference behavior 指令读取期望 alias 名称。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiReferenceBehaviorTestDirectives

    /**
     * 执行 import alias 引用断言。
     *
     * 方法定位引用使用点和 import alias 声明，先确认引用可解析，再确认引用能命中 alias PSI。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val referenceExpression = findUsageSimpleName(mainFile, directives.referenceBehaviorTargetName)
        val importAlias = PsiTreeUtil.findChildrenOfType(mainFile, CjImportAlias::class.java)
            .single { it.name == directives.expectedAliasName }

        val reference = referenceExpression.mainReference
        assertNotNull(reference.resolve(), "alias 引用必须可先解析")
        assertTrue(reference.isReferenceTo(importAlias), "alias 引用应被 isReferenceTo(alias) 命中")
    }
}
