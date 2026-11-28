package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.Package
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationPosition
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationSourceElement
import org.cangnova.cangjie.cfir.declarations.identifierSourceElementOrNull
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Position 独立于 FullId：当前包使用 pkgId=0/file 一基，导入包使用 pkgId 一基/file 零基。 */
class CjoSourcePositionResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `current package positions use one based file indices`() {
        val packages = importedPackages()
        for ((file, expectedPath) in listOf(1u to CURRENT_FIRST, 2u to CURRENT_SECOND)) {
            val pkg = owningPackage(CjoPositionMetadata(file = file, pkgId = 0u, line = 7, column = 11))

            assertEquals(
                CjSourceLocation(expectedPath, 7, 11),
                CjoSourcePositionResolver.resolve(pkg, pkg.allDecls(0)!!.begin, packages),
            )
        }
    }

    @Test
    fun `absent positions and zero current file index do not acquire a source location`() {
        val packages = importedPackages()
        val absent = owningPackage(null)
        assertNull(CjoSourcePositionResolver.resolve(absent, absent.allDecls(0)!!.begin, packages))

        val zeroFile = owningPackage(CjoPositionMetadata(file = 0u, pkgId = 0u, line = 7, column = 11))
        assertNull(CjoSourcePositionResolver.resolve(zeroFile, zeroFile.allDecls(0)!!.begin, packages))
    }

    @Test
    fun `imported package and file indices have independent one based and zero based conventions`() {
        val packages = importedPackages()
        val cases = listOf(
            Triple(1u, 0u, IMPORTED_FIRST_FIRST),
            Triple(1u, 1u, IMPORTED_FIRST_SECOND),
            Triple(2u, 0u, IMPORTED_SECOND_FIRST),
            Triple(2u, 1u, IMPORTED_SECOND_SECOND),
        )
        for ((packageId, file, expectedPath) in cases) {
            val pkg = owningPackage(CjoPositionMetadata(file = file, pkgId = packageId, line = 13, column = 4))

            assertEquals(
                CjSourceLocation(expectedPath, 13, 4),
                CjoSourcePositionResolver.resolve(pkg, pkg.allDecls(0)!!.begin, packages),
                "Position(pkgId=$packageId, file=$file)",
            )
        }
    }

    @Test
    fun `positions index allFiles directly instead of interpreting global FileInfo file ids`() {
        val packages = importedPackages()
        val currentPosition = CjoPositionMetadata(file = 2u, pkgId = 0u, line = 3, column = 8)
        val current = owningPackage(
            currentPosition,
            fileInfo = fileInfoWithGlobalIds(901u, 704u),
        )

        assertEquals(901u, current.allFileInfo(0)!!.fileId)
        assertEquals(704u, current.allFileInfo(1)!!.fileId)
        assertEquals(
            CjSourceLocation(CURRENT_SECOND, 3, 8),
            CjoSourcePositionResolver.resolve(current, current.allDecls(0)!!.begin, packages),
        )
        val imported = owningPackage(CjoPositionMetadata(file = 1u, pkgId = 2u, line = 5, column = 6))
        assertEquals(
            CjSourceLocation(IMPORTED_SECOND_SECOND, 5, 6),
            CjoSourcePositionResolver.resolve(imported, imported.allDecls(0)!!.begin, packages),
        )
    }

    @Test
    fun `invalid package and file indices including unsigned overflow remain unresolved`() {
        val packages = importedPackages()
        val indices = listOf(
            0u to 3u,
            1u to 2u,
            4u to 0u,
            UInt.MAX_VALUE to 0u,
            (Int.MAX_VALUE.toUInt() + 1u) to 0u,
            0u to UInt.MAX_VALUE,
            0u to (Int.MAX_VALUE.toUInt() + 1u),
            1u to UInt.MAX_VALUE,
            1u to (Int.MAX_VALUE.toUInt() + 1u),
            // FullId 的 CURRENT=-2 不能被当成 Position 的当前包标记。
            (UInt.MAX_VALUE - 1u) to 1u,
        )
        for ((packageId, file) in indices) {
            val pkg = owningPackage(CjoPositionMetadata(file = file, pkgId = packageId, line = 7, column = 11))

            assertNull(
                CjoSourcePositionResolver.resolve(pkg, pkg.allDecls(0)!!.begin, packages),
                "Position(pkgId=$packageId, file=$file) must not alias a valid file",
            )
        }
    }

    @Test
    fun `missing imported packages and invalid line or column do not use current package locations`() {
        val packages = importedPackages()
        val missing = owningPackage(CjoPositionMetadata(file = 0u, pkgId = 3u, line = 7, column = 11))
        assertNull(CjoSourcePositionResolver.resolve(missing, missing.allDecls(0)!!.begin, packages))

        for ((line, column) in listOf(0 to 1, 1 to 0, -1 to 1, 1 to -1, Int.MIN_VALUE to 1, 1 to Int.MIN_VALUE)) {
            for (packageId in listOf(0u, 1u)) {
                val file = if (packageId == 0u) 1u else 0u
                val pkg = owningPackage(CjoPositionMetadata(file = file, pkgId = packageId, line = line, column = column))
                assertNull(CjoSourcePositionResolver.resolve(pkg, pkg.allDecls(0)!!.begin, packages))
            }
        }
    }

    @Test
    fun `blank metadata file paths do not produce invalid source locations`() {
        val current = owningPackage(CjoPositionMetadata(file = 1u, line = 1, column = 1), files = listOf(""))
        assertNull(CjoSourcePositionResolver.resolve(current, current.allDecls(0)!!.begin, importedPackages()))

        val imported = owningPackage(CjoPositionMetadata(file = 0u, pkgId = 1u, line = 1, column = 1))
        val packages = InMemoryPackages(listOf(packageMetadata("positions.first", listOf(""))))
        assertNull(CjoSourcePositionResolver.resolve(imported, imported.allDecls(0)!!.begin, packages))
    }

    @Test
    fun `declaration deserialization resolves each imported position while retaining its current CJO identity`() {
        val context = deserializationContext()
        val function = assertIs<CfirNamedFunction>(context.createDeclDeserializer().deserializeDecl(0))
        val position = assertNotNull(function.cjoDeclarationPosition)
        val declarationLocation = CjSourceLocation(IMPORTED_FIRST_FIRST, 5, 3)
        val identifierLocation = CjSourceLocation(IMPORTED_FIRST_SECOND, 6, 9)
        val endLocation = CjSourceLocation(IMPORTED_SECOND_SECOND, 8, 20)

        assertEquals(declarationLocation, position.declarationLocation)
        assertEquals(identifierLocation, position.identifierLocation)
        assertEquals(endLocation, position.endLocation)
        val source = assertNotNull(function.cjoDeclarationSourceElement)
        assertEquals(declarationLocation, source.sourceLocation)
        assertEquals(IMPORTED_FIRST_FIRST, source.binaryFilePath)
        val identifierSource = assertNotNull(source.identifierSourceElementOrNull())
        assertEquals(identifierLocation, identifierSource.sourceLocation)
        assertEquals(IMPORTED_FIRST_SECOND, identifierSource.binaryFilePath)
        assertSame(context.moduleData, function.moduleData)
        val origin = assertNotNull(function.cjoDeclarationOrigin)
        assertEquals(context.sourcePath!!.toAbsolutePath().normalize().toString(), origin.packagePath)
        assertEquals(0, origin.declarationIndex)
    }

    @Test
    fun `missing identifier package leaves the declaration anchor intact without creating a name anchor`() {
        val context = deserializationContext()
        val function = assertIs<CfirNamedFunction>(context.createDeclDeserializer().deserializeDecl(1))
        val position = assertNotNull(function.cjoDeclarationPosition)
        val declarationLocation = CjSourceLocation(CURRENT_FIRST, 10, 1)

        assertEquals(declarationLocation, position.declarationLocation)
        assertEquals(CjSourceLocation(CURRENT_SECOND, 15, 2), position.endLocation)
        assertNull(position.identifierLocation)
        val source = assertNotNull(function.cjoDeclarationSourceElement)
        assertEquals(declarationLocation, source.sourceLocation)
        assertNull(source.relatedInformationLocation)
        assertNull(source.identifierSourceElementOrNull())
        assertSame(context.moduleData, function.moduleData)
        assertEquals(context.sourcePath!!.toAbsolutePath().normalize().toString(), function.cjoDeclarationOrigin?.packagePath)
        assertEquals(1, function.cjoDeclarationOrigin?.declarationIndex)
    }

    @Test
    fun `unresolved declaration begin does not discard a valid end or imported identifier`() {
        val context = deserializationContext()
        val function = assertIs<CfirNamedFunction>(context.createDeclDeserializer().deserializeDecl(2))
        val position = assertNotNull(function.cjoDeclarationPosition)
        val identifierLocation = CjSourceLocation(IMPORTED_FIRST_SECOND, 12, 8)

        assertNull(position.declarationLocation)
        assertEquals(CjSourceLocation(IMPORTED_FIRST_FIRST, 14, 4), position.endLocation)
        assertEquals(identifierLocation, position.identifierLocation)
        val source = assertNotNull(function.cjoDeclarationSourceElement)
        assertNull(source.sourceLocation)
        assertNull(source.binaryFilePath)
        val identifierSource = assertNotNull(source.identifierSourceElementOrNull())
        assertEquals(identifierLocation, identifierSource.sourceLocation)
        assertEquals(IMPORTED_FIRST_SECOND, identifierSource.binaryFilePath)
        assertSame(context.moduleData, function.moduleData)
        assertEquals(context.sourcePath!!.toAbsolutePath().normalize().toString(), function.cjoDeclarationOrigin?.packagePath)
        assertEquals(2, function.cjoDeclarationOrigin?.declarationIndex)
    }

    private fun deserializationContext(): CfirDeserializationContext {
        val imported = listOf(
            packageMetadata("positions.first", listOf(IMPORTED_FIRST_FIRST, IMPORTED_FIRST_SECOND)),
            packageMetadata("positions.second", listOf(IMPORTED_SECOND_FIRST, IMPORTED_SECOND_SECOND)),
        )
        val current = packageMetadata("positions.current", listOf(CURRENT_FIRST, CURRENT_SECOND)).copy(
            imports = IMPORTS,
            declarations = listOf(
                // 官方 writer 通常把三种位置写在同文件；跨文件夹具验证三个 wire Position 必须分别解码。
                functionDeclaration(
                    "crossFile",
                    begin = CjoPositionMetadata(file = 0u, pkgId = 1u, line = 5, column = 3),
                    end = CjoPositionMetadata(file = 1u, pkgId = 2u, line = 8, column = 20),
                    identifier = CjoPositionMetadata(file = 1u, pkgId = 1u, line = 6, column = 9),
                ),
                functionDeclaration(
                    "missingIdentifier",
                    begin = CjoPositionMetadata(file = 1u, pkgId = 0u, line = 10, column = 1),
                    end = CjoPositionMetadata(file = 2u, pkgId = 0u, line = 15, column = 2),
                    identifier = CjoPositionMetadata(file = 0u, pkgId = 3u, line = 11, column = 8),
                ),
                functionDeclaration(
                    "missingBegin",
                    begin = CjoPositionMetadata(file = 0u, pkgId = 3u, line = 10, column = 1),
                    end = CjoPositionMetadata(file = 0u, pkgId = 1u, line = 14, column = 4),
                    identifier = CjoPositionMetadata(file = 1u, pkgId = 1u, line = 12, column = 8),
                ),
            ),
            types = listOf(
                CjoTypeMetadata(kind = TypeKind.Unit),
                CjoTypeMetadata(
                    kind = TypeKind.Func,
                    semanticInfo = CjoFunctionTypeInfoMetadata(returnType = 1u, isC = false, hasVariableLenArg = false),
                ),
            ),
        )
        for (metadata in imported + current) {
            CjoPackageWriter.write(tempDir.resolve(CjoConstants.packageNameToPath(metadata.fullPackageName)), metadata)
        }
        val manager = CjoManager(
            CjoSearchPath { if (it == "CANGJIE_LIBRARY") tempDir.toString() else null },
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
        )
        val loaded = assertNotNull(manager.loadPackageSnapshot("positions.current"))
        val module = CfirBinaryDependenciesModuleData(Name.special("<source position test>"))
        val session = object : CfirSession(CfirSession.Kind.Library) {}.apply {
            register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent())
        }
        module.bindSession(session)
        return CfirDeserializationContext(
            pkg = loaded.pkg,
            header = loaded.header,
            moduleData = module,
            cjoManager = manager,
            sourcePath = loaded.sourcePath,
            implicitSystemAnnotations = emptySet(),
        )
    }

    private fun functionDeclaration(
        name: String,
        begin: CjoPositionMetadata,
        end: CjoPositionMetadata,
        identifier: CjoPositionMetadata,
    ): CjoPackageDeclaration = CjoPackageDeclaration(
        identifier = name,
        kind = DeclKind.FuncDecl,
        type = 2u,
        begin = begin,
        end = end,
        identifierPosition = identifier,
        info = CjoFunctionInfo(
            body = CjoFunctionBodyInfo(
                parameterLists = listOf(emptyList()),
                desugaredParameterLists = listOf(emptyList()),
                returnType = 1u,
            ),
        ),
    )

    private fun owningPackage(
        position: CjoPositionMetadata?,
        files: List<String> = listOf(CURRENT_FIRST, CURRENT_SECOND),
        fileInfo: List<CjoFileInfoMetadata> = emptyList(),
    ): Package = encode(
        packageMetadata("positions.current", files).copy(
            imports = IMPORTS,
            declarations = listOf(CjoPackageDeclaration(identifier = "site", begin = position)),
            fileInfo = fileInfo,
        ),
    )

    private fun importedPackages(): CjoLoadedPackageProvider = InMemoryPackages(
        listOf(
            packageMetadata("positions.first", listOf(IMPORTED_FIRST_FIRST, IMPORTED_FIRST_SECOND))
                .copy(fileInfo = fileInfoWithGlobalIds(1051u, 2051u)),
            packageMetadata("positions.second", listOf(IMPORTED_SECOND_FIRST, IMPORTED_SECOND_SECOND))
                .copy(fileInfo = fileInfoWithGlobalIds(3081u, 4081u)),
        ),
    )

    /** 包快照全部来自正式 writer 的字节；仅省去与位置索引无关的磁盘搜索。 */
    private class InMemoryPackages(metadata: List<CjoPackageMetadata>) : CjoLoadedPackageProvider {
        private val snapshots = metadata.associate { input ->
            val pkg = encode(input)
            input.fullPackageName to object : CjoLoadedPackage {
                override val pkg: Package = pkg
                override val header: CjoPackageHeader = CjoPackageHeader.fromPackage(pkg)
                override val sourcePath: Path = Path.of("in-memory", CjoConstants.packageNameToPath(input.fullPackageName))
            }
        }

        override fun loadPackageSnapshot(fullPkgName: String): CjoLoadedPackage? = snapshots[fullPkgName]
    }

    private companion object {
        const val CURRENT_FIRST = "source/current/first.cj"
        const val CURRENT_SECOND = "source/current/second.cj"
        const val IMPORTED_FIRST_FIRST = "source/imported-first/first.cj"
        const val IMPORTED_FIRST_SECOND = "source/imported-first/second.cj"
        const val IMPORTED_SECOND_FIRST = "source/imported-second/first.cj"
        const val IMPORTED_SECOND_SECOND = "source/imported-second/second.cj"
        val IMPORTS = listOf("positions.first", "positions.second", "positions.missing")

        fun encode(metadata: CjoPackageMetadata): Package =
            Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(metadata)))

        fun packageMetadata(name: String, files: List<String>): CjoPackageMetadata = CjoPackageMetadata(
            fullPackageName = name,
            moduleName = name,
            schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3,
            allFiles = files,
        )

        fun fileInfoWithGlobalIds(first: UInt, second: UInt): List<CjoFileInfoMetadata> = listOf(first, second).mapIndexed { index, fileId ->
            CjoFileInfoMetadata(
                fileId = fileId,
                begin = CjoPositionMetadata(file = index.toUInt() + 1u, line = 1, column = 1),
                end = CjoPositionMetadata(file = index.toUInt() + 1u, line = 100, column = 1),
            )
        }
    }
}
