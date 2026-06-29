package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.directives.model.Directive
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
/**
 * 表示 `DefaultRegisteredDirectivesProvider`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class DefaultRegisteredDirectivesProvider(defaultGlobalDirectives: RegisteredDirectives) : TestService {
    /**
     * 保存 `defaultDirectives`，供测试服务在测试执行期间读取或传递。
     */
    val defaultDirectives: RegisteredDirectives by lazy {
        defaultGlobalDirectives
    }
}
/**
 * 保存 `TestServices.defaultRegisteredDirectivesProvider`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.defaultRegisteredDirectivesProvider: DefaultRegisteredDirectivesProvider by TestServices.testServiceAccessor()

/**
 * 保存 `TestServices.defaultDirectives`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.defaultDirectives: RegisteredDirectives
    get() = defaultRegisteredDirectivesProvider.defaultDirectives
