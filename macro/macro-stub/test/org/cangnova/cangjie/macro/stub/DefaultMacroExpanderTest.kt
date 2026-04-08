package org.cangnova.cangjie.macro.stub

import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.expressions.builder.buildMacroExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.macro.DefaultMacroExpander
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroCallSite
import org.cangnova.cangjie.macro.MacroCollector
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroReplacementOutput
import org.cangnova.cangjie.macro.MacroReplacer
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultMacroExpanderTest {
    @Test
    fun `expander orchestrates collect execute replace flow`() {
        val file = buildTestFile()
        val site = MacroCallSite(
            expression = buildMacroExpression {
                name = Name.identifier("DemoMacro")
                inputText = "(value)"
            },
            file = file,
            callInfo = MacroCallInfo(
                idName = "DemoMacro",
                methodName = "DemoMacro",
                argTokens = emptyList(),
            ),
        )

        var collectCount = 0
        val collector = object : MacroCollector {
            override fun collect(files: List<CfirFile>): List<MacroCallSite> {
                collectCount++
                return if (collectCount == 1) listOf(site) else emptyList()
            }
        }

        var replaceCount = 0
        val replacer = object : MacroReplacer {
            override fun replace(
                files: List<CfirFile>,
                expansions: Map<MacroCallSite, MacroExpansionResult>,
            ): MacroReplacementOutput {
                replaceCount++
                assertEquals(listOf(file), files)
                val expansion = expansions[site]
                assertNotNull(expansion)
                val success = expansion as? MacroExpansionResult.Success
                assertNotNull(success)
                assertEquals("let expanded = value", success?.expandedText)
                return MacroReplacementOutput(
                    files = files,
                    diagnostics = emptyList(),
                    replacedCount = 1,
                )
            }
        }

        var executionCount = 0
        val executor = StubMacroExecutor().apply {
            registerExpansion("DemoMacro") {
                executionCount++
                MacroExpansionResult.Success(
                    tokens = emptyList(),
                    expandedText = "let expanded = value",
                )
            }
        }

        val output = DefaultMacroExpander(
            collector = collector,
            executor = executor,
            replacer = replacer,
        ).expandAll(listOf(file))

        assertEquals(2, collectCount)
        assertEquals(1, executionCount)
        assertEquals(1, replaceCount)
        assertEquals(1, output.expandedCount)
        assertEquals(1, output.iterations)
        assertTrue(output.diagnostics.isEmpty())
    }

    private fun buildTestFile(): CfirFile {
        return buildFile {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "macro-test.cj"
            packageDirective = buildPackageDirective {
                packageFqName = FqName("macro.test")
            }
        }
    }

    private object TestSession : CfirSession(Kind.Source) {
        override fun toString(): String = "DefaultMacroExpanderTestSession"
    }

    private object TestModuleData : CfirModuleData() {
        override val name: Name = Name.identifier("macro-stub-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = true
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "macro-stub-test"
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }
}
