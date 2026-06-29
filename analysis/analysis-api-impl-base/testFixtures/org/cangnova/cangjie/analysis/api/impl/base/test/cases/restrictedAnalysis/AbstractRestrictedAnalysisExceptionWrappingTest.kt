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
 *
 * 测试验证 restricted analysis 执行块中抛出的异常是否按框架规则包装为
 * `CaRestrictedAnalysisException`，以及哪些异常应保持原样透出。
 */
abstract class AbstractRestrictedAnalysisExceptionWrappingTest : AbstractRestrictedAnalysisTest() {
    /**
     * 当前异常包装测试额外注册的模块级指令。
     *
     * 指令指定要抛出的异常类型，以及该异常是否应绕过 restricted analysis 包装。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    /**
     * 执行 restricted analysis 异常包装断言。
     *
     * 方法根据指令实例化异常，在允许的 restricted analysis 块中抛出，并比较捕获异常是否被正确包装。
     */
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

    /**
     * 根据全限定名实例化测试要抛出的异常。
     *
     * 支持无参构造器和单参数 `Throwable` 构造器，覆盖普通异常与需要 cause 的异常形态。
     */
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

    /**
     * restricted analysis 异常包装测试的指令集合。
     *
     * 指令作用于模块级，因为异常包装行为不依赖具体源码位置，只依赖当前分析入口配置。
     */
    object Directives : SimpleDirectivesContainer() {
        /**
         * 当前测试要在 restricted analysis 块中抛出的异常全限定名。
         *
         * 测试会反射实例化该异常，并检查最终捕获到的异常形态。
         */
        val THROW by stringDirective(
            description = "待抛出的异常全限定名。",
            applicability = DirectiveApplicability.Module,
        )

        /**
         * 标记当前异常不应被包装为 `CaRestrictedAnalysisException`。
         *
         * 指令存在时测试期望捕获到原始异常类型；缺失时测试期望捕获包装异常。
         */
        val EXPECT_UNWRAPPED by directive(
            description = "指定当前异常不应被包装为 CaRestrictedAnalysisException。",
            applicability = DirectiveApplicability.Module,
        )
    }
}
