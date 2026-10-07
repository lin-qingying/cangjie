@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexQueryService
import org.cangnova.cangjie.analysis.api.standalone.base.index.CaStandaloneSymbolIndexQueryService
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * 为索引区段测试注册名称发现服务。
 *
 * 生产态由 standalone 描述符装配该服务；测试宿主不加载整份描述符，
 * 因此按**同一实现**显式注册——否则测试会因为拿不到索引而得到空候选，
 * 而那种「通过」的缺席断言掩盖的正是服务未注册。
 */
object IndexSectionTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 注册会话内名称索引服务。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(
            CaSymbolIndexQueryService::class.java,
            CaStandaloneSymbolIndexQueryService::class.java,
        )
    }
}
