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
    /**
     * 为当前测试额外注册诊断收集前置回调，使测试能够观察 checker 进入嵌套声明时的上下文。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + BeforeElementDiagnosticCollectionRegistrar

    /**
     * 构建主文件的 low-level CFIR，收集需要校验的声明节点，并触发默认诊断检查链。
     */
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

    /**
     * 在测试项目中注册 low-level CFIR session 配置扩展点，并安装本测试专用的诊断回调。
     */
    private object BeforeElementDiagnosticCollectionRegistrar : AnalysisApiTestServiceRegistrar() {
        /**
         * 声明 `LLCfirSessionConfigurator` 扩展点，使测试环境可以向 session 注入自定义组件。
         */
        override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtensionPoint(project)
        }

        /**
         * 注册会话配置器，将断言服务传递给诊断收集前置回调。
         */
        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            LLCfirSessionConfigurator.registerExtension(project, BeforeElementLLCfirSessionConfigurator(testServices))
        }
    }

    /**
     * 将本测试的 `BeforeElementDiagnosticCollectionHandler` 实例挂到每个 low-level CFIR session 上。
     */
    private class BeforeElementLLCfirSessionConfigurator(
        /**
         * 测试服务集合，用于取得断言服务并构造回调处理器。
         */
        private val testServices: TestServices,
    ) : LLCfirSessionConfigurator {
        /**
         * 向 session 注册诊断收集前置处理器，替换默认空实现。
         */
        override fun configure(session: LLCfirSession) {
            session.register(
                BeforeElementDiagnosticCollectionHandler::class,
                BeforeElementTestDiagnosticCollectionHandler(testServices.assertions),
            )
        }
    }

    /**
     * 在诊断检查进入嵌套声明前比对实时 checker context 与持久化上下文收集结果。
     */
    private class BeforeElementTestDiagnosticCollectionHandler(
        /**
         * 测试断言服务，用于报告上下文链不一致的失败信息。
         */
        private val assertions: AssertionsService,
    ) : BeforeElementDiagnosticCollectionHandler() {
        /**
         * 以可读形式渲染 CFIR 节点，保证 extend 等声明在断言文本中稳定可辨。
         */
        private val cfirRenderer = CfirRenderer.withReadability()

        /**
         * 当前测试文件中需要验证上下文的结构化声明列表。
         */
        lateinit var elementsToCheckContext: List<CfirDeclaration>

        /**
         * 当前测试文件对应的 CFIR 文件根节点，作为持久化上下文收集的遍历入口。
         */
        lateinit var cfirFile: CfirFile

        /**
         * 在诊断检查器即将进入嵌套声明时，确认上下文回放结果与运行时上下文完全一致。
         */
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

        /**
         * 将构造函数值参数列表渲染为稳定的逗号分隔名称，用于上下文路径断言。
         */
        private fun List<CfirValueParameter>.renderParameterNames(): String =
            joinToString(separator = ",") { parameter -> parameter.name.renderSafeName() }

        /**
         * 渲染声明名称，并把编译器内部的无名占位符转换成可读文本。
         */
        private fun org.cangnova.cangjie.name.Name.renderSafeName(): String =
            if (this == SpecialNames.NO_NAME_PROVIDED) "<no name provided>" else asString()
    }
}

/**
 * 使用源码 low-level CFIR 配置运行上下文收集一致性测试。
 */
abstract class AbstractSourceCfirContextCollectionTest : AbstractCfirContextCollectionTest() {
    /**
     * 固定使用源码模块测试配置，避免依赖 dependent session 影响上下文链断言。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
