package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedCallableName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.api.impl.base.test.targetNameText
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * `resolver.resolveToSymbol` 的抽象测试。
 *
 * testData 同时声明目标调用和目标 simple-name，验证两条公开入口是否收敛到同一 callable 语义。
 */
abstract class AbstractResolveSymbolTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val callExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { it.text == directives.targetCallText }
        val nameReference = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .single { it.referencedName == directives.targetNameText }

        analyzeForTest(callExpression) {
            val resolvedFromCall = callExpression.resolveToSymbol()
            val resolvedFromName = nameReference.resolveToSymbol()

            assertNotNull(resolvedFromCall, "调用表达式没有解析到符号。")
            assertNotNull(resolvedFromName, "simple-name 没有解析到符号。")
            assertTrue(resolvedFromCall is CaCallableSymbol, "调用表达式应解析为 callable 符号。")
            assertTrue(resolvedFromName is CaCallableSymbol, "simple-name 应解析为 callable 符号。")
            assertEquals(directives.expectedCallableName, (resolvedFromCall as CaCallableSymbol).name?.asString())
            assertEquals(directives.expectedCallableName, (resolvedFromName as CaCallableSymbol).name?.asString())
            assertEquals(
                resolvedFromCall.callableId,
                (resolvedFromName as CaCallableSymbol).callableId,
                "调用表达式和 simple-name 的解析结果不一致。",
            )
        }
    }
}
