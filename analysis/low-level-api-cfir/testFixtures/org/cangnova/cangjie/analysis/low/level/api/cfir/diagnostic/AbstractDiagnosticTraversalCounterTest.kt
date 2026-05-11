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
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + BeforeElementDiagnosticCollectionRegistrar

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

    private fun CfirElement.renderForMessage(): String {
        return "${source?.kind} <> ${this::class.simpleName}"
    }

    private object BeforeElementDiagnosticCollectionRegistrar : AnalysisApiTestServiceRegistrar() {
        override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtensionPoint(project)
        }

        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtension(project, BeforeElementLLCfirSessionConfigurator)
        }
    }

    private object BeforeElementLLCfirSessionConfigurator : LLCfirSessionConfigurator {
        override fun configure(session: LLCfirSession) {
            session.register(BeforeElementDiagnosticCollectionHandler::class, BeforeElementTestDiagnosticCollectionHandler())
        }
    }

    private class BeforeElementTestDiagnosticCollectionHandler : BeforeElementDiagnosticCollectionHandler() {
        val visitedTimes: MutableMap<CfirElement, Int> = mutableMapOf()

        override fun beforeCollectingForElement(element: CfirElement) {
            visitedTimes.compute(element) { _, count -> (count ?: 0) + 1 }
        }
    }
}

abstract class AbstractSourceDiagnosticTraversalCounterTest : AbstractDiagnosticTraversalCounterTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
