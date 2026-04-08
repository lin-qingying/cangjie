package org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis

import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisException
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * restricted analysis 异常包装抽象测试。
 */
abstract class AbstractRestrictedAnalysisExceptionWrappingTest : AbstractRestrictedAnalysisTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = mainModule.testModule.directives
        val throwableType = directives[Directives.THROW].singleOrNull()
            ?: error("必须通过 THROW 指令指定待抛出的异常类型。")
        val expectUnwrapped = directives.contains(Directives.EXPECT_UNWRAPPED)

        val restrictedService = mainModule.restrictedAnalysisService
        restrictedService.enableRestrictedAnalysisMode = true
        restrictedService.allowRestrictedAnalysis = true

        val thrown = instantiateThrowable(throwableType)

        try {
            analyzeForTest(mainFile) {
                throw thrown
            }
            error("受限分析异常包装测试必须抛出异常。")
        } catch (caught: Throwable) {
            if (expectUnwrapped) {
                assertEquals(thrown::class.qualifiedName, caught::class.qualifiedName)
            } else {
                assertTrue(caught is CaRestrictedAnalysisException)
                assertEquals(thrown::class.qualifiedName, caught.cause!!::class.qualifiedName)
            }
        }
    }

    private fun instantiateThrowable(throwableFqName: String): Throwable {
        val exceptionClass = Class.forName(throwableFqName)
        val defaultConstructor = exceptionClass.constructors.singleOrNull { it.parameterCount == 0 }

        val instance = if (defaultConstructor != null) {
            exceptionClass.getDeclaredConstructor().newInstance()
        } else {
            val throwableConstructor = exceptionClass.constructors
                .singleOrNull { it.parameterCount == 1 && it.parameterTypes[0] == Throwable::class.java }
                ?: error("指定的异常 `$throwableFqName` 必须有空构造器或单参数 Throwable 构造器。")
            throwableConstructor.newInstance(Throwable("mock-cause"))
        }

        return instance as? Throwable ?: error("指定类型 `$throwableFqName` 不是 Throwable。")
    }

    object Directives : SimpleDirectivesContainer() {
        val THROW by stringDirective(
            description = "待抛出的异常全限定名。",
            applicability = DirectiveApplicability.Module,
        )

        val EXPECT_UNWRAPPED by directive(
            description = "指定当前异常不应被包装为 CaRestrictedAnalysisException。",
            applicability = DirectiveApplicability.Module,
        )
    }
}
