package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceScopeContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSymbolIndex
import org.cangnova.cangjie.cfir.resolve.providers.macro.bindMacroImports
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(CompilerConfiguration.Internals::class)
class FrontendMacroConstructionServiceTest {
    @Test
    fun degradedModeRegistersDeclarationSurfacePlaceholderWithoutFinalSurfaceNode() {
        val fixture = macroDeclarationSurfaceFixture()

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expand(
            pre = fixture.pre,
            context = fixture.context,
            mode = MacroConstructionService.Mode.DEGRADED,
        )

        assertTrue(result is MacroConstructionResult.Degraded)
        val degraded = result as MacroConstructionResult.Degraded
        assertSame(fixture.file, degraded.recordableFiles.files.single())
        assertEquals(fixture.surface.surfaceId, degraded.registry.placeholderOriginById[fixture.surface.surfaceId])
        assertSame(fixture.surface, degraded.registry.originSurfaceForPlaceholder(fixture.surface.surfaceId))
        assertTrue(
            degraded.registry.diagnostics.any { it.originSurfaceId == fixture.surface.surfaceId },
            "surface-only degraded diagnostics must keep original macro surface id",
        )
    }

    @Test
    fun strictModeRejectsDeclarationSurfaceWithoutStableSplice() {
        val fixture = macroDeclarationSurfaceFixture()

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expand(
            pre = fixture.pre,
            context = fixture.context,
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Failed)
    }

    private fun macroDeclarationSurfaceFixture(): MacroDeclarationSurfaceFixture {
        val session = object : CfirSession(Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)

        val packageFqName = FqName("sample")
        val function = buildNamedFunction {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier("annotated")))
            this.name = Name.identifier("annotated")
            isMut = false
            body = buildBlock {}
        }
        val file = buildFile {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            this.name = "sample.cj"
            packageDirective = buildPackageDirective {
                this.packageFqName = packageFqName
            }
            declarations += function
        }
        val surface = MacroSurfaceDecl(
            surfaceId = 2001L,
            qualifiedName = FqName.topLevel(Name.identifier("MissingAnnotationMacro")),
            kind = MacroSurface.Kind.PLAIN,
            hasParenthesis = true,
            attrTokens = emptyList(),
            inputTokens = emptyList(),
            sourceRange = null,
            scopeContext = MacroSurfaceScopeContext(
                packageFqName = packageFqName,
                enclosingClassFqName = null,
                enclosingFunctionName = null,
            ),
            modifiers = emptyList(),
            carriedAnnotations = listOf("@MissingAnnotationMacro()"),
            capturedRawSyntax = "@MissingAnnotationMacro()",
            containerContext = MacroSurfaceContainerContext(
                outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL,
                isInsidePrimaryConstructor = false,
                isInsideEnumBody = false,
                isInsideBlock = false,
            ),
            replaceHandle = CfirReplaceHandle(handleId = 2001L),
        )
        val pre = buildPreMacroRawFiles(
            session = session,
            rawCfirFiles = listOf(file),
            fileSurfaces = listOf(listOf(surface)),
        )

        return MacroDeclarationSurfaceFixture(
            file = file,
            surface = surface,
            pre = pre,
            context = bindMacroImports(pre, MacroSymbolIndex.EMPTY),
        )
    }

    private data class MacroDeclarationSurfaceFixture(
        val file: CfirFile,
        val surface: MacroSurfaceDecl,
        val pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
        val context: MacroResolutionContext,
    )

    private class TestModuleData(session: CfirSession) : CfirModuleData() {
        override val name: Name = Name.identifier("frontend-macro-construction-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = true
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "frontend-macro-construction-test"
        override val session: CfirSession = session

        init {
            bindSession(session)
        }
    }
}
