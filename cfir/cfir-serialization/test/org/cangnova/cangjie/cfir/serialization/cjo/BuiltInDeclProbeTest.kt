package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.BuiltInInfo
import PackageFormat.BuiltInType
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.Package
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import java.nio.file.Files
import java.nio.ByteBuffer
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 验证官方 `std.core.cjo` 中 `BuiltInDecl` 的编码和 CFIR 反序列化结果。
 */
class BuiltInDeclProbeTest {
    @Test
    fun `std core contains the five builtin declarations`() {
        val pkg = stdCorePackage()
        val builtIns = builtInDecls(pkg)

        assertEquals("std.core", pkg.fullPkgName)
        assertEquals(5, builtIns.size)
        assertEquals(
            mapOf(
                "RawArray" to BuiltInType.Array,
                "VArray" to BuiltInType.VArray,
                "CPointer" to BuiltInType.CPointer,
                "CString" to BuiltInType.CString,
                "CFunc" to BuiltInType.CFunc,
            ),
            builtIns.associate { it.decl.identifier!! to it.info!!.builtInType },
        )
        assertEquals(
            mapOf(
                "RawArray" to 1,
                "VArray" to 1,
                "CPointer" to 1,
                "CString" to 0,
                "CFunc" to 1,
            ),
            builtIns.associate { it.decl.identifier!! to (it.decl.generic?.typeParametersLength ?: 0) },
        )
        assertEquals(1, builtIns.single { it.decl.identifier == "CPointer" }.decl.generic?.constraintsLength)
        assertTrue(builtIns.all { it.decl.isTopLevel })
    }

    @Test
    fun `std core builtin declarations deserialize with generic shape and pointer bound`() {
        val tempDir = Files.createTempDirectory("cjo-builtin-decl-")
        try {
            val bytes = stdCoreBytes()
            val target = tempDir.resolve(CjoConstants.packageNameToPath("std.core"))
            target.parent?.createDirectories()
            target.outputStream().use { it.write(bytes) }

            val manager = CjoManager(
                CjoSearchPath { key ->
                    if (key == "CANGJIE_STDLIB_MODULE") tempDir.toString() else null
                },
            )
            val header = assertNotNull(manager.loadPackageHeader("std.core"))
            val pkg = assertNotNull(manager.loadPackage("std.core"))
            val context = CfirDeserializationContext(
                pkg = pkg,
                header = header,
                moduleData = BuiltInTestModuleData,
                cjoManager = manager,
            )

            val expectedKinds = mapOf(
                "RawArray" to CfirBuiltInTypeKind.ARRAY,
                "VArray" to CfirBuiltInTypeKind.VARRAY,
                "CPointer" to CfirBuiltInTypeKind.CPOINTER,
                "CString" to CfirBuiltInTypeKind.CSTRING,
                "CFunc" to CfirBuiltInTypeKind.CFUNC,
            )
            val declarations = expectedKinds.map { (name, kind) ->
                val index = assertNotNull(header.topLevelClassifierNameToIndices[name]).single()
                val declaration = assertIs<CfirBuiltInDeclaration>(
                    context.createDeclDeserializer().deserializeDecl(index),
                )
                assertEquals(kind, declaration.kind)
                assertEquals(name, declaration.name.asString())
                assertEquals(kind.typeParameterCount, declaration.typeParameters.size)
                assertTrue(declaration.superTypeRefs.isEmpty())
                assertEquals(CfirDeclarationOrigin.Library, declaration.origin)
                name to declaration
            }.toMap()

            val pointerParameter = assertIs<CfirTypeParameter>(declarations.getValue("CPointer").typeParameters.single())
            val pointerBound = assertIs<ConeClassLikeType>(pointerParameter.bounds.single().coneTypeOrNull)
            assertEquals(StdlibClassIds.CType, pointerBound.classId)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    private data class BuiltInDeclView(
        val decl: Decl,
        val info: BuiltInInfo?,
    )

    private fun builtInDecls(pkg: Package): List<BuiltInDeclView> {
        return (0 until pkg.allDeclsLength).mapNotNull { index ->
            val decl = pkg.allDecls(index) ?: return@mapNotNull null
            if (decl.kind != DeclKind.BuiltInDecl) return@mapNotNull null
            BuiltInDeclView(decl, decl.info(BuiltInInfo()) as? BuiltInInfo)
        }
    }

    private fun stdCorePackage(): Package = Package.getRootAsPackage(ByteBuffer.wrap(stdCoreBytes()))

    private fun stdCoreBytes(): ByteArray = javaClass.classLoader
        .getResourceAsStream("cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo")
        ?.use { it.readBytes() }
        ?: fail("std.core.cjo not found on test classpath")

    /** 反序列化内建声明测试使用的最小库模块数据。 */
    private object BuiltInTestModuleData : CfirModuleData() {
        override val name: Name = Name.identifier("builtin-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = org.cangnova.cangjie.cfir.common.CfirPlatform.DEFAULT
        override val isCommon: Boolean = false
        override val stableModuleName: String = "builtin-test"
        override val session: CfirSession
            get() = BuiltInTestSession

        init {
            bindSession(BuiltInTestSession)
        }
    }

    /** 内建声明测试使用的库 session。 */
    private object BuiltInTestSession : CfirSession(CfirSession.Kind.Library) {
        init {
            register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
    }
}
