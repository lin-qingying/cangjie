package org.cangjie.cfir.renderer

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirFunction
import org.cangjie.cfir.declarations.CfirImport
import org.cangjie.cfir.declarations.CfirPackageDirective
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.declarations.CfirValueParameter
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangjie.cfir.expressions.CfirLiteralKind
import org.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CfirRendererApiTest {

    @Test
    fun `default render delegates to golden compat`() {
        val file = sampleFile("sample")

        val legacy = CfirRenderer.render(file)
        val golden = CfirRenderer.withGoldenCompat().renderElementAsString(file)

        assertEquals(golden, legacy)
    }

    @Test
    fun `withXxx factories produce renderable instances`() {
        val file = sampleFile("factories")

        val golden = CfirRenderer.withGoldenCompat().renderElementAsString(file)
        val debug = CfirRenderer.withDebug().renderElementAsString(file)
        val readability = CfirRenderer.withReadability().renderElementAsString(file)

        assertEquals(golden, debug)
        assertEquals(golden, readability)
    }

    @Test
    fun `constructor injected resolve phase renderer can change output`() {
        val file = sampleFile("phase")
        val defaultOutput = CfirRenderer.withGoldenCompat().renderElementAsString(file)

        val renderer = CfirRenderer(
            resolvePhaseRenderer = CfirResolvePhaseRenderer { declaration ->
                if (declaration is CfirFunction) {
                    declaration.body = CfirBlock(
                        statements = mutableListOf(
                            CfirLiteralExpression(kind = CfirLiteralKind.STRING, value = "phase"),
                        ),
                    )
                }
            },
            declarationRenderer = CfirDeclarationRenderer { declaration ->
                if (declaration is CfirFunction) {
                    // no-op hook existence check through output change below
                }
            },
            packageDirectiveRenderer = CfirPackageDirectiveRenderer { },
            typeRenderer = CfirTypeRenderer { "R|CUSTOM|" },
            referenceRenderer = CfirReferenceRenderer { "custom-ref" },
            statusRenderer = CfirStatusRenderer { "" },
            inlineExpressionRenderer = CfirInlineExpressionRenderer { "custom-inline" },
            patternRenderer = CfirPatternRenderer { "custom-pattern" },
        )

        val customOutput = renderer.renderElementAsString(file)

        assertNotEquals(defaultOutput, customOutput)
    }

    private fun sampleFile(name: String): CfirFile {
        val moduleData = CfirModuleData(Name.identifier("<renderer-test>"))
        val returnTypeRef = CfirBasicTypeRef(name = Name.identifier("Int64"))
        val valueParameter = CfirValueParameter(
            moduleData = moduleData,
            origin = CfirDeclarationOrigin.Source,
            returnTypeRef = returnTypeRef,
            name = Name.identifier("x"),
        )
        val function = CfirFunction(
            moduleData = moduleData,
            origin = CfirDeclarationOrigin.Source,
            returnTypeRef = returnTypeRef,
            name = Name.identifier("foo"),
            valueParameters = listOf(valueParameter),
            body = CfirBlock(
                statements = mutableListOf(
                    CfirLiteralExpression(kind = CfirLiteralKind.INT, value = 1),
                ),
            ),
        )
        function.resolvePhase = CfirResolvePhase.RAW_CFIR

        return CfirFile(
            origin = CfirDeclarationOrigin.Source,
            moduleData = moduleData,
            name = "$name.cj",
            packageDirective = CfirPackageDirective(FqName("org.cangjie.renderer")),
            imports = listOf(CfirImport(FqName("org.cangjie.std.io"))),
            declarations = mutableListOf(function),
        ).also {
            it.resolvePhase = CfirResolvePhase.RAW_CFIR
        }
    }
}
