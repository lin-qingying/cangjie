package org.cangnova.cangjie.chir.cjmp

import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider

import PackageFormat.DeclKind
import PackageFormat.Package
import org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationOrigin
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackageSnapshotProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirNullSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.chir.cfir2chir.Cfir2ChirConversionException
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.file.Path

@OptIn(CfirSymbolProviderInternals::class)
class CfirCjmpChirSemanticIdentityProviderFactoryTest {
    @Test
    fun `resolves official CHIR identity through exact CJO package snapshot and declaration index`() {
        val snapshot = packageSnapshot(
            path = Path.of("build", "fixtures", "common.cjo"),
            declarations = listOf(
                CjoPackageDeclaration(
                    identifier = "first",
                    kind = DeclKind.FuncDecl,
                    mangledName = "firstPostSemaMangle",
                ),
                CjoPackageDeclaration(
                    identifier = "shared",
                    kind = DeclKind.FuncDecl,
                    mangledName = "sharedPostSemaMangle",
                ),
            ),
        )
        val provider = snapshotsOf(snapshot)

        assertEquals(
            ChirSemanticId("@sharedPostSemaMangle"),
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(snapshot.sourcePath.toString(), declarationIndex = 1),
                provider,
            ),
        )
    }

    @Test
    fun `uses official CJO export signature when generic function post-sema mangle is omitted`() {
        val snapshot = packageSnapshot(
            path = Path.of("build", "fixtures", "generic-member.cjo"),
            declarations = listOf(
                CjoPackageDeclaration(
                    identifier = "transform",
                    kind = DeclKind.FuncDecl,
                    exportId = "_CN23pkg3BoxIG_E9transformH",
                    mangledBeforeSema = "23pkg3Box<1>S9transformF\$\$",
                ),
            ),
        )

        assertEquals(
            ChirSemanticId("@_CN23pkg3BoxIG_E9transformH"),
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(snapshot.sourcePath.toString(), declarationIndex = 0),
                snapshotsOf(snapshot),
            ),
        )
    }

    @Test
    fun nominalIdentityUsesCjoSourcePackageAndDeclarationName() {
        val firstSnapshot = packageSnapshot(
            path = Path.of("build", "fixtures", "common.cjo"),
            declarations = listOf(
                CjoPackageDeclaration(identifier = "Box", kind = DeclKind.ClassDecl),
            ),
        )
        val relocatedSnapshot = packageSnapshot(
            path = Path.of("build", "fixtures", "relocated", "common.cjo"),
            declarations = listOf(
                CjoPackageDeclaration(identifier = "Box", kind = DeclKind.ClassDecl),
            ),
        )

        val firstIdentity = CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
            CfirCjoDeclarationOrigin(firstSnapshot.sourcePath.toString(), declarationIndex = 0),
            snapshotsOf(firstSnapshot),
        )

        assertEquals(
            ChirSemanticId("cfir:cjmp:cjo-nominal:10:common.pkg:3:Box:CLASS"),
            firstIdentity,
        )
        assertEquals(
            firstIdentity,
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(relocatedSnapshot.sourcePath.toString(), declarationIndex = 0),
                snapshotsOf(relocatedSnapshot),
            ),
        )
    }

    @Test
    fun `uses official raw mangle without declaration prefix for FOREIGN functions`() {
        val foreignAttribute = Attribute.FOREIGN.ordinal
        val foreignAttributeWord = foreignAttribute / ULong.SIZE_BITS
        val attributes = List(foreignAttributeWord + 1) { wordIndex ->
            if (wordIndex == foreignAttributeWord) {
                1uL shl (foreignAttribute % ULong.SIZE_BITS)
            } else {
                0uL
            }
        }
        val snapshot = packageSnapshot(
            path = Path.of("build", "fixtures", "foreign.cjo"),
            declarations = listOf(
                CjoPackageDeclaration(
                    identifier = "foreignApi",
                    kind = DeclKind.FuncDecl,
                    mangledName = "foreignPostSemaMangle",
                    mangledBeforeSema = "foreignRawMangle",
                    attributes = attributes,
                ),
            ),
        )

        assertEquals(
            ChirSemanticId("foreignRawMangle"),
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(snapshot.sourcePath.toString(), declarationIndex = 0),
                snapshotsOf(snapshot),
            ),
        )
    }

    @Test
    fun `rejects FOREIGN function without official raw mangle`() {
        val foreignAttribute = Attribute.FOREIGN.ordinal
        val foreignAttributeWord = foreignAttribute / ULong.SIZE_BITS
        val snapshot = packageSnapshot(
            path = Path.of("build", "fixtures", "foreign-missing-raw.cjo"),
            declarations = listOf(
                CjoPackageDeclaration(
                    identifier = "foreignApi",
                    kind = DeclKind.FuncDecl,
                    mangledName = "foreignPostSemaMangle",
                    attributes = List(foreignAttributeWord + 1) { wordIndex ->
                        if (wordIndex == foreignAttributeWord) {
                            1uL shl (foreignAttribute % ULong.SIZE_BITS)
                        } else {
                            0uL
                        }
                    },
                ),
            ),
        )

        assertThrows(Cfir2ChirConversionException::class.java) {
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(snapshot.sourcePath.toString(), declarationIndex = 0),
                snapshotsOf(snapshot),
            )
        }
    }

    @Test
    fun `rejects origins outside the active provider generation and missing mangled names`() {
        val snapshot = packageSnapshot(
            path = Path.of("build", "fixtures", "common.cjo"),
            declarations = listOf(CjoPackageDeclaration(identifier = "shared", kind = DeclKind.FuncDecl)),
        )
        val provider = snapshotsOf(snapshot)

        assertThrows(Cfir2ChirConversionException::class.java) {
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(Path.of("build", "fixtures", "other.cjo").toString(), 0),
                provider,
            )
        }
        assertThrows(Cfir2ChirConversionException::class.java) {
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(snapshot.sourcePath.toString(), 1),
                provider,
            )
        }
        assertThrows(Cfir2ChirConversionException::class.java) {
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(snapshot.sourcePath.toString(), 0),
                provider,
            )
        }
    }

    @Test
    fun `resolves exact CJO snapshots recursively through the active dependency provider graph`() {
        val path = Path.of("build", "fixtures", "composite.cjo")
        val snapshot = packageSnapshot(
            path = path,
            declarations = listOf(
                CjoPackageDeclaration(
                    identifier = "shared",
                    kind = DeclKind.FuncDecl,
                    mangledName = "sharedFromComposite",
                ),
            ),
        )
        val dependencies = CfirCompositeSymbolProvider(
            TestSession,
            listOf(
                symbolProvider(),
                symbolProvider(snapshotsOf(snapshot)),
            ),
        )

        assertEquals(
            ChirSemanticId("@sharedFromComposite"),
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(snapshot.sourcePath.toString(), declarationIndex = 0),
                dependencies,
            ),
        )
    }

    @Test
    fun `accepts repeated source snapshots only when they preserve the same official identity`() {
        val path = Path.of("build", "fixtures", "ambiguous.cjo")
        val first = packageSnapshot(
            path,
            listOf(CjoPackageDeclaration(identifier = "shared", kind = DeclKind.FuncDecl, mangledName = "same")),
        )
        val sameIdentity = packageSnapshot(path, listOf(CjoPackageDeclaration(
            identifier = "shared",
            kind = DeclKind.FuncDecl,
            mangledName = "same",
        )))
        val dependencies = CfirCompositeSymbolProvider(
            TestSession,
            listOf(symbolProvider(snapshotsOf(first)), symbolProvider(snapshotsOf(sameIdentity))),
        )

        assertEquals(
            ChirSemanticId("@same"),
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(first.sourcePath.toString(), declarationIndex = 0),
                dependencies,
            ),
        )

        val conflicting = packageSnapshot(
            path,
            listOf(CjoPackageDeclaration(identifier = "shared", kind = DeclKind.FuncDecl, mangledName = "different")),
        )
        val conflictingDependencies = CfirCompositeSymbolProvider(
            TestSession,
            listOf(symbolProvider(snapshotsOf(first)), symbolProvider(snapshotsOf(conflicting))),
        )
        assertThrows(Cfir2ChirConversionException::class.java) {
            CfirCjmpChirSemanticIdentityProviderFactory.identityOf(
                CfirCjoDeclarationOrigin(first.sourcePath.toString(), declarationIndex = 0),
                conflictingDependencies,
            )
        }
    }

    private fun packageSnapshot(path: Path, declarations: List<CjoPackageDeclaration>): CjoLoadedPackage {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "common.pkg",
                moduleName = "common",
                declarations = declarations,
            ),
        )
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        return object : CjoLoadedPackage {
            override val pkg: Package = pkg
            override val header: CjoPackageHeader = CjoPackageHeader.fromPackage(pkg)
            override val sourcePath: Path = path.toAbsolutePath().normalize()
        }
    }

    private fun snapshotsOf(snapshot: CjoLoadedPackage): CjoLoadedPackageSnapshotProvider =
        object : CjoLoadedPackageSnapshotProvider {
            override fun loadedPackageSnapshot(sourcePath: Path): CjoLoadedPackage? =
                snapshot.takeIf { sourcePath.toAbsolutePath().normalize() == snapshot.sourcePath }
        }

    private fun symbolProvider(
        snapshots: CjoLoadedPackageSnapshotProvider? = null,
    ): CfirSymbolProvider = object : CfirSymbolProvider(TestSession), CjoLoadedPackageSnapshotProvider {
        override val symbolNamesProvider = CfirNullSymbolNamesProvider

        override fun getClassLikeSymbolByClassId(classId: ClassId) = null

        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        override fun hasPackage(fqName: FqName) = false

        override fun loadedPackageSnapshot(sourcePath: Path): CjoLoadedPackage? =
            snapshots?.loadedPackageSnapshot(sourcePath)
    }

    private object TestSession : CfirSession(CfirSession.Kind.Library)
}
