package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.ClassDiagnosticRetriever
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.beforeElementDiagnosticCollectionHandler
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.PersistentCheckerContextFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.SessionHolderImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 验证 diagnostic collection 对应 CFIR 元素只访问一次。
 */
abstract class AbstractDiagnosticTraversalCounterTest : AbstractAnalysisApiBasedTest() {
    /**
     * 为测试注册诊断收集前置处理器，用于统计每个 CFIR 元素的访问次数。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + BeforeElementDiagnosticCollectionRegistrar

    /**
     * 触发默认诊断收集，并断言每个应检查的 CFIR 元素恰好被访问一次。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        mainFile.collectDiagnosticsForFile(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)

        val cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)
        val errorElements = collectErrorElements(cfirFile)

        if (errorElements.isNotEmpty()) {
            val zeroElements = errorElements.filter { it.second == 0 }
            val nonZeroElements = errorElements.filter { it.second > 1 }
            val message = buildString {
                if (zeroElements.isNotEmpty()) {
                    appendLine("The following elements were not visited")
                    appendLine(zeroElements.joinToString(separator = "\n\n") { it.first.renderForMessage() })
                }
                if (nonZeroElements.isNotEmpty()) {
                    appendLine("The following elements were visited more than one time")
                    appendLine(nonZeroElements.joinToString(separator = "\n\n") { "${it.second} times ${it.first.renderForMessage()}" })
                }
            }
            testServices.assertions.fail { message }
        }
    }

    /**
     * 收集所有访问次数不符合预期的 CFIR 元素及其实际访问次数。
     */
    private fun collectErrorElements(cfirFile: CfirFile): List<Pair<CfirElement, Int>> {
        val handler = cfirFile.moduleData.session.beforeElementDiagnosticCollectionHandler
            as BeforeElementTestDiagnosticCollectionHandler
        val nonDuplicatingElements = findNonDuplicatingCfirElements(cfirFile).filter { element ->
            when {
                element is CfirTypeRef && element.source?.kind != CjRealSourceElementKind -> false
                element.source?.kind == CjRealSourceElementKind -> true
                ClassDiagnosticRetriever.shouldDiagnosticsAlwaysBeCheckedOn(element) -> true
                else -> false
            }
        }.toSet()

        val errorElements = mutableListOf<Pair<CfirElement, Int>>()
        cfirFile.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element in nonDuplicatingElements) {
                    val visitedTimes = handler.visitedTimes[element] ?: 0
                    if (visitedTimes != 1) {
                        errorElements += element to visitedTimes
                    }
                }
                element.acceptChildren(this)
            }
        })

        return errorElements
    }

    /**
     * 使用诊断收集 visitor 预先计算在标准遍历中只出现一次的 CFIR 元素集合。
     */
    private fun findNonDuplicatingCfirElements(cfirFile: CfirFile): Set<CfirElement> {
        val elementUsageCount = mutableMapOf<CfirElement, Int>()
        val sessionHolder = SessionHolderImpl(cfirFile.moduleData.session, ScopeSession())
        val visitor = object : AbstractDiagnosticCollectorVisitor(
            PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder),
        ) {
            override fun visitNestedElements(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun checkElement(element: CfirElement) {
                elementUsageCount.compute(element) { _, count -> (count ?: 0) + 1 }
            }
        }

        cfirFile.accept(visitor, null)
        return elementUsageCount.filterValues { it == 1 }.keys
    }

    /**
     * 渲染失败消息中的 CFIR 元素摘要，包含源码 kind 与运行时节点类型。
     */
    private fun CfirElement.renderForMessage(): String {
        return "${source?.kind} <> ${this::class.simpleName}"
    }

    /**
     * 在测试项目中注册 session 配置扩展点，并安装访问次数统计处理器。
     */
    private object BeforeElementDiagnosticCollectionRegistrar : AnalysisApiTestServiceRegistrar() {
        /**
         * 声明 `LLCfirSessionConfigurator` 扩展点，允许测试注册 session 级组件。
         */
        override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtensionPoint(project)
        }

        /**
         * 注册本测试使用的 session 配置器。
         */
        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtension(project, BeforeElementLLCfirSessionConfigurator)
        }
    }

    /**
     * 为每个 low-level CFIR session 安装诊断收集前置处理器。
     */
    private object BeforeElementLLCfirSessionConfigurator : LLCfirSessionConfigurator {
        /**
         * 把访问次数统计处理器注册到 session 组件容器。
         */
        override fun configure(session: LLCfirSession) {
            session.register(BeforeElementDiagnosticCollectionHandler::class, BeforeElementTestDiagnosticCollectionHandler())
        }
    }

    /**
     * 记录诊断收集流程对每个 CFIR 元素的实际访问次数。
     */
    private class BeforeElementTestDiagnosticCollectionHandler : BeforeElementDiagnosticCollectionHandler() {
        /**
         * 以 CFIR 元素实例为键保存访问计数，供测试结束后与预期遍历集合比较。
         */
        val visitedTimes: MutableMap<CfirElement, Int> = mutableMapOf()

        /**
         * 在诊断收集器处理元素前递增对应元素的访问次数。
         */
        override fun beforeCollectingForElement(element: CfirElement) {
            visitedTimes.compute(element) { _, count -> (count ?: 0) + 1 }
        }
    }
}

/**
 * 使用源码 low-level CFIR 配置运行诊断遍历次数测试。
 */
abstract class AbstractSourceDiagnosticTraversalCounterTest : AbstractDiagnosticTraversalCounterTest() {
    /**
     * 固定使用源码模块配置，避免 dependent session 改变诊断遍历对象。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
