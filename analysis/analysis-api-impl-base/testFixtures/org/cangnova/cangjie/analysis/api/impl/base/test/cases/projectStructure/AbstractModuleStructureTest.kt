package org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure

import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
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
    /**
     * 当前模块结构测试额外注册的项目结构指令。
     *
     * 模块结构测试不继承组件级目标定位指令，只需要模块级 expected shape、dependency 和 resolvable 断言。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = listOf(AnalysisApiProjectStructureTestDirectives)

    /**
     * 执行主模块及其辅助模块形状断言。
     *
     * 方法从测试模块模型中读取期望指令，并逐项比较主模块、binary artifact view、auxiliary modules、
     * direct regular/friend dependencies 以及 dangling/not-under-content-root 上下文绑定。
     */
    override fun doTestByMainModuleAndOptionalMainFile(
        mainFile: CjFile?,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val directives = mainModule.testModule.directives
        val primaryModule = mainModule.caModule

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
        val actualAuxiliaryShapes = primaryModule.directRegularDependencies
            .mapNotNull(::toAuxiliaryShapeOrNull)
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

    }
}

/**
 * 将公开 `CaModule` 实例映射为测试数据中使用的模块形状枚举。
 *
 * 该转换只暴露模块语义类别，避免 golden 依赖具体实现类名。
 */
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
 * 将 direct regular dependency 中的辅助模块映射为期望形状。
 *
 * 非辅助模块返回 `null`，由调用方过滤后只比较 builtins 与 library fallback 等辅助依赖。
 */
private fun toAuxiliaryShapeOrNull(module: CaModule): ExpectedCaModuleShape? = when (module) {
    is CaBuiltinsModule -> ExpectedCaModuleShape.BuiltinsModule
    is CaLibraryFallbackDependenciesModule -> ExpectedCaModuleShape.LibraryFallbackDependenciesModule
    else -> null
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
