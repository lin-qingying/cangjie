@file:OptIn(
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.api.CaExperimentalApi::class,
)

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaDanglingFileModuleImpl
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.explicitModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirFileSourceGeneration
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.compilationParsingResultOrNull
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import javax.swing.SwingUtilities

/** 模块的编译配置跨 session 退休与重建保持；不通过临时注入或强持有弱缓存改变生产生命周期。 */
class CfirCjmpModuleModeLifecycleTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/cjmpModuleModeLifecycle",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    /** 异常配置变更不能继续命中旧 raw 属性；恢复原组件后同一来源仍命中原代次。 */
    @Test
    fun cachedRawRejectsDifferentSessionParsingContext(mainFile: CjFile) {
        ReadAction.run<RuntimeException> {
            val facade = mainFile.getResolutionFacadeForTest()
            val file = mainFile.getOrBuildCfirFile(facade)
            val session = file.moduleData.session
            val original = session.cjmpSettings
            session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.NONE))
            try {
                assertThrows(IllegalStateException::class.java) { mainFile.getOrBuildCfirFile(facade) }
            } finally {
                session.register(CfirCjmpSettingsComponent::class, original)
            }
            assertSame(file, mainFile.getOrBuildCfirFile(facade))
        }
    }

    @Test
    fun cjmpModeSurvivesSessionRecreation(testServices: TestServices) {
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val expectedMode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            assertRecreatedModule(module, expectedMode)
        }
    }

    /** 无显式模式时按真实图推导；同时有父模块和实现模块时 SPECIFIC 优先。 */
    @Test
    fun implicitModuleRolesFollowRefinementGraph(testServices: TestServices) {
        val expected = mapOf("isolated" to CfirCjmpMode.NONE, "common" to CfirCjmpMode.COMMON,
            "bridge" to CfirCjmpMode.SPECIFIC, "specific" to CfirCjmpMode.SPECIFIC)
        for (module in testServices.cjTestModuleStructure.mainModules) {
            assertNull((module.caModule as CaSourceModule).cjmpCompilationMode)
            assertRecreatedModule(module, expected.getValue(module.name))
        }
    }

    /** 外来代次在任何 owner/cache 发布前被拒绝，消费缓存仍为空。 */
    @Test
    fun foreignSessionIsRejectedBeforeCachePublication(testServices: TestServices) {
        val common = testServices.cjTestModuleStructure.mainModules.single {
            it.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() == CfirCjmpMode.COMMON
        }
        val file = common.cjFiles.single()
        val owner = ReadAction.compute<LLCfirSourcesSession, RuntimeException> {
            val raw = file.getOrBuildCfirFile(file.getResolutionFacadeForTest())
            raw.moduleData.session as LLCfirSourcesSession
        }
        val foreign = ReadAction.compute<LLCfirFileSourceGeneration, RuntimeException> {
            requireNotNull(owner.moduleComponents.cache.getFileSourceForModification(file))
        }
        val sessions = LLCfirSessionCache.getInstance(common.caModule.project)
        SwingUtilities.invokeAndWait { WriteAction.run<RuntimeException> { sessions.storage.sourceCache.remove(common.caModule) } }
        ReadAction.run<RuntimeException> {
            val consumer = sessions.getSession(common.caModule) as LLCfirSourcesSession
            assertNotSame(owner, consumer)
            assertEquals(owner.moduleData, consumer.moduleData, "同一模块重建的不同 session 代次")
            assertEquals(owner.cjmpSettings.mode, consumer.cjmpSettings.mode)
            foreign.product.requireConsistentInput()
            assertEquals(foreign.product.actualInputSeed.cjmpContext,
                consumer.cjmpSettings.toParsingContext(consumer.languageVersionSettings),
                "语境相同才能隔离 session 身份判据")
            val cache = consumer.moduleComponents.cache
            val before = cache.getAllCachedCfirFilesForResolution().toList()
            assertThrows(IllegalStateException::class.java) { cache.fileCached(file) { foreign } }
            assertNull(cache.getCachedCfirFile(file))
            assertEquals(before, cache.getAllCachedCfirFilesForResolution().toList())
        }
    }

    /** context 与 dangling 都重建后继续读取相同编译模式，同时保留各自独立模块和文件。 */
    @Test
    fun danglingModeFollowsRecreatedContext(testServices: TestServices) {
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val expected = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val cache = LLCfirSessionCache.getInstance(module.caModule.project)
            lateinit var file: CjFile
            lateinit var danglingModule: CaDanglingFileModuleImpl
            lateinit var context: LLCfirSession
            lateinit var dangling: LLCfirDanglingFileSession
            lateinit var previousRaw: CfirFile
            ReadAction.run<RuntimeException> {
                context = cache.getSession(module.caModule)
                file = CjPsiFactory(module.caModule.project).createFile("floating.cj", "package floating\nfunc probe(): Int64 { 1 }")
                danglingModule = CaDanglingFileModuleImpl(listOf(file), module.caModule, CaDanglingFileResolutionMode.PREFER_SELF)
                file.explicitModule = danglingModule
                dangling = cache.getSession(danglingModule) as LLCfirDanglingFileSession
                assertEquals(expected, dangling.cjmpSettings.mode)
                assertNotSame(context.moduleData, dangling.moduleData)
                assertSame(danglingModule, dangling.caModule)
                previousRaw = dangling.moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(file)
                assertRawContext(previousRaw, dangling, expected)
            }
            SwingUtilities.invokeAndWait {
                WriteAction.run<RuntimeException> {
                    cache.storage.danglingFileSessionCache.remove(danglingModule)
                    cache.storage.unstableDanglingFileSessionCache.remove(danglingModule)
                    cache.storage.sourceCache.remove(module.caModule)
                }
            }
            assertFalse(context.isValid)
            assertFalse(dangling.isValid)
            ReadAction.run<RuntimeException> {
                val nextContext = cache.getSession(module.caModule)
                val next = cache.getSession(danglingModule) as LLCfirDanglingFileSession
                assertNotSame(context, nextContext)
                assertNotSame(dangling, next)
                assertEquals(expected, next.cjmpSettings.mode)
                val nextRaw = next.moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(file)
                assertNotSame(previousRaw, nextRaw)
                assertRawContext(nextRaw, next, expected)
            }
        }
    }

    private fun assertRecreatedModule(module: CjTestModule, expectedMode: CfirCjmpMode) {
        val cache = LLCfirSessionCache.getInstance(module.caModule.project)
        val previous = ReadAction.compute<LLCfirSession, RuntimeException> { cache.getSession(module.caModule) }
        assertEquals(expectedMode, previous.cjmpSettings.mode)
        val previousRaw = ReadAction.compute<CfirFile, RuntimeException> {
            val psi = module.cjFiles.single()
            psi.getOrBuildCfirFile(psi.getResolutionFacadeForTest()).also { assertRawContext(it, previous, expectedMode) }
        }
        SwingUtilities.invokeAndWait { WriteAction.run<RuntimeException> { cache.storage.sourceCache.remove(module.caModule) } }
        assertFalse(previous.isValid, "退出 cache 的 session 必须被真实退休")
        val recreated = ReadAction.compute<LLCfirSession, RuntimeException> { cache.getSession(module.caModule) }
        assertNotSame(previous, recreated)
        assertEquals(expectedMode, recreated.cjmpSettings.mode)
        ReadAction.run<RuntimeException> {
            val psi = module.cjFiles.single()
            val file = psi.getOrBuildCfirFile(psi.getResolutionFacadeForTest())
            assertNotSame(previousRaw, file)
            assertRawContext(file, recreated, expectedMode)
        }
    }

    private fun assertRawContext(file: CfirFile, session: LLCfirSession, expected: CfirCjmpMode) {
        assertSame(session, file.moduleData.session)
        val product = requireNotNull((file.psi as CjFile).compilationParsingResultOrNull())
        val context = product.actualInputSeed.cjmpContext as CjmpParsingContext.Compilation
        assertEquals(expected == CfirCjmpMode.COMMON, context.compileCommon)
        assertEquals(expected == CfirCjmpMode.SPECIFIC, context.compileSpecific)
    }
}
