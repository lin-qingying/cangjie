package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirImportNamespaceContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.resolve.resolveImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.cfir.resolve.providers.isPackageVisibleSourceImport
import org.junit.runner.RunWith

/** 真实 CJ 源码经过两种 raw builder，再验证组织限定在 provider 重导出链中的身份。 */
@RunWith(JUnit3RunnerWithInners::class)
class CfirOrganizationReexportTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiOrganizationReexports() = assertOrganizationReexports(useLightTree = false)
    fun testLightTreeOrganizationReexports() = assertOrganizationReexports(useLightTree = true)

    fun testPsiPackageVisibleGroupedImports() = assertPackageVisibleGroupedImports(useLightTree = false)
    fun testLightTreePackageVisibleGroupedImports() = assertPackageVisibleGroupedImports(useLightTree = true)

    /** 两个根包文件通过 internal 分组共享库类型，别名与非别名都不得丢失局部路径。 */
    private fun assertPackageVisibleGroupedImports(useLightTree: Boolean) {
        for (alias in listOf(false, true)) {
            val session = createTestSession()
            val provider = session.cfirProvider as CfirProviderImpl
            fun file(name: String, text: String): CfirFile = if (useLightTree) {
                buildCfirFileFromLightTree(text, session, "$name.cj")
            } else {
                PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL).buildCfirFile(createCjFile(name, text))
            }
            val importedName = if (alias) "b" else "ArrayList"
            val library = file("collection", "package std.collection\npublic class ArrayList<T> {}")
            val consumer = file("fileUseArrayList", "public var a: $importedName<Int64> = $importedName<Int64>()")
            val importer = file("imports", "internal import {std.collection.ArrayList${if (alias) " as b" else ""}}\nmain() {}")
            assertEquals(Visibilities.Internal, importer.imports.single().visibility)
            assertTrue(importer.imports.single().isPackageVisibleSourceImport())
            assertNull(importer.imports.single().organizationName)
            assertNull(importer.packageDirective.organizationName)
            assertEquals(FqName("std.collection.ArrayList"), importer.imports.single().importedFqName)
            assertEquals(FqName.ROOT, importer.packageDirective.packageFqName)
            assertEquals(consumer.packageDirective.packageFqName, importer.packageDirective.packageFqName)
            val files = listOf(library, consumer, importer)
            val pre = buildPreMacroRawFiles(session, files)
            val result = MacroConstructionService.successOf(pre, files, MacroExpansionRegistry.EMPTY)
            recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
            val expected = library.declarations.filterIsInstance<CfirClass>().single().symbol
            assertEquals("Both root-package files must be registered", 2, provider.getCfirFilesByPackage(FqName.ROOT).size)
            assertSame(expected, provider.symbolProvider.getClassLikeSymbolByClassId(ClassId(FqName("std.collection"), Name.identifier("ArrayList"))))
            kotlin.test.assertTrue(Name.identifier(importedName) in provider.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(FqName.ROOT).orEmpty(),
                "Root export names must include $importedName; imports=" + provider.getCfirFilesByPackage(FqName.ROOT).flatMap { it.imports }.map { Triple(it.importedFqName, it.visibility, it.aliasName) })
            assertSame(expected, provider.symbolProvider.getClassLikeSymbolByClassId(ClassId(FqName.ROOT, Name.identifier(importedName))))
            val namespace = provider.symbolProvider.getImportNamespace(CfirImportNamespaceContext(FqName.ROOT, null))
            assertEquals(listOf(expected), namespace.classifiers(Name.identifier(importedName)))
        }
    }

    private fun assertOrganizationReexports(useLightTree: Boolean) {
        val session = createTestSession()
        val provider = session.cfirProvider as CfirProviderImpl
        fun file(name: String, source: String): CfirFile = if (useLightTree) {
            buildCfirFileFromLightTree(source, session, "$name.cj")
        } else {
            PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL).buildCfirFile(createCjFile(name, source))
        }
        val files = listOf(
            file("unqualified", "package shared\npublic class Box {}\npublic func same(): Unit {}"),
            file("org1", "package org1::shared\npublic class Box {}\npublic func same(): Unit {}"),
            file("org2", "package org2::shared\npublic class Box {}\npublic func same(): Unit {}"),
            file("facade", """
                package facade
                public import org1::shared.{Box as First, same as first}
                public import org2::shared.{Box as Second, same as second}
            """.trimIndent()),
            file("star", "package star\npublic import org2::shared.*"),
            file("chain", "package chain\npublic import facade.First as Again"),
            file("missing", "package missing\npublic import absent::shared.*"),
            file("bridge1", "package org1::bridge\npublic import org2::shared.*"),
            file("bridge2", "package org2::bridge\npublic import org1::shared.*"),
        )
        val classes = files.take(3).map { it.declarations.filterIsInstance<CfirClass>().single() }
        val functions = files.take(3).map { it.declarations.filterIsInstance<CfirNamedFunction>().single() }
        val pre = buildPreMacroRawFiles(session, files)
        val result = MacroConstructionService.successOf(pre, files, MacroExpansionRegistry.EMPTY)
        recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
        val symbols = provider.symbolProvider
        assertSame(classes[1].symbol, symbols.getClassLikeSymbolByClassId(ClassId(FqName("facade"), Name.identifier("First"))))
        assertSame(classes[2].symbol, symbols.getClassLikeSymbolByClassId(ClassId(FqName("facade"), Name.identifier("Second"))))
        assertEquals(listOf(functions[1].symbol), symbols.getTopLevelFunctionSymbols(FqName("facade"), Name.identifier("first")))
        assertEquals(listOf(functions[2].symbol), symbols.getTopLevelCallableSymbols(FqName("facade"), Name.identifier("second")))
        assertSame(classes[2].symbol, symbols.getClassLikeSymbolByClassId(ClassId(FqName("star"), Name.identifier("Box"))))
        assertEquals(listOf(functions[2].symbol), symbols.getTopLevelFunctionSymbols(FqName("star"), Name.identifier("same")))
        assertSame(classes[1].symbol, symbols.getClassLikeSymbolByClassId(ClassId(FqName("chain"), Name.identifier("Again"))))
        assertNull(symbols.getClassLikeSymbolByClassId(ClassId(FqName("missing"), Name.identifier("Box"))))
        assertTrue(symbols.getTopLevelCallableSymbols(FqName("missing"), Name.identifier("same")).isEmpty())

        // 查询导入也由相同 raw builder 解析，组织限定的是 bridge 而不是最终声明的包。
        fun query(source: String) = file("query", "package consumer\nimport $source").imports.single()
        val bridgeBinding = session.resolveImportBinding(query("org1::bridge.Box"), CfirLookupOrigin.EXPLICIT_IMPORT)
        assertEquals(listOf(classes[2].symbol), bridgeBinding.targets.filterIsInstance<CfirResolvedImportTarget.ClassLike>().map { it.symbol })
        val reverseBridge = session.resolveImportBinding(query("org2::bridge.Box"), CfirLookupOrigin.EXPLICIT_IMPORT)
        assertEquals(listOf(classes[1].symbol), reverseBridge.targets.filterIsInstance<CfirResolvedImportTarget.ClassLike>().map { it.symbol })
        val composite = CfirCompositeSymbolProvider(session, listOf(symbols))
        val namespace = composite.getImportNamespace(CfirImportNamespaceContext(FqName("bridge"), Name.identifier("org1")))
        assertEquals(listOf(classes[2].symbol), namespace.classifiers(Name.identifier("Box")))
        assertEquals(listOf(functions[2].symbol), namespace.functions(Name.identifier("same")))
        val starBinding = session.resolveImportBinding(query("org1::bridge.*"), CfirLookupOrigin.EXPLICIT_IMPORT)
        val starScope = CfirExplicitStarImportingScope(listOf(starBinding), composite)
        val starClasses = mutableListOf<CfirClassLikeSymbol<*>>()
        starScope.processClassifiersByName(Name.identifier("Box"), starClasses::add)
        assertEquals(listOf(classes[2].symbol), starClasses)
        val packageScope = CfirPackageMemberScope(FqName("bridge"), session, composite, organizationName = Name.identifier("org1"))
        val packageFunctions = mutableListOf<CfirNamedFunctionSymbol>()
        packageScope.processFunctionsByName(Name.identifier("same"), packageFunctions::add)
        assertEquals(listOf(functions[2].symbol), packageFunctions)
    }
}
