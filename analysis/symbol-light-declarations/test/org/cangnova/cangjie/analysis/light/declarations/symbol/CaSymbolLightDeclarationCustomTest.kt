package org.cangnova.cangjie.analysis.light.declarations.symbol

import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `analysis:symbol-light-declarations` 自定义场景测试。
 *
 * 对齐 Kotlin `SymbolLightClassesCustomTest` 的定位：锁定 provider 在非生成
 * golden 用例下必须维持的语义契约。当前覆盖三条已确认链路：
 * 1. enum 声明经 symbol view 投影时，枚举条目（构造器）应作为 callable 成员出现；
 * 2. typealias 的 light declaration 只保留"类型别名"形态，不伪造成员树；
 * 3. file view 投影的每个顶层声明都归属 use-site module，且 origin 可回指源 PSI。
 */
class CaSymbolLightDeclarationCustomTest : AbstractAnalysisApiExecutionTest(
    "analysis/symbol-light-declarations/testData/custom",
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
     * enum 条目在 symbol view 中应投影为 callable 成员（对齐 Kotlin enum entry 场景）。
     */
    @Test
    fun enumEntriesProjectedThroughSymbolView(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val enumDeclaration = mainFile.enumDeclaration("Color")

        analyzeForTest(mainFile) {
            val lightDeclaration = provider.getLightDeclaration(enumDeclaration.classSymbol)
                ?: error("Symbol view must project the enum declaration `Color`.")
            val enumLight = lightDeclaration as? CaLightClassLikeDeclaration
                ?: error("Enum light declaration must be class-like, but got `${lightDeclaration.kind}`.")

            assertEquals("Color", enumLight.name)
            assertTrue(enumLight.typeParameters.isEmpty(), "非泛型 enum 不应携带类型形参")

            val entryNames = enumLight.members
                .filterIsInstance<CaLightCallableDeclaration>()
                .mapNotNull { member -> member.name }
            assertTrue("Red" in entryNames, "enum 条目 `Red` 应投影为 callable 成员，实际成员: $entryNames")
            assertTrue("Green" in entryNames, "enum 条目 `Green` 应投影为 callable 成员，实际成员: $entryNames")
        }
    }

    /**
     * typealias 的 light declaration 不应伪造成员树，且 classId 稳定。
     */
    @Test
    fun typeAliasLightDeclarationShape(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val declarations = provider.getLightDeclarations(mainFile, mainModule.caModule)

        val alias = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { declaration ->
            declaration.name == "DocAlias"
        }

        assertEquals(CaLightDeclarationOriginKind.SOURCE_PSI, alias.origin.kind)
        assertEquals("sample/symbol/light/custom/DocAlias", alias.classId?.asString())
        assertTrue(alias.members.isEmpty(), "typealias light declaration 不应伪造成员树")
        assertTrue(alias.typeParameters.isEmpty(), "非泛型 typealias 不应携带类型形参")
    }

    /**
     * file view 投影的顶层声明应归属 use-site module，且 origin 回指源 PSI。
     */
    @Test
    fun lightDeclarationModuleAndOrigin(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val declarations = provider.getLightDeclarations(mainFile, mainModule.caModule)

        val document = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { declaration ->
            declaration.name == "Document"
        }
        val topLevel = declarations.filterIsInstance<CaLightCallableDeclaration>().single { declaration ->
            declaration.name == "topLevel"
        }

        for (declaration in listOf<CaLightDeclaration>(document, topLevel)) {
            assertSame(mainModule.caModule, declaration.module, "`${declaration.name}` 应归属 use-site module")
            assertEquals(CaLightDeclarationOriginKind.SOURCE_PSI, declaration.origin.kind)
            assertSame(mainFile, declaration.origin.containingFile, "`${declaration.name}` origin 应回指主文件")
            assertNotNull(declaration.origin.sourceElement, "`${declaration.name}` origin 应携带源 PSI 元素")
        }
    }
}

/**
 * 在文件顶层查找指定名称的 enum 声明。
 */
private fun CjFile.enumDeclaration(name: String): CjEnum {
    return declarations.filterIsInstance<CjEnum>().singleOrNull { declaration ->
        declaration.name == name
    } ?: error("Cannot find enum declaration `$name` in `${this.name}`.")
}
