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
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalUnsignedTypes::class)
/**
 * 验证 CJO FullId 解析器和类型反序列化器对当前包、导入包和非法嵌套声明的处理。
 */
class CjoFullIdResolverTest {
    /**
     * 验证 FullId resolver 遵循官方 C++ 的 pkgId、decl、index 解析契约。
     */
    @Test
    fun `resolver follows official cpp FullId contract`() {
        val fixture = FullIdTestFixture.create()
        val resolver = fixture.context.fullIdResolver

        assertEquals(
            "main.pkg.LocalLeaf",
            resolver.resolveClassId(createFullId(pkgId = -2, index = 2u))?.asFqNameString(),
        )
        assertEquals(
            "dep.pkg.Outer",
            resolver.resolveClassId(createFullId(pkgId = 0, decl = "dep::Outer"))?.asFqNameString(),
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

    /**
     * 验证类型反序列化器会通过 FullId resolver 解析当前包和导入包中的类标识。
     */
    @Test
    fun `type deserializer resolves current and imported class ids via FullId resolver`() {
        val fixture = FullIdTestFixture.create()
        val deserializer = CfirTypeDeserializer(fixture.context)

        assertEquals(
            "main.pkg.LocalLeaf",
            deserializer.deserializeType(0).requireResolvedClassId().asFqNameString(),
        )
        assertEquals(
            "dep.pkg.Outer",
            deserializer.deserializeType(1).requireResolvedClassId().asFqNameString(),
        )
        assertEquals(
            "dep.pkg.Leaf",
            deserializer.deserializeType(2).requireResolvedClassId().asFqNameString(),
        )
    }

    /**
     * 验证仓颉不支持嵌套类声明时，FullId resolver 会拒绝嵌套 class-like 声明。
     */
    @Test
    fun `resolver rejects nested class like declarations because cangjie has no nested classes`() {
        val fixture = FullIdTestFixture.createWithNestedClassLikeDeclarations()
        val resolver = fixture.context.fullIdResolver

        assertEquals(
            null,
            resolver.resolveClassId(createFullId(pkgId = -2, index = 2u)),
        )
        assertEquals(
            null,
            resolver.resolveClassId(createFullId(pkgId = 0, decl = "dep::Outer.Inner")),
        )
    }

    /**
     * FullId 解析测试夹具，封装临时 CJO 包、manager 和反序列化上下文。
     */
    private class FullIdTestFixture(
        /**
         * 待测反序列化上下文。
         */
        val context: CfirDeserializationContext,
    ) {
        companion object {
            /**
             * 构造包含当前包声明、导入包声明和类型表引用的普通 FullId 测试夹具。
             */
            fun create(): FullIdTestFixture {
                val tempDir = createTempDirectory("cjo-fullid-test").toFile()

                tempDir.resolve("dep.cjo").writeBytes(
                    buildPackageBytes(
                        fullPackageName = "dep.pkg",
                        decls = listOf(
                            DeclSpec(
                                identifier = "Outer",
                                exportId = "dep::Outer",
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
                            ),
                            DeclSpec(
                                identifier = "LocalLeaf",
                                exportId = "main::LocalLeaf",
                            ),
                        ),
                        types = listOf(
                            TypeSpec(fullIdPkgId = -2, fullIdIndex = 2u),
                            TypeSpec(fullIdPkgId = 0, fullIdDecl = "dep::Outer"),
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

            /**
             * 构造包含嵌套 class-like 声明的非法 FullId 测试夹具。
             */
            fun createWithNestedClassLikeDeclarations(): FullIdTestFixture {
                val tempDir = createTempDirectory("cjo-fullid-nested-test").toFile()

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
        /**
         * 测试 CJO 声明规格。
         */
        private data class DeclSpec(
            /**
             * 声明短名。
             */
            val identifier: String,
            /**
             * CJO exportId 字段；为空表示只能按声明短名解析。
             */
            val exportId: String?,
            /**
             * 声明是否为顶层声明。
             */
            val isTopLevel: Boolean = true,
            /**
             * 类声明 body 中引用的声明索引，用于模拟嵌套声明。
             */
            val bodyFormattedDeclIndices: UIntArray = UIntArray(0),
        )

        /**
         * 测试 CJO 类型表中的 FullId 引用规格。
         */
        private data class TypeSpec(
            /**
             * FullId 的 pkgId 字段。
             */
            val fullIdPkgId: Int,
            /**
             * FullId 的 decl 字段。
             */
            val fullIdDecl: String? = null,
            /**
             * FullId 的 index 字段。
             */
            val fullIdIndex: UInt = 0u,
        )

        /**
         * 构造最小 CJO Package FlatBuffers 字节数组。
         */
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

        /**
         * 构造单个 FullId FlatBuffers 对象。
         */
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

        /**
         * 从反序列化类型中提取已解析的 classId。
         */
        private fun ConeCangJieType.requireResolvedClassId(): ClassId {
            val lookupTag = assertInstanceOf(
                ConeClassLikeLookupTagImpl::class.java,
                assertInstanceOf(ConeClassLikeType::class.java, this).lookupTag,
            )
            return lookupTag.classId
        }

        /**
         * FullId 测试使用的库 session。
         */
        private object TestSession : CfirSession(Kind.Library) {
            /**
             * 返回稳定的测试 session 名称，便于断言失败时定位。
             */
            override fun toString(): String = "CjoFullIdResolverTestSession"
        }

        /**
         * FullId 测试使用的模块数据。
         */
        private object TestModuleData : CfirModuleData() {
            /**
             * 测试模块名称。
             */
            override val name: Name = Name.identifier("cfir-serialization-test")
            /**
             * 测试模块无普通依赖。
             */
            override val dependencies: List<CfirModuleData> = emptyList()
            /**
             * 测试模块无 refinement 依赖。
             */
            override val refinementDependencies: List<CfirModuleData> = emptyList()
            /**
             * 测试模块无传递 refinement 依赖。
             */
            override val allRefinementDependencies: List<CfirModuleData> = emptyList()
            /**
             * 使用默认仓颉目标平台。
             */
            override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
            /**
             * 使用默认 CFIR 平台。
             */
            override val platform: CfirPlatform = CfirPlatform.DEFAULT
            /**
             * 标记模块是否为 common 平台。
             */
            override val isCommon: Boolean = targetPlatform.isCommon()
            /**
             * 测试模块不声明额外能力。
             */
            override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
            /**
             * 稳定模块名。
             */
            override val stableModuleName: String = "cfir-serialization-test"
            /**
             * 绑定到测试库 session。
             */
            override val session: CfirSession
                get() = TestSession

            init {
                bindSession(TestSession)
            }
        }
    }
}
