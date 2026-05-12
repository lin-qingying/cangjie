package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.builder.buildMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MacroConstructionArchitectureTest {
    @Test
    fun `symbol index records same-package source macro but excludes it from legal lookup`() {
        val fixture = Fixture()
        val macro = fixture.macroDeclaration("LocalMacro", "test.pkg")
        val file = fixture.file(packageName = "test.pkg", declarations = listOf(macro))
        val provider = CfirProviderImpl(fixture.session)
        val pre = buildPreMacroRawFiles(fixture.session, listOf(file))

        val index = buildMacroSymbolIndex(pre)

        val samePackage = index.samePackageMacroDef(FqName("test.pkg"), Name.identifier("LocalMacro"))
        assertNotNull(samePackage)
        assertSame(macro, samePackage!!.declaration)
        assertEquals(MacroDefinitionEntry.Source.SOURCE_PACKAGE, samePackage.source)
        assertTrue(index.lookupByShortName(Name.identifier("LocalMacro")).isEmpty())
        assertEquals(null, index.lookupByFqName(FqName("test.pkg.LocalMacro")))
        assertTrue(provider.isEmpty)
        assertTrue(provider.getAllFiles().isEmpty())
    }

    @Test
    fun `bindMacroImports resolves explicit alias wildcard default and builtin macro lookups`() {
        val fixture = Fixture()
        val explicit = macroEntry("lib.macros.Trace", MacroDefinitionEntry.Source.LIBRARY)
        val wildcard = macroEntry("lib.all.Wild", MacroDefinitionEntry.Source.MACRO_ARTIFACT)
        val default = macroEntry("default.macros.Defaulted", MacroDefinitionEntry.Source.SHARED_BUILTIN)
        val file = fixture.file(
            packageName = "test.pkg",
            imports = listOf(
                fixture.import("lib.macros.Trace", alias = "Log"),
                fixture.import("lib.all", isAllUnder = true),
            ),
        )
        val pre = buildPreMacroRawFiles(fixture.session, listOf(file))
        val index = buildMacroSymbolIndex(
            pre = pre,
            libraryDefinitions = listOf(explicit),
            macroArtifactDefinitions = listOf(wildcard),
            sharedBuiltinDefinitions = listOf(default),
        )

        val context = bindMacroImports(
            pre = pre,
            symbolIndex = index,
            defaultMacroImports = listOf(FqName("default.macros")),
        )

        assertSame(
            explicit,
            (context.resolveMacroCall(FqName("test.pkg"), null, Name.identifier("Log")) as MacroResolution.Resolved).entry,
        )
        assertSame(
            wildcard,
            (context.resolveMacroCall(FqName("test.pkg"), null, Name.identifier("Wild")) as MacroResolution.Resolved).entry,
        )
        assertSame(
            default,
            (context.resolveMacroCall(FqName("test.pkg"), null, Name.identifier("Defaulted")) as MacroResolution.Resolved).entry,
        )
        val builtin = context.resolveMacroCall(FqName("test.pkg"), null, Name.identifier("sourceFile"))
        assertTrue(builtin is MacroResolution.Builtin)
        assertEquals(MacroDefinitionEntry.Source.BUILTIN_MACRO, (builtin as MacroResolution.Builtin).entry.source)
    }

    @Test
    fun `same-package source macro resolution wins before foreign lookup`() {
        val fixture = Fixture()
        val file = fixture.file(
            packageName = "test.pkg",
            declarations = listOf(fixture.macroDeclaration("Conflict", "test.pkg")),
        )
        val pre = buildPreMacroRawFiles(fixture.session, listOf(file))
        val index = buildMacroSymbolIndex(
            pre = pre,
            libraryDefinitions = listOf(macroEntry("lib.macros.Conflict", MacroDefinitionEntry.Source.LIBRARY)),
        )
        val context = bindMacroImports(pre, index, defaultMacroImports = listOf(FqName("lib.macros")))

        val resolution = context.resolveMacroCall(FqName("test.pkg"), null, Name.identifier("Conflict"))

        assertTrue(resolution is MacroResolution.SamePackage)
        assertEquals(MacroDefinitionEntry.Source.SOURCE_PACKAGE, (resolution as MacroResolution.SamePackage).sourceEntry.source)
    }

    @Test
    fun `builtin non-macro surface is not treated as executor macro`() {
        val fixture = Fixture()
        val pre = buildPreMacroRawFiles(fixture.session, listOf(fixture.file(packageName = "test.pkg")))
        val context = bindMacroImports(pre, buildMacroSymbolIndex(pre))

        val resolution = context.resolveMacroCall(FqName("test.pkg"), null, Name.identifier("IfAvailable"))

        assertTrue(resolution is MacroResolution.BuiltinNonMacro)
    }

    @Test
    fun `alias conflict records all conflicting import targets`() {
        val fixture = Fixture()
        val file = fixture.file(
            packageName = "test.pkg",
            imports = listOf(
                fixture.import("lib.one.Trace", alias = "Log"),
                fixture.import("lib.two.Trace", alias = "Log"),
            ),
        )
        val pre = buildPreMacroRawFiles(fixture.session, listOf(file))
        val index = buildMacroSymbolIndex(
            pre = pre,
            libraryDefinitions = listOf(
                macroEntry("lib.one.Trace", MacroDefinitionEntry.Source.LIBRARY),
                macroEntry("lib.two.Trace", MacroDefinitionEntry.Source.LIBRARY),
            ),
        )

        val context = bindMacroImports(pre, index)

        assertEquals(1, context.aliasConflicts.size)
        assertEquals(Name.identifier("Log"), context.aliasConflicts.single().alias)
        assertEquals(
            setOf(FqName("lib.one.Trace"), FqName("lib.two.Trace")),
            context.aliasConflicts.single().targets.toSet(),
        )
    }

    @Test
    fun `recordExpandedRawFilesOnce finalizes provider and rejects duplicate record`() {
        val fixture = Fixture()
        val provider = CfirProviderImpl(fixture.session)
        val pre = buildPreMacroRawFiles(fixture.session, listOf(fixture.file(packageName = "test.pkg")))
        val result = MacroConstructionService.successOf(
            pre = pre,
            files = pre.files.map { it.cfirFile },
            registry = MacroExpansionRegistry.EMPTY,
        )

        recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)

        assertFalse(provider.isEmpty)
        assertTrue(provider.isFinalized)
        assertEquals(1, provider.getAllFiles().size)
        val failure = assertThrows<IllegalStateException> {
            recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
        }
        assertTrue(failure.message!!.contains("Source CfirProviderImpl is not empty"))
    }

    @Test
    fun `recordExpandedRawFilesOnce records surfaces only after construction result becomes recordable`() {
        val fixture = Fixture()
        val provider = CfirProviderImpl(fixture.session)
        val surface = surface(id = 1, name = "Imported", packageName = "test.pkg")
        val file = fixture.file(packageName = "test.pkg")
        val pre = buildPreMacroRawFiles(
            session = fixture.session,
            rawCfirFiles = listOf(file),
            fileSurfaces = listOf(listOf(surface)),
        )

        assertEquals(listOf(surface), pre.allSurfaces)
        assertTrue(provider.isEmpty)

        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            pre = pre,
            mode = MacroConstructionService.Mode.STRICT,
        ) as MacroConstructionResult.Success
        recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)

        assertTrue(provider.isFinalized)
        assertEquals(listOf(file), provider.getAllFiles())
    }

    @Test
    fun `registry records generated source origin for ordinary checker remap`() {
        val generatedSource = CjOffsetsOnlySourceElement(startOffset = 10, endOffset = 20)
        val registry = MacroExpansionRegistry()

        registry.registerGeneratedSource(generatedSource, originSurfaceId = 42L)

        assertEquals(42L, registry.generatedSourceOriginById[generatedSource])
    }

    private class Fixture {
        val session: CfirSession = object : CfirSession(Kind.Source) {}
        val moduleData: CfirModuleData = CfirSourceModuleData(
            name = Name.identifier("<macro-test>"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            platform = CfirPlatform.DEFAULT,
        ).also {
            it.bindSession(session)
            session.register(CfirModuleData::class, it)
        }

        fun file(
            packageName: String,
            imports: List<org.cangnova.cangjie.cfir.declarations.CfirImport> = emptyList(),
            declarations: List<CfirDeclaration> = emptyList(),
        ): CfirFile = buildFile {
            source = null
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "${packageName.replace('.', '_')}.cj"
            sourceFile = null
            packageDirective = buildPackageDirective {
                source = null
                packageFqName = FqName(packageName)
            }
            this.imports += imports
            sourceFileLinesMapping = null
            this.declarations += declarations
        }

        fun import(fqName: String, isAllUnder: Boolean = false, alias: String? = null) = buildImport {
            source = null
            importedFqName = FqName(fqName)
            this.isAllUnder = isAllUnder
            aliasName = alias?.let(Name::identifier)
            aliasSource = null
        }

        fun macroDeclaration(name: String, packageName: String) = buildMacroDeclaration {
            source = null
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl.DEFAULT
            returnTypeRef = buildImplicitTypeRef()
            body = null
            symbol = CfirMacroDeclarationSymbol(CallableId(FqName(packageName), Name.identifier(name)))
            this.name = Name.identifier(name)
        }

    }

    private companion object {
        fun macroEntry(fqName: String, source: MacroDefinitionEntry.Source): MacroDefinitionEntry {
            val full = FqName(fqName)
            return MacroDefinitionEntry(
                packageFqName = full.parent(),
                name = full.shortName(),
                source = source,
            )
        }

        fun surface(id: Long, name: String, packageName: String): MacroSurfaceExpr {
            return MacroSurfaceExpr(
                surfaceId = id,
                qualifiedName = FqName(name),
                kind = MacroSurface.Kind.PLAIN,
                hasParenthesis = true,
                attrTokens = emptyList(),
                inputTokens = emptyList(),
                sourceRange = MacroSurfaceSourceRange(
                    source = null,
                    startOffset = 0,
                    endOffset = name.length,
                ),
                scopeContext = MacroSurfaceScopeContext(
                    packageFqName = FqName(packageName),
                    enclosingClassFqName = null,
                    enclosingFunctionName = null,
                ),
                modifiers = emptyList(),
                carriedAnnotations = emptyList(),
                capturedRawSyntax = null,
                containerContext = MacroSurfaceContainerContext(
                    outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
                    isInsidePrimaryConstructor = false,
                    isInsideEnumBody = false,
                    isInsideBlock = true,
                ),
                replaceHandle = CfirReplaceHandle(id),
            )
        }
    }
}
