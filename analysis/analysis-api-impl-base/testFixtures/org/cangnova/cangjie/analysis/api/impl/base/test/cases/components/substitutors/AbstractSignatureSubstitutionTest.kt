package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.substitutors

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedSubstitutedParameterType
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedSubstitutedReturnType
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetFunctionName
import org.cangnova.cangjie.analysis.api.components.buildSubstitutor
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * 签名替换抽象测试。
 *
 * 这里覆盖公开 substitutor 协议的完整闭环：
 * 1. 基于签名类型参数创建公开替换器；
 * 2. 对结构化签名执行公开替换；
 * 3. 观察替换后参数与返回类型的公开渲染结果。
 */
abstract class AbstractSignatureSubstitutionTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val targetClass = PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java)
            .single { it.name == directives.targetClassName }

        analyzeForTest(mainFile) {
            val classSymbol = getClassLikeSymbol(targetClass.getClassId()!!)
            val callableSymbol = getTopLevelCallableSymbols(
                mainFile.packageFqName,
                Name.identifier(directives.targetFunctionName),
            ).singleOrNull()
            val signature = callableSymbol?.asSignature()

            assertNotNull(classSymbol, "签名替换测试需要可恢复的 class-like 符号。")
            assertNotNull(callableSymbol, "签名替换测试需要可恢复的 callable 符号。")
            assertNotNull(signature, "签名替换测试需要可恢复的公开签名。")

            val substitutor = buildSubstitutor {
                substitution(
                    typeParameter = signature!!.typeParameters.single(),
                    type = classSymbol!!.defaultType,
                )
            }
            val substitutedSignature = callableSymbol!!.substitute(substitutor)

            assertEquals(
                directives.expectedSubstitutedParameterType,
                normalizeTypeRendering(substitutedSignature.valueParameters.single().type!!.render()),
            )
            assertEquals(
                directives.expectedSubstitutedReturnType,
                normalizeTypeRendering(substitutedSignature.returnType!!.render()),
            )
        }
    }
}
