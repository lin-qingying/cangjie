package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.beforeElementDiagnosticCollectionHandler
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.PersistenceContextCollector
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructureElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolvableSessionForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.resolve.SessionHolderImpl
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 对齐 Kotlin `AbstractFirContextCollectionTest`：
 * 在诊断收集真正进入嵌套声明前，校验运行时 checker context
 * 与 `PersistenceContextCollector` 回放出来的 containing-declarations 链一致。
 */
abstract class AbstractCfirContextCollectionTest : AbstractAnalysisApiBasedTest() {
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + BeforeElementDiagnosticCollectionRegistrar

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val session = mainFile.getResolvableSessionForTest()
        check(session is LLCfirResolvableModuleSession)

        val handler = session.beforeElementDiagnosticCollectionHandler as BeforeElementTestDiagnosticCollectionHandler
        val fileStructure = session.moduleComponents.fileStructureCache.getFileStructure(mainFile)
        handler.elementsToCheckContext = fileStructure.getAllStructureElements().map(FileStructureElement::declaration)
        handler.cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)

        mainFile.collectDiagnosticsForFile(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
    }

    private object BeforeElementDiagnosticCollectionRegistrar : AnalysisApiTestServiceRegistrar() {
        override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtensionPoint(project)
        }

        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtension(project, BeforeElementLLCfirSessionConfigurator(testServices))
        }
    }

    private class BeforeElementLLCfirSessionConfigurator(
        private val testServices: TestServices,
    ) : LLCfirSessionConfigurator {
        override fun configure(session: LLCfirSession) {
            session.register(
                BeforeElementDiagnosticCollectionHandler::class,
                BeforeElementTestDiagnosticCollectionHandler(testServices.assertions),
            )
        }
    }

    private class BeforeElementTestDiagnosticCollectionHandler(
        private val assertions: AssertionsService,
    ) : BeforeElementDiagnosticCollectionHandler() {
        private val cfirRenderer = CfirRenderer.withReadability()

        lateinit var elementsToCheckContext: List<CfirDeclaration>
        lateinit var cfirFile: CfirFile

        override fun beforeGoingNestedDeclaration(declaration: CfirDeclaration, context: CheckerContext) {
            if (declaration is CfirFile || declaration !in elementsToCheckContext) {
                return
            }

            val expectedContextStructure = context.containingDeclarations.renderStructure()
            val collectedContext = PersistenceContextCollector.collectContext(
                SessionHolderImpl(declaration.moduleData.session, ScopeSession()),
                cfirFile,
                declaration,
            )

            assertions.assertEquals(
                expectedContextStructure,
                collectedContext.containingDeclarations.renderStructure(),
            )
        }

        /**
         * 只比较 low-level 主干真实提供的声明路径，不把尚未接入的 checker 细节混进断言。
         */
        private fun List<CfirDeclaration>.renderStructure(): String =
            filterIndexed { index, declaration -> index == 0 || this[index - 1] !== declaration }
                .joinToString(separator = " -> ") { declaration ->
                when (declaration) {
                    is CfirFile -> "file:${declaration.name}"
                    is CfirTypeAlias -> "typealias:${declaration.symbol.classId.asString()}"
                    is CfirClass -> "class:${declaration.symbol.classId.asString()}"
                    is CfirInterface -> "interface:${declaration.symbol.classId.asString()}"
                    is CfirStruct -> "struct:${declaration.symbol.classId.asString()}"
                    is CfirEnum -> "enum:${declaration.symbol.classId.asString()}"
                    is CfirExtend -> "extend:${cfirRenderer.renderElementAsString(declaration.extendedTypeRef)}"
                    is CfirConstructor -> "ctor:${declaration.valueParameters.renderParameterNames()}"
                    is CfirNamedFunction -> "func:${declaration.symbol.name.renderSafeName()}"
                    is CfirMainFunction -> "main:${declaration.symbol.name.renderSafeName()}"
                    is CfirMacroDeclaration -> "macro:${declaration.symbol.name.renderSafeName()}"
                    is CfirFinalizer -> "finalizer"
                    is CfirProperty -> "property:${declaration.name.renderSafeName()}"
                    is CfirPropertyAccessor -> if (declaration.isGetter) {
                        "getter:${declaration.propertySymbol.name.renderSafeName()}"
                    } else {
                        "setter:${declaration.propertySymbol.name.renderSafeName()}"
                    }
                    is CfirFieldVariable -> "field:${declaration.symbol.name.renderSafeName()}"
                    is CfirPatternVariable -> "pattern:${declaration.symbol.name.renderSafeName()}"
                    is CfirPatternBindingVariable -> "patternBinding:${declaration.symbol.name.renderSafeName()}"
                    is CfirValueParameter -> "value:${declaration.name.renderSafeName()}"
                    is CfirTypeParameter -> "type:${declaration.name.renderSafeName()}"
                    else -> declaration::class.simpleName ?: "<unknown>"
                }
            }

        private fun List<CfirValueParameter>.renderParameterNames(): String =
            joinToString(separator = ",") { parameter -> parameter.name.renderSafeName() }

        private fun org.cangnova.cangjie.name.Name.renderSafeName(): String =
            if (this == SpecialNames.NO_NAME_PROVIDED) "<no name provided>" else asString()
    }
}

abstract class AbstractSourceCfirContextCollectionTest : AbstractCfirContextCollectionTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
