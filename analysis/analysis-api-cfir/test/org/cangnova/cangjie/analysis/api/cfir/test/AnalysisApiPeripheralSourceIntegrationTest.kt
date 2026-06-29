package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.stubs.CaStubIndexFacade
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.analysis.test.services.environmentManager
import org.cangnova.cangjie.analysis.tools.CaAnalysisInspectorTools
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定外围分析模块在 source-backed module 上的协同行为。
 *
 * 这组回归测试不只检查单个服务能否被定位，
 * 而是同时覆盖：
 * 1. `analysis:stubs` 的文件/包/成员索引；
 * 2. `analysis:symbol-light-declarations` 的声明视图构建；
 * 3. `analysis:analysis-tools` 的统一 dump/inspector 入口。
 */
class AnalysisApiPeripheralSourceIntegrationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/peripheralModules",
) {
    /**
     * 使用 standalone CFIR 配置验证外围 source-backed 模块协同能力。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证普通源码模块上的 stub index、light declaration 与 inspector dump 视图。
     */
    @Test
    fun sourceViews(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertEquals(TestModuleKind.Source, mainModule.moduleKind)
        assertPeripheralServices(
            mainFile = mainFile,
            mainModule = mainModule,
            testServices = testServices,
            expectedClassName = "Greeter",
            expectedCallableName = "topLevel",
            expectedMemberName = "member",
        )
    }

    /**
     * 验证 library source 模块上的外围服务视图与普通 source 模块保持同构。
     */
    @Test
    fun librarySourceViews(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertEquals(TestModuleKind.LibrarySource, mainModule.moduleKind)
        assertPeripheralServices(
            mainFile = mainFile,
            mainModule = mainModule,
            testServices = testServices,
            expectedClassName = "LibraryGreeter",
            expectedCallableName = "exported",
            expectedMemberName = "member",
        )
    }

    /**
     * 验证 source-backed light declaration provider 会保留同名顶层函数重载的独立签名。
     */
    @Test
    fun sourceOverloadedLightDeclarations(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val overloadDeclarations = provider.getLightDeclarations(mainFile, mainModule.caModule)
            .filterIsInstance<CaLightCallableDeclaration>()
            .filter { it.name == "overload" }

        assertEquals(2, overloadDeclarations.size, "light declaration provider 应保留两个顶层重载函数。")
        assertTrue(
            overloadDeclarations[0] !== overloadDeclarations[1],
            "两个重载函数不能复用同一个 light declaration 实例。",
        )
        assertEquals(
            2,
            overloadDeclarations.mapNotNull { declaration -> declaration.signature }.toSet().size,
            "两个重载函数必须保留不同的 signature。",
        )
    }

    /**
     * 以统一断言锁定 source / library source 两种 source-backed module 的行为一致性。
     */
    private fun assertPeripheralServices(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
        expectedClassName: String,
        expectedCallableName: String,
        expectedMemberName: String,
    ) {
        val project = testServices.environmentManager.getProject()
        val stubIndexFacade = CaStubIndexFacade.getInstance(project)
        val lightDeclarationProvider = CaLightDeclarationProvider.getInstance(project)
        val inspectorTools = CaAnalysisInspectorTools(project)
        val packageFqName = mainFile.packageFqName

        val fileClassifierNames = stubIndexFacade.fileProvider.getTopLevelClassifierNames(mainFile).map(Name::asString).toSet()
        val fileCallableNames = stubIndexFacade.fileProvider.getTopLevelCallableNames(mainFile).map(Name::asString).toSet()
        assertEquals(
            setOf(expectedClassName),
            fileClassifierNames,
            "stub 文件视图应稳定暴露顶层 class-like 声明。",
        )
        assertEquals(
            setOf(expectedCallableName),
            fileCallableNames,
            "stub 文件视图应稳定暴露顶层 callable 声明。",
        )

        val packageClassifierNames = stubIndexFacade.packageIndex.getTopLevelClassifierNames(packageFqName).map(Name::asString).toSet()
        val packageCallableNames = stubIndexFacade.packageIndex.getTopLevelCallableNames(packageFqName).map(Name::asString).toSet()
        assertEquals(setOf(expectedClassName), packageClassifierNames)
        assertEquals(setOf(expectedCallableName), packageCallableNames)

        val classMemberDump = inspectorTools.dumpClassMemberStubNames(
            ClassId(packageFqName, Name.identifier(expectedClassName)),
        )
        assertTrue(
            classMemberDump.contains(expectedMemberName),
            "class member stub 索引应包含成员 `$expectedMemberName`。actual=$classMemberDump",
        )

        val fileLightDeclarations = lightDeclarationProvider.getLightDeclarations(mainFile, mainModule.caModule)
        val classLightDeclaration = fileLightDeclarations
            .filterIsInstance<CaLightClassLikeDeclaration>()
            .singleOrNull { it.name == expectedClassName }
        assertNotNull(
            classLightDeclaration,
            "light declaration provider 应返回顶层 class-like 视图。",
        )
        assertTrue(
            classLightDeclaration!!.members.filterIsInstance<CaLightCallableDeclaration>().any { it.name == expectedMemberName },
            "light declaration 的成员视图应保留 `$expectedMemberName`。",
        )
        assertTrue(
            fileLightDeclarations.filterIsInstance<CaLightCallableDeclaration>().any { it.name == expectedCallableName },
            "light declaration provider 应返回顶层 callable 视图。",
        )

        val moduleLightDump = inspectorTools.dumpLightDeclarations(mainModule.caModule)
        assertTrue(moduleLightDump.contains(expectedClassName))
        assertTrue(moduleLightDump.contains(expectedCallableName))
        assertTrue(moduleLightDump.contains(expectedMemberName))

        val fileStubDump = inspectorTools.dumpStubFile(mainFile)
        assertTrue(fileStubDump.contains("topLevelClassifiers=[$expectedClassName]"))
        assertTrue(fileStubDump.contains("topLevelCallables=[$expectedCallableName]"))

        val packageStubDump = inspectorTools.dumpStubPackage(packageFqName)
        assertTrue(packageStubDump.contains(expectedClassName))
        assertTrue(packageStubDump.contains(expectedCallableName))

        val fileLightDump = inspectorTools.dumpLightDeclarations(mainFile, mainModule.caModule)
        assertTrue(fileLightDump.contains(expectedClassName))
        assertTrue(fileLightDump.contains(expectedCallableName))
        assertTrue(fileLightDump.contains(expectedMemberName))
    }
}
