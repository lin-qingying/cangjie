package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjEnumPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReturnExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * enum pattern 的 constructor 引用回归：
 * - `case Some(v)` / `case None` 必须解析到 enum constructor 符号（导航/快速文档的前提）；
 * - 未知 constructor 名字报 `UNRESOLVED_REFERENCE`，不再是 `PATTERN_NOT_MATCH`；
 * - 裸名字 `None1` 仍是 binding pattern，不应产生 unresolved 诊断。
 */
class PatternConstructorResolutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = AnalysisSessionMode.Normal,
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    @Test
    fun qualifiedEnumPattern(mainFile: CjFile) {
        val (symbols, diagnostics) = analyzeForTest(mainFile) {
            val names = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            fun symbolsOf(text: String, parentType: Class<*>): List<String> =
                names.filter { name -> name.text == text && parentType.isInstance(name.parent) }
                    .flatMap { name -> name.resolveToSymbols() }
                    .map { symbol -> symbol::class.simpleName ?: "?" }

            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }

            mapOf(
                "qualifiedA" to symbolsOf("A", org.cangnova.cangjie.psi.CjDotQualifiedExpression::class.java),
                "qualifiedB" to symbolsOf("B", org.cangnova.cangjie.psi.CjDotQualifiedExpression::class.java),
                "qualifiedMissing" to symbolsOf("Missing", org.cangnova.cangjie.psi.CjDotQualifiedExpression::class.java),
            ) to diagnostics
        }

        assertTrue(
            symbols.getValue("qualifiedA").any { it.contains("EnumConstructor") },
            "`Local.A` 的 selector 必须解析为 enum constructor 符号：${symbols["qualifiedA"]}",
        )
        assertTrue(
            symbols.getValue("qualifiedB").any { it.contains("EnumConstructor") },
            "`Local.B` 的 selector 必须解析为 enum constructor 符号：${symbols["qualifiedB"]}",
        )
        assertTrue(
            symbols.getValue("qualifiedMissing").isEmpty(),
            "`Local.Missing` 不应产生可导航符号：${symbols["qualifiedMissing"]}",
        )
        assertEquals(
            1,
            diagnostics.count { it == "UNRESOLVED_REFERENCE" },
            "限定形式的未知 constructor 只能报一次 UNRESOLVED_REFERENCE：$diagnostics",
        )
        assertTrue(
            diagnostics.none { it == "PATTERN_NOT_MATCH" },
            "限定形式的未知 constructor 不应报 PATTERN_NOT_MATCH：$diagnostics",
        )
    }

    /**
     * 裸名字是别的 enum 的 constructor（官方 `FindEnumPatternTarget`：目标只在 subject enum 内查找）：
     * - `case Green`（`Green` 属于 `Color`，subject 是 `Shape`）报一次 `UNRESOLVED_REFERENCE`，
     *   且不得导航到 `Color.Green`——否则诊断被吞掉、导航和 Find Usages 还会指向无关的 enum；
     * - 两个 enum 的同名 constructor 按各自的 subject enum 定位。
     */
    @Test
    fun foreignEnumPattern(mainFile: CjFile) {
        val (owners, diagnostics) = analyzeForTest(mainFile) {
            val names = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            // 每个出现位置解析到的 constructor 所属 enum；按源码顺序排列。
            fun ownersOf(text: String): List<List<String>> =
                names.filter { name -> name.text == text && name.parent is CjVarOrEnumPattern }
                    .map { name ->
                        name.resolveToSymbols().map { symbol ->
                            (symbol as? CaEnumConstructorSymbol)?.containingEnumClassId?.shortClassName?.asString()
                                ?: (symbol::class.simpleName ?: "?")
                        }
                    }

            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName to diagnostic.psi.text }

            mapOf(
                "Green" to ownersOf("Green"),
                "Red" to ownersOf("Red"),
                "Circle" to ownersOf("Circle"),
                "Square" to ownersOf("Square"),
                "Dot" to ownersOf("Dot"),
            ) to diagnostics
        }

        assertEquals(
            listOf(emptyList<String>()),
            owners["Green"],
            "`case Green` 的 subject 是 Shape，不得导航到 Color.Green",
        )
        assertEquals(
            listOf(emptyList<String>()),
            owners["Red"],
            "`Wrap(Red)` 的 payload 类型是 Shape，不得导航到 Color.Red",
        )
        assertEquals(
            listOf(listOf("Shape"), listOf("Token")),
            owners["Circle"],
            "同名 constructor 必须按各自的 subject enum 定位",
        )
        assertEquals(listOf(listOf("Shape")), owners["Square"])
        assertEquals(listOf(listOf("Token")), owners["Dot"])

        assertEquals(
            listOf("UNRESOLVED_REFERENCE" to "Green", "UNRESOLVED_REFERENCE" to "Red"),
            diagnostics.filter { (factory, _) -> factory == "UNRESOLVED_REFERENCE" }.sortedBy { it.second },
            "两个外部 constructor 各报一次 UNRESOLVED_REFERENCE：$diagnostics",
        )
        assertTrue(
            diagnostics.none { (factory, _) -> factory == "PATTERN_NOT_MATCH" || factory == "UNREACHABLE_PATTERN" },
            "未知 constructor 不应再派生 PATTERN_NOT_MATCH / UNREACHABLE_PATTERN：$diagnostics",
        )
    }

    /**
     * 限定名 enum pattern 的两段各自解析（官方 `EnumPattern::constructor` 是 `MemberAccess`）：
     * - 限定符 `Shape` / `ShapeAlias` / `Slot<Int64>` 解析到 enum 或 typealias 本身，而不是 constructor——
     *   否则限定符上的导航跳错，enum 的 Find Usages / Rename 也会漏掉这些出现位置；
     * - selector 解析到 enum constructor；经 typealias 限定时同样可导航，且不报 pattern 不匹配。
     */
    @Test
    fun qualifiedEnumPatternOwner(mainFile: CjFile) {
        val (targets, diagnostics) = analyzeForTest(mainFile) {
            val names = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .filter { name -> PsiTreeUtil.getParentOfType(name, CjEnumPattern::class.java) != null }
                .filter { name -> name.parent is org.cangnova.cangjie.psi.CjDotQualifiedExpression }

            // 每个出现位置渲染成 `文本 -> 目标`；目标是 class-like 时取短名，是 constructor 时取 `Enum.Ctor`。
            val targets = names.map { name ->
                val rendered = name.resolveToSymbols().map { symbol ->
                    when (symbol) {
                        is CaEnumConstructorSymbol ->
                            "${symbol.containingEnumClassId?.shortClassName?.asString()}.${symbol.name.asString()}"
                        is CaClassLikeSymbol -> "type ${symbol.classId?.shortClassName?.asString()}"
                        else -> symbol::class.simpleName ?: "?"
                    }
                }
                "${name.text} -> $rendered"
            }
            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.text}" }
            targets to diagnostics
        }

        assertEquals(
            listOf(
                "Shape -> [type Shape]",
                "Circle -> [Shape.Circle]",
                "Shape -> [type Shape]",
                "Square -> [Shape.Square]",
                "ShapeAlias -> [type ShapeAlias]",
                "Circle -> [Shape.Circle]",
                "ShapeAlias -> [type ShapeAlias]",
                "Square -> [Shape.Square]",
                "Slot<Int64> -> [type Slot]",
                "Free -> [Slot.Free]",
                "IntSlot -> [type IntSlot]",
                "Used -> [Slot.Used]",
            ),
            targets,
        )
        assertEquals(emptyList<String>(), diagnostics, "限定符是 enum 或指向它的 typealias 时不应有任何诊断")
    }

    @Test
    fun patternConstructorResolution(mainFile: CjFile) {
        assertEquals(
            CaDeserializedDeclarationsOrigin.STUBS,
            CaPlatformSettings.getInstance(mainFile.project).deserializedDeclarationsOrigin,
            "该用例必须运行在 STUBS 配置下",
        )

        val (symbols, diagnostics) = analyzeForTest(mainFile) {
            val names = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            fun symbolsOf(text: String, parentType: Class<*>): List<String> =
                names.filter { name -> name.text == text && parentType.isInstance(name.parent) }
                    .flatMap { name -> name.resolveToSymbols() }
                    .map { symbol -> symbol::class.simpleName ?: "?" }

            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }

            mapOf(
                "somePattern" to symbolsOf("Some", CjEnumPattern::class.java),
                "nonePattern" to symbolsOf("None", CjVarOrEnumPattern::class.java),
                "returnNone" to symbolsOf("None", CjReturnExpression::class.java),
                "some1Pattern" to symbolsOf("Some1", CjEnumPattern::class.java),
                "somPattern" to symbolsOf("Som", CjEnumPattern::class.java),
                "none1Binding" to symbolsOf("None1", CjVarOrEnumPattern::class.java),
            ) to diagnostics
        }

        assertTrue(
            symbols.getValue("somePattern").any { it.contains("EnumConstructor") },
            "`case Some(v)` 的 Some 必须解析为 enum constructor 符号：${symbols["somePattern"]}",
        )
        assertTrue(
            symbols.getValue("nonePattern").any { it.contains("EnumConstructor") },
            "`case None` 的 None 必须解析为 enum constructor 符号：${symbols["nonePattern"]}",
        )
        assertTrue(
            symbols.getValue("returnNone").any { it.contains("EnumConstructor") },
            "`return None` 的 None 必须解析为 enum constructor 符号：${symbols["returnNone"]}",
        )
        assertTrue(
            symbols.getValue("some1Pattern").isEmpty() && symbols.getValue("somPattern").isEmpty(),
            "未知 constructor pattern 不应产生可导航符号：$symbols",
        )
        assertTrue(
            symbols.getValue("none1Binding").isEmpty(),
            "裸名字 None1 是 binding pattern，不应作为 enum constructor 解析：$symbols",
        )

        assertEquals(
            2,
            diagnostics.count { it == "UNRESOLVED_REFERENCE" },
            "只有 Some1 / Som 两个未知 constructor 应报 UNRESOLVED_REFERENCE：$diagnostics",
        )
        assertTrue(
            diagnostics.none { it == "PATTERN_NOT_MATCH" },
            "未知 constructor 不应再报 PATTERN_NOT_MATCH：$diagnostics",
        )
        assertEquals(
            1,
            diagnostics.count { it == "UNREACHABLE_PATTERN" },
            "None1 后面的 Some(v) 应报一次 UNREACHABLE_PATTERN：$diagnostics",
        )
    }
}
