package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.services.MetaTestConfigurator
import org.cangnova.cangjie.test.services.ModuleStructureExtractor
import org.cangnova.cangjie.test.services.PreAnalysisHandler
import org.cangnova.cangjie.test.services.TestServices

typealias Constructor<R> = (TestServices) -> R

typealias Constructor2<T, R> = (TestServices, T) -> R

/**
 * 测试配置
 *
 * 对应 Kotlin K2 的 TestConfiguration
 */
interface TestConfiguration<Step : TestStep<*, *>> {
    val rootDisposable: Disposable
    val testServices: TestServices
    val directives: DirectivesContainer
    val defaultRegisteredDirectives: RegisteredDirectives
    val moduleStructureExtractor: ModuleStructureExtractor
    val preAnalysisHandlers: List<PreAnalysisHandler>
    val metaTestConfigurators: List<MetaTestConfigurator>
    val afterAnalysisCheckers: List<AfterAnalysisChecker>
    val metaInfoHandlerEnabled: Boolean

    val steps: List<Step>
}

fun <R> (() -> R).coerce(): Constructor<R> {
    return { this.invoke() }
}
