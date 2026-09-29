package org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.analysis.api.impl.base.packages.CangJiePsiFacadeImpl
import org.cangnova.cangjie.psi.CangJiePsiFacade

/**
 * 注册 standalone 测试专用服务。
 *
 * 对齐 Kotlin `StandaloneModeTestServiceRegistrar`：
 * 该层只承载 standalone *tests* 特有的补充注册，
 * 不再混入 standalone 生产态 permission/lifetime/platform settings。
 */
object CaStandaloneModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 使用与 standalone 生产态相同的包 PSI 门面，使引用测试能验证真实包导航目标。
     */
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        if (project.getService(CangJiePsiFacade::class.java) == null) {
            project.registerService(CangJiePsiFacade::class.java, CangJiePsiFacadeImpl(project))
        }
    }
}
