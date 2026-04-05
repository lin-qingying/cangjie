package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.ClassInfo
import PackageFormat.CompositeTyInfo
import PackageFormat.Decl
import PackageFormat.DeclInfo
import PackageFormat.DeclKind
import PackageFormat.FullId
import PackageFormat.Package
import PackageFormat.SemaTy
import PackageFormat.SemaTyInfo
import PackageFormat.TypeKind
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalUnsignedTypes::class)
class CjoFullIdResolverTest {
    @Test
    fun `resolver follows official cpp FullId contract`() {
        val fixture = FullIdTestFixture.create()
        val resolver = fixture.context.fullIdResolver

        assertEquals(
            "main.pkg.LocalOuter.LocalInner",
            resolver.resolveClassId(createFullId(pkgId = -2, index = 2u))?.asFqNameString(),
        )
        assertEquals(
            "dep.pkg.Outer.Inner",
            resolver.resolveClassId(createFullId(pkgId = 0, decl = "dep::Outer.Inner"))?.asFqNameString(),
        )
        assertEquals(
            "dep.pkg.Leaf",
            resolver.resolveClassId(createFullId(pkgId = 0, decl = "Leaf"))?.asFqNameString(),
        )

        val packageReference = resolver.resolve(createFullId(pkgId = -3, decl = "dep.pkg"))
        assertEquals(
            "dep.pkg",
            assertInstanceOf(ResolvedFullId.PackageReference::class.java, packageReference).packageFqName.asString(),
        )

        assertInstanceOf(
            ResolvedFullId.Invalid::class.java,
            resolver.resolve(createFullId(pkgId = -1)),
        )
    }

    @Test
    fun `type deserializer resolves current and imported class ids via FullId resolver`() {
        val fixture = FullIdTestFixture.create()
        val deserializer = CfirTypeDeserializer(fixture.context)

        assertEquals(
            "main.pkg.LocalOuter.LocalInner",
            deserializer.deserializeType(0).requireResolvedClassId().asFqNameString(),
        )
        assertEquals(
            "dep.pkg.Outer.Inner",
            deserializer.deserializeType(1).requireResolvedClassId().asFqNameString(),
        )
        assertEquals(
            "dep.pkg.Leaf",
            deserializer.deserializeType(2).requireResolvedClassId().asFqNameString(),
        )
    }

    private class FullIdTestFixture(
        val context: CfirDeserializationContext,
    ) {
        companion object {
            fun create(): FullIdTestFixture {
                val tempDir = createTempDirectory("cjo-fullid-test").toFile()

                tempDir.resolve("dep.cjo").writeBytes(
                    buildPackageBytes(
                        fullPackageName = "dep.pkg",
                        decls = listOf(
                            DeclSpec(
                                identifier = "Outer",
                                exportId = "dep::Outer",
                                bodyFormattedDeclIndices = uintArrayOf(2u),
                            ),
                            DeclSpec(
                                identifier = "Inner",
                                exportId = "dep::Outer.Inner",
                                isTopLevel = false,
                            ),
                            DeclSpec(
                                identifier = "Leaf",
                                exportId = null,
                            ),
                        ),
                    ),
                )

                tempDir.resolve("main.cjo").writeBytes(
                    buildPackageBytes(
                        fullPackageName = "main.pkg",
                        imports = listOf("dep.pkg"),
                        decls = listOf(
                            DeclSpec(
                                identifier = "LocalOuter",
                                exportId = "main::LocalOuter",
                                bodyFormattedDeclIndices = uintArrayOf(2u),
                            ),
                            DeclSpec(
                                identifier = "LocalInner",
                                exportId = "main::LocalOuter.LocalInner",
                                isTopLevel = false,
                            ),
                        ),
                        types = listOf(
                            TypeSpec(fullIdPkgId = -2, fullIdIndex = 2u),
                            TypeSpec(fullIdPkgId = 0, fullIdDecl = "dep::Outer.Inner"),
                            TypeSpec(fullIdPkgId = 0, fullIdDecl = "Leaf"),
                        ),
                    ),
                )

                val searchPath = CjoSearchPath { envName ->
                    when (envName) {
                        "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> tempDir.absolutePath
                        else -> null
                    }
                }
                val cjoManager = CjoManager(searchPath)
                val pkg = requireNotNull(cjoManager.loadPackage("main.pkg"))
                val header = requireNotNull(cjoManager.loadPackageHeader("main.pkg"))

                return FullIdTestFixture(
                    CfirDeserializationContext(
                        pkg = pkg,
                        header = header,
                        moduleData = TestModuleData,
                        cjoManager = cjoManager,
                    ),
                )
            }
        }
    }

    companion object {
        private data class DeclSpec(
            val identifier: String,
            val exportId: String?,
            val isTopLevel: Boolean = true,
            val bodyFormattedDeclIndices: UIntArray = UIntArray(0),
        )

        private data class TypeSpec(
            val fullIdPkgId: Int,
            val fullIdDecl: String? = null,
            val fullIdIndex: UInt = 0u,
        )

        private fun buildPackageBytes(
            fullPackageName: String,
            imports: List<String> = emptyList(),
            decls: List<DeclSpec>,
            types: List<TypeSpec> = emptyList(),
        ): ByteArray {
            val builder = FlatBufferBuilder(1024)

            val fullPackageNameOffset = builder.createString(fullPackageName)
            val moduleNameOffset = builder.createString(fullPackageName.substringAfterLast('.'))

            val declOffsets = decls.map { spec ->
                val identifierOffset = builder.createString(spec.identifier)
                val exportIdOffset = spec.exportId?.let(builder::createString) ?: 0
                val infoOffset = if (spec.bodyFormattedDeclIndices.isNotEmpty()) {
                    val bodyOffset = ClassInfo.createBodyVector(builder, spec.bodyFormattedDeclIndices)
                    ClassInfo.createClassInfo(builder, 0, bodyOffset, 0, false, 0u, false, 0u)
                } else {
                    0
                }

                Decl.startDecl(builder)
                Decl.addKind(builder, DeclKind.ClassDecl)
                Decl.addIsTopLevel(builder, spec.isTopLevel)
                Decl.addFullPkgName(builder, fullPackageNameOffset)
                Decl.addIdentifier(builder, identifierOffset)
                if (exportIdOffset != 0) {
                    Decl.addExportId(builder, exportIdOffset)
                }
                if (infoOffset != 0) {
                    Decl.addInfoType(builder, DeclInfo.ClassInfo)
                    Decl.addInfo(builder, infoOffset)
                }
                Decl.endDecl(builder)
            }

            val typeOffsets = types.map { spec ->
                val declOffset = spec.fullIdDecl?.let(builder::createString) ?: 0
                val fullIdOffset = FullId.createFullId(builder, spec.fullIdPkgId, declOffset, spec.fullIdIndex)
                val infoOffset = CompositeTyInfo.createCompositeTyInfo(builder, fullIdOffset, false)
                SemaTy.createSemaTy(builder, TypeKind.Class, 0, SemaTyInfo.CompositeTyInfo, infoOffset)
            }

            val importsOffset = if (imports.isNotEmpty()) {
                Package.createImportsVector(builder, imports.map(builder::createString).toIntArray())
            } else {
                0
            }
            val allDeclsOffset = if (declOffsets.isNotEmpty()) {
                Package.createAllDeclsVector(builder, declOffsets.toIntArray())
            } else {
                0
            }
            val allTypesOffset = if (typeOffsets.isNotEmpty()) {
                Package.createAllTypesVector(builder, typeOffsets.toIntArray())
            } else {
                0
            }

            Package.startPackage(builder)
            Package.addFullPkgName(builder, fullPackageNameOffset)
            Package.addModuleName(builder, moduleNameOffset)
            if (importsOffset != 0) {
                Package.addImports(builder, importsOffset)
            }
            if (allDeclsOffset != 0) {
                Package.addAllDecls(builder, allDeclsOffset)
            }
            if (allTypesOffset != 0) {
                Package.addAllTypes(builder, allTypesOffset)
            }
            val packageOffset = Package.endPackage(builder)
            Package.finishPackageBuffer(builder, packageOffset)
            return builder.sizedByteArray()
        }

        private fun createFullId(
            pkgId: Int,
            decl: String? = null,
            index: UInt = 0u,
        ): FullId {
            val builder = FlatBufferBuilder(128)
            val declOffset = decl?.let(builder::createString) ?: 0
            val fullIdOffset = FullId.createFullId(builder, pkgId, declOffset, index)
            builder.finish(fullIdOffset)
            return FullId.getRootAsFullId(ByteBuffer.wrap(builder.sizedByteArray()))
        }

        private fun ConeCangJieType.requireResolvedClassId(): ClassId {
            val lookupTag = assertInstanceOf(
                ConeClassLikeLookupTagImpl::class.java,
                assertInstanceOf(ConeClassLikeType::class.java, this).lookupTag,
            )
            return lookupTag.classId
        }

        private object TestSession : CfirSession(Kind.Library) {
            override fun toString(): String = "CjoFullIdResolverTestSession"
        }

        private object TestModuleData : CfirModuleData() {
            override val name: Name = Name.identifier("cfir-serialization-test")
            override val dependencies: List<CfirModuleData> = emptyList()
            override val refinementDependencies: List<CfirModuleData> = emptyList()
            override val allRefinementDependencies: List<CfirModuleData> = emptyList()
            override val platform: CfirPlatform = CfirPlatform.DEFAULT
            override val isCommon: Boolean = true
            override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
            override val stableModuleName: String = "cfir-serialization-test"
            override val session: CfirSession
                get() = TestSession

            init {
                bindSession(TestSession)
            }
        }
    }
}
