package org.cangnova.cangjie.analysis.light.declarations.symbol

import com.intellij.openapi.application.ApplicationManager
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightExtendDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.documentation
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.api.util.requireIsInstance
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定 `analysis:symbol-light-declarations` 模块自有契约。
 *
 * 这里不再借 `analysis-api-cfir` 做外围集成兜底，而是直接验证 provider 自己负责的三条链路：
 * 1. source PSI -> symbol -> light declaration 的结构与缓存；
 * 2. source-backed declaration 的 documentation 恢复；
 * 3. decompiled/builtins PSI -> light declaration 的树形与来源稳定性。
 */
class CaSymbolLightDeclarationProviderTest : AbstractAnalysisApiExecutionTest(
    "analysis/symbol-light-declarations/testData/provider",
) {
    /**
     * 使用 standalone CFIR Analysis API 测试环境。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册 symbol-light-declarations provider 的测试服务。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(SymbolLightDeclarationsTestServiceRegistrar)

    /**
     * 验证源码文件中的 class-like、typealias、extend 和 callable light declarations。
     */
    @Test
    fun sourceLightDeclarations(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val declarations = provider.getLightDeclarations(mainFile, mainModule.caModule)

        val document = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { it.name == "Document" }
        val alias = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { it.name == "DocAlias" }
        val extend = declarations.single { declaration -> declaration.name == "Document" && declaration.kind.name == "EXTEND" }
        requireIsInstance<CaLightExtendDeclaration>(extend)
        val topLevelCallable = declarations.filterIsInstance<CaLightCallableDeclaration>().single { it.name == "topLevel" }

        assertEquals(CaLightDeclarationOriginKind.SOURCE_PSI, document.origin.kind)
        assertEquals("sample/symbol/light/Document", document.classId?.asString())
        assertTrue(document.members.filterIsInstance<CaLightCallableDeclaration>().any { it.name == "member" })

        assertEquals(CaLightDeclarationOriginKind.SOURCE_PSI, alias.origin.kind)
        assertEquals("sample/symbol/light/DocAlias", alias.classId?.asString())
        assertTrue(alias.members.isEmpty(), "typealias light declaration 不应伪造成员树")

        assertEquals(CaLightDeclarationOriginKind.SOURCE_PSI, extend.origin.kind)
        assertEquals("sample/symbol/light/Document", extend.targetClassId?.asString())
        assertTrue(extend.members.filterIsInstance<CaLightCallableDeclaration>().any { it.name == "prettyPrint" })

        assertEquals("sample/symbol/light/topLevel", topLevelCallable.callableId?.toString())

        analyzeForTest(mainFile) {
            assertEquals("Source top level doc.", documentation(topLevelCallable))

            val classSymbolLightDeclaration = provider.getLightDeclaration(mainFile.declarations
                .filterIsInstance<org.cangnova.cangjie.psi.CjTypeStatement>()
                .single { declaration -> declaration !is org.cangnova.cangjie.psi.CjExtend && declaration.name == "Document" }
                .classSymbol)
            assertNotNull(classSymbolLightDeclaration)
            assertEquals(document.classId, (classSymbolLightDeclaration as CaLightClassLikeDeclaration).classId)
        }
    }

    /**
     * 验证同一个 light declaration 实例上的成员树会稳定缓存。
     */
    @Test
    fun sourceLightDeclarationCaching(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val declarations = provider.getLightDeclarations(mainFile, mainModule.caModule)
        val documentDeclaration = mainFile.declarations
            .filterIsInstance<org.cangnova.cangjie.psi.CjTypeStatement>()
            .single { declaration -> declaration !is org.cangnova.cangjie.psi.CjExtend && declaration.name == "Document" }

        analyzeForTest(mainFile) {
            val lightBySymbol = provider.getLightDeclaration(documentDeclaration.classSymbol)
            assertNotNull(lightBySymbol)

            val fileViewDocument = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { it.name == "Document" }
            val fileViewMember = fileViewDocument.members.filterIsInstance<CaLightCallableDeclaration>().single { it.name == "member" }

            val firstMembersRead = fileViewDocument.members
            val secondMembersRead = fileViewDocument.members
            assertSame(firstMembersRead, secondMembersRead, "成员树应在同一 light declaration 上稳定缓存")
            assertSame(fileViewMember, secondMembersRead.filterIsInstance<CaLightCallableDeclaration>().single { it.name == "member" })

            val symbolViewDocument = lightBySymbol as CaLightClassLikeDeclaration
            assertSame(symbolViewDocument.members, symbolViewDocument.members, "symbol light declaration 成员树也应稳定缓存")
        }
    }

    /**
     * 验证 builtins 反编译文件可以投影为带 decompiled 来源的 light declarations。
     */
    @Test
    fun decompiledBuiltinsLightDeclarations(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val decompiledFile = ApplicationManager.getApplication().runWriteAction<CjFile?> {
            val binaryIndex = mainFile.project.getService(CaDecompiledBinaryIndex::class.java)
            val builtinFiles = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()
            val binaryFile = binaryIndex.findBuiltinsBinaryFile(FqName("std.objectpool"))
                ?: builtinFiles.firstOrNull { virtualFile ->
                    virtualFile.name.equals("std.objectpool.cjo", ignoreCase = true) &&
                        binaryIndex.readPackageFqName(virtualFile) == FqName("std.objectpool")
                }
                ?: error("Cannot find std.objectpool builtins binary")
            PsiManager.getInstance(mainFile.project).findFile(binaryFile) as? CjFile
        }

        assertNotNull(decompiledFile)

        val declarations = ApplicationManager.getApplication().runWriteAction<List<CaLightDeclaration>> {
            provider.getLightDeclarations(decompiledFile!!, mainModule.caModule)
        }

        val objectPool = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { it.name == "ObjectPool" }
        assertEquals(CaLightDeclarationOriginKind.DECOMPILED_PSI, objectPool.origin.kind)
        assertTrue(objectPool.origin.containingFile?.isCompiled == true)
        assertTrue(objectPool.members.isNotEmpty(), "decompiled light declaration 应保留成员树")
        assertNotNull(objectPool.origin.sourceElement, "decompiled light declaration 应保留 decompiled PSI 源元素")

        analyzeForTest(mainFile) {
            assertNull(documentation(objectPool), "decompiled builtins 在当前边界下没有真实 CDoc，应稳定返回 null")
        }
    }
}

/**
 * symbol-light-declarations 测试使用的 Analysis API 服务注册器。
 */
internal object SymbolLightDeclarationsTestServiceRegistrar : org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar() {
    /**
     * 测试加载的 plugin XML 路径。
     */
    private const val SYMBOL_LIGHT_DECLARATIONS_PLUGIN_XML = "META-INF/analysis-api/cangjie-symbol-light-declarations.xml"

    /**
     * 注册应用级反编译与 symbol-light-declarations 服务。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        CaAnalysisApiDecompiledTestServiceRegistrar.registerApplicationServices(application)
        PluginStructureProvider.registerApplicationServices(application, SYMBOL_LIGHT_DECLARATIONS_PLUGIN_XML)
    }

    /**
     * 注册项目级反编译与 symbol-light-declarations 服务。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        CaAnalysisApiDecompiledTestServiceRegistrar.registerProjectServices(project)
        PluginStructureProvider.registerProjectServices(project, SYMBOL_LIGHT_DECLARATIONS_PLUGIN_XML)
    }
}
