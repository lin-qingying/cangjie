package org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * 注册 standalone 测试专用服务。
 *
 * 对齐 Kotlin `StandaloneModeTestServiceRegistrar`：
 * 该层只承载 standalone *tests* 特有的补充注册，
 * 不再混入 standalone 生产态 permission/lifetime/platform settings。
 */
object CaStandaloneModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * standalone 当前没有额外 project-model 测试服务，保留空实现用于占位扩展点。
     */
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
    }
}
