@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.api.CaExperimentalApi::class,
)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.application.ReadAction
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.contextModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPsiFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 同一真实 LL 模块内核对宏输入字段、当前 source CFIR 和结构遍历，复用现有空 consumer fixture。 */
class MacroCurrentSourceFileStructureTest : AbstractAnalysisApiBasedTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun actualContextInputsMapToCanonicalFieldsAndAllDiagnosticStructures() = withSource { source, session ->
        val raw = session.moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(source)
        val klassPsi = source.declarations.filterIsInstance<CjClass>().single()
        val klass = raw.declarations.filterIsInstance<CfirClass>().single()
        val fieldsPsi = requireNotNull(klassPsi.body).declarations.filterIsInstance<CjFieldVariable>()
        val fields = klass.declarations.filterIsInstance<CfirFieldVariable>()
        assertEquals(listOf("firstCount", "secondCount"), fields.map { it.name.asString() })
        assertEquals(2, fieldsPsi.size)
        fields.zip(fieldsPsi).forEach { (field, psi) ->
            assertSame(psi, field.psi)
            assertSame(field, CfirElementFinder.findDeclaration(raw, psi))
        }
        val structure = FileStructure.build(source, session.moduleComponents)
        val elements = structure.getAllStructureElements()
        assertEquals(5, elements.size, "file、class、两个字段及 probe 函数都必须拥有正式结构")
        fields.zip(fieldsPsi).forEach { (field, psi) ->
            assertSame(field, structure.getStructureElementFor(psi).declaration)
            assertSame(field, structure.getStructureElementFor(psi).mappings.get(psi))
        }
        DiagnosticCollectionFailures.clearAll()
        structure.getAllDiagnosticsForFile(DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(emptyList<Throwable>(), DiagnosticCollectionFailures.drain(), "未知宏可以产生诊断，不能产生映射异常")
    }

    @Test
    fun unrecordedConstructionTreeNeverBecomesTheSourceStructureDeclaration() = withSource { source, session ->
        val construction = session.moduleComponents.cfirFileBuilder.buildRawCfirFileUncached(source)
        assertNull(session.macroSemanticSnapshot)
        val active = session.moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(source)
        assertNotSame(construction, active)
        val sourceClass = source.declarations.filterIsInstance<CjClass>().single()
        val activeClass = active.declarations.filterIsInstance<CfirClass>().single()
        val constructionClass = construction.declarations.filterIsInstance<CfirClass>().single()
        assertSame(active, session.moduleComponents.cache.getContainerCfirFile(activeClass))
        assertNull(session.moduleComponents.cache.getContainerCfirFile(constructionClass), "未发布 construction 不能取得 canonical source file 归属")
        assertNull(session.cfirProvider.getCfirClassifierContainerFileIfAny(constructionClass.symbol), "symbol container 查询必须保留同一实际树的所有权")
        val structure = FileStructure.build(source, session.moduleComponents)
        assertSame(activeClass, structure.getStructureElementFor(sourceClass).declaration)
        assertNotSame(constructionClass, structure.getStructureElementFor(sourceClass).declaration)
        assertSame(active, session.moduleComponents.cache.getCachedCfirFile(source))
        assertNull(session.macroSemanticSnapshot, "未经过 Recordable/publication 的 carrier 不能成为最终视图")
    }

    @Test
    fun sameTextAndFunctionNameCannotRecoverADeclarationFromAnotherPsiTree() = withSource { source, session ->
        val active = session.moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(source)
        val actualPsi = source.declarations.filterIsInstance<CjNamedFunction>().single()
        val actual = active.declarations.filterIsInstance<CfirNamedFunction>().single()
        val copy = CjPsiFactory(source.project).createFile("sameText.cj", source.text)
        copy.contextModule = source.contextModule
        val copiedPsi = copy.declarations.filterIsInstance<CjNamedFunction>().single()
        assertEquals(actualPsi.name, copiedPsi.name)
        assertEquals(actualPsi.text, copiedPsi.text)
        assertSame(actual, CfirElementFinder.findDeclaration(active, actualPsi))
        assertNull(CfirElementFinder.findDeclaration(active, copiedPsi), "名字、ClassId 或 offset 不能替代 PSI identity")
    }

    private fun withSource(block: (CjFile, LLCfirResolvableModuleSession) -> Unit) {
        runTest("analysis/low-level-api-cfir/testData/diagnostics/cjoImportedModuleIdentity.cj") { services ->
            val mainFile = requireNotNull(findMainFileAndModule(services).mainFile)
            ReadAction.run<RuntimeException> {
                val projectStructure = CangJieProjectStructureProvider.getInstance(mainFile.project)
                val contextModule = projectStructure.getModule(mainFile, null)
                val source = CjPsiFactory(mainFile.project).createFile("contextInputs.cj", SOURCE)
                source.contextModule = contextModule
                // contextModule 只提供语义上下文；新 PSI 的实际模块决定其 source session 所有者。
                val sourceModule = projectStructure.getModule(source, contextModule)
                val session = LLCfirSessionCache.getInstance(mainFile.project).getSession(sourceModule) as LLCfirResolvableModuleSession
                assertEquals(sourceModule, session.moduleComponents.module)
                block(source, session)
            }
        }
    }

    private companion object {
        val SOURCE = """
            package untitled89
            @ContextOuter
            class ContextMacroProbe {
                @ContextInner
                var firstCount: Int64 = 40
                @ContextInner
                var secondCount: Int64 = 2
            }
            func probe(): Int64 { return 42 }
        """.trimIndent()
    }
}
