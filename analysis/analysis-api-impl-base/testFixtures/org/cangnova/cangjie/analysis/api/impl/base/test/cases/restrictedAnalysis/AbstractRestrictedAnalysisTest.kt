package org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * restricted analysis 抽象测试基座。
 *
 * 这里统一为测试宿主替换可切换的 `CaRestrictedAnalysisService`，
 * 让具体用例专注于“拒绝策略”和“异常包装”两条公开契约本身。
 */
abstract class AbstractRestrictedAnalysisTest : AbstractAnalysisApiBasedTest() {
    /**
     * 当前 restricted analysis 测试额外安装的服务注册器。
     *
     * 该注册器会用可切换实现替换项目中的 `CaRestrictedAnalysisService`，供测试精确控制受限状态。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + listOf(RestrictedAnalysisTestServiceRegistrar)

    /**
     * 获取当前测试模块项目中安装的可切换 restricted analysis 服务。
     *
     * 具体测试通过该属性修改受限模式和允许状态，然后触发公开 Analysis API 入口观察行为。
     */
    protected val CjTestModule.restrictedAnalysisService: SwitchableCaRestrictedAnalysisService
        get() = SwitchableCaRestrictedAnalysisService.getInstance(caModule.project)
}

/**
 * restricted analysis 测试专用服务注册器。
 *
 * 注册器在项目级卸载默认 `CaRestrictedAnalysisService`，并替换为测试可控的
 * `SwitchableCaRestrictedAnalysisService`。
 */
private object RestrictedAnalysisTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 注册项目级 restricted analysis 服务。
     *
     * 每个测试项目都获得独立服务实例，避免测试之间共享受限状态。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.picoContainer.unregisterComponent(CaRestrictedAnalysisService::class.java.name)
        project.registerService(CaRestrictedAnalysisService::class.java, SwitchableCaRestrictedAnalysisService())
    }
}

/**
 * 可切换的 restricted analysis 服务实现。
 *
 * 该实现暴露两个可写开关，用于测试“当前是否处于 restricted analysis 模式”和
 * “该模式下是否允许进入分析”两类独立状态。
 */
class SwitchableCaRestrictedAnalysisService : CaRestrictedAnalysisService {
    /**
     * 是否启用 restricted analysis 模式。
     *
     * 开启后 `isAnalysisRestricted` 返回 `true`，分析入口会进入受限分析处理分支。
     */
    var enableRestrictedAnalysisMode: Boolean = true
    /**
     * 在 restricted analysis 模式下是否允许进入分析。
     *
     * 关闭后 `isRestrictedAnalysisAllowed` 返回 `false`，分析入口应调用拒绝逻辑。
     */
    var allowRestrictedAnalysis: Boolean = true

    /**
     * 当前项目是否处于 restricted analysis 模式。
     *
     * 返回值直接来自测试开关，使测试能够覆盖受限与非受限两种入口状态。
     */
    override val isAnalysisRestricted: Boolean
        get() = enableRestrictedAnalysisMode

    /**
     * 当前 restricted analysis 是否被允许。
     *
     * 返回值直接来自测试开关，用于触发允许或拒绝两条路径。
     */
    override val isRestrictedAnalysisAllowed: Boolean
        get() = allowRestrictedAnalysis

    /**
     * 拒绝进入 restricted analysis 的测试实现。
     *
     * 直接抛出专用异常，方便测试区分拒绝路径与其它运行时异常。
     */
    override fun rejectRestrictedAnalysis(): Nothing {
        throw RestrictedAnalysisNotAllowedException()
    }

    /**
     * restricted analysis 被拒绝时抛出的专用测试异常。
     *
     * 具体拒绝策略测试捕获该异常来判断入口是否按预期被阻止。
     */
    class RestrictedAnalysisNotAllowedException : RuntimeException("Restricted analysis is not allowed.")

    /**
     * `SwitchableCaRestrictedAnalysisService` 的项目级获取入口。
     *
     * 该 companion 以强类型形式封装平台服务查询，避免各测试重复强转。
     */
    companion object {
        /**
         * 从指定项目中获取测试安装的可切换 restricted analysis 服务。
         *
         * 若项目未安装测试服务，强转会暴露为测试宿主配置错误。
         */
        fun getInstance(project: Project): SwitchableCaRestrictedAnalysisService =
            CaRestrictedAnalysisService.getInstance(project) as SwitchableCaRestrictedAnalysisService
    }
}
