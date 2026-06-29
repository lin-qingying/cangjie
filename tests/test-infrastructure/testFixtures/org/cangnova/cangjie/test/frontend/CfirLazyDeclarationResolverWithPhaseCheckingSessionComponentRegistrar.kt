package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.TestService

/**
 * 表示 `CfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar(
    @Suppress("UNUSED_PARAMETER") testServices: TestServices,
) : TestService {
    /**
     * 保存 `lazyResolver`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val lazyResolver = CfirCompilerLazyDeclarationResolverWithPhaseChecking()

    /**
     * 执行 `registerAdditionalComponent` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun registerAdditionalComponent(configurator: CfirSessionConfigurator) {
        configurator.registerComponent(CfirLazyDeclarationResolver::class, lazyResolver)
    }
}

/**
 * 保存 `TestServices.cfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar`，供CFIR 前端测试在测试执行期间读取或传递。
 */
val TestServices.cfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar: CfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar?
    by TestServices.nullableTestServiceAccessor()
