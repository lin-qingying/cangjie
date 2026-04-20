package org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure

import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiProjectStructureTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.ExpectedCaModuleShape
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedBinaryArtifactModuleShape
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedContextModuleName
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedOriginalModuleName
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedPrimaryModuleShape
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedResolvable
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull

/**
 * 对齐 Kotlin Analysis API `projectStructure/moduleKind` 层的抽象测试。
 *
 * 这一层验证的不是某个语义组件，而是测试框架和平台桥接是否把测试数据中的模块声明
 * 稳定映射成 `CaModule` 家族、binary artifact view 与 auxiliary modules。
 */
abstract class AbstractModuleStructureTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = listOf(AnalysisApiProjectStructureTestDirectives)

    override fun doTestByMainModuleAndOptionalMainFile(
        mainFile: CjFile?,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val directives = mainModule.testModule.directives
        val primaryModule = mainModule.caModule
        val allModulePlatforms = mainModule.allCaModules.map { it.targetPlatform }.distinct()

        assertEquals(
            directives.expectedPrimaryModuleShape,
            primaryModule.toExpectedShape(),
            "主模块暴露出的 Analysis API 模块形状不正确。",
        )
        assertEquals(
            directives.expectedResolvable,
            primaryModule.isResolvable,
            "主模块的可解析性与测试数据声明不一致。",
        )

        val binaryArtifactModule = mainModule.binaryArtifactModule
        val expectedBinaryShape = directives.expectedBinaryArtifactModuleShape
        if (expectedBinaryShape == null) {
            assertNull(binaryArtifactModule, "当前场景不应暴露 binary artifact module。")
        } else {
            assertNotNull(binaryArtifactModule, "当前场景应暴露 binary artifact module。")
            assertEquals(
                expectedBinaryShape,
                binaryArtifactModule!!.toExpectedShape(),
                "binary artifact module 的 Analysis API 形状不正确。",
            )
        }

        val expectedAuxiliaryShapes = directives[AnalysisApiProjectStructureTestDirectives.EXPECTED_AUXILIARY_MODULE_SHAPE]
            .sortedBy(ExpectedCaModuleShape::name)
        val actualAuxiliaryShapes = mainModule.auxiliaryModules
            .map(CaModule::toExpectedShape)
            .sortedBy(ExpectedCaModuleShape::name)
        assertEquals(
            expectedAuxiliaryShapes,
            actualAuxiliaryShapes,
            "auxiliary modules 的公开形状集合不正确。",
        )

        val expectedRegularDependencies = directives[AnalysisApiProjectStructureTestDirectives.EXPECTED_DIRECT_REGULAR_DEPENDENCY]
            .sorted()
        val actualRegularDependencies = primaryModule.directRegularDependencies
            .map(::presentableModuleName)
            .sorted()
        assertEquals(
            expectedRegularDependencies,
            actualRegularDependencies,
            "主模块 direct regular dependencies 不符合模块图约束。",
        )

        val expectedFriendDependencies = directives[AnalysisApiProjectStructureTestDirectives.EXPECTED_DIRECT_FRIEND_DEPENDENCY]
            .sorted()
        val actualFriendDependencies = primaryModule.directFriendDependencies
            .map(::presentableModuleName)
            .sorted()
        assertEquals(
            expectedFriendDependencies,
            actualFriendDependencies,
            "主模块 direct friend dependencies 不符合模块图约束。",
        )

        directives.expectedContextModuleName?.let { expectedContextModuleName ->
            val danglingFileModule = primaryModule as? CaDanglingFileModule
                ?: error("仅 DanglingFileModule 场景允许声明 EXPECTED_CONTEXT_MODULE。")
            assertEquals(
                expectedContextModuleName,
                presentableModuleName(danglingFileModule.contextModule),
                "dangling file 模块绑定的 context module 不正确。",
            )
        }

        directives.expectedOriginalModuleName?.let { expectedOriginalModuleName ->
            val notUnderContentRootModule = primaryModule as? CaNotUnderContentRootModule
                ?: error("仅 NotUnderContentRootModule 场景允许声明 EXPECTED_ORIGINAL_MODULE。")
            assertEquals(
                expectedOriginalModuleName,
                notUnderContentRootModule.originalModule?.let(::presentableModuleName),
                "not-under-content-root 模块绑定的 original module 不正确。",
            )
        }

        assertEquals(
            1,
            allModulePlatforms.size,
            "同一测试模块导出的所有 Analysis API 模块应共享同一 target platform。",
        )
    }
}

private fun CaModule.toExpectedShape(): ExpectedCaModuleShape = when (this) {
    is CaDanglingFileModule -> ExpectedCaModuleShape.DanglingFileModule
    is CaLibrarySourceModule -> ExpectedCaModuleShape.LibrarySourceModule
    is CaLibraryFallbackDependenciesModule -> ExpectedCaModuleShape.LibraryFallbackDependenciesModule
    is CaBuiltinsModule -> ExpectedCaModuleShape.BuiltinsModule
    is CaLibraryModule -> ExpectedCaModuleShape.LibraryBinaryModule
    is CaNotUnderContentRootModule -> ExpectedCaModuleShape.NotUnderContentRootModule
    is CaSourceModule -> ExpectedCaModuleShape.SourceModule
    else -> error("Unsupported CaModule implementation in project structure test: ${this::class.qualifiedName}")
}

/**
 * 为项目结构断言提供稳定的模块名渲染。
 *
 * 测试数据只应依赖公开模块语义，而不应依赖具体实现类的 `toString()` 偶然值。
 */
private fun presentableModuleName(module: CaModule): String = when (module) {
    is CaDanglingFileModule -> module.file.name
    is CaLibrarySourceModule -> module.libraryName
    is CaLibraryFallbackDependenciesModule -> "${module.dependencyOwnerName}.fallback"
    is CaBuiltinsModule -> module.builtinsName
    is CaLibraryModule -> module.libraryName
    is CaNotUnderContentRootModule -> module.name
    is CaSourceModule -> module.name
    else -> error("Unsupported CaModule implementation in project structure test: ${module::class.qualifiedName}")
}
