package org.cangnova.cangjie.cfir.serialization.cjo

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CjoPackageHeaderTest {
    @Test
    fun `top level anonymous extend is indexed independently from declaration names`() {
        val builder = FlatBufferBuilder(256)
        val packageNameOffset = builder.createString("std.core")
        val moduleNameOffset = builder.createString("std")
        val toStringNameOffset = builder.createString("ToString")

        val toStringTypeOffset = run {
            val declOffset = builder.createString("ToString")
            val fullIdOffset = FullId.createFullId(builder, -2, declOffset, 1u)
            val infoOffset = CompositeTyInfo.createCompositeTyInfo(builder, fullIdOffset, false)
            SemaTy.createSemaTy(builder, TypeKind.Interface, 0, SemaTyInfo.CompositeTyInfo, infoOffset)
        }

        val extendInfoOffset = PackageFormat.ExtendInfo.createExtendInfo(builder, 0, 0)
        val extendDeclOffset = run {
            Decl.startDecl(builder)
            Decl.addKind(builder, DeclKind.ExtendDecl)
            Decl.addIsTopLevel(builder, true)
            Decl.addFullPkgName(builder, packageNameOffset)
            Decl.addType(builder, toStringTypeOffset.toUInt())
            Decl.addInfoType(builder, DeclInfo.ExtendInfo)
            Decl.addInfo(builder, extendInfoOffset)
            Decl.endDecl(builder)
        }

        val interfaceInfoOffset = PackageFormat.InterfaceInfo.createInterfaceInfo(builder, 0, 0)
        val interfaceDeclOffset = run {
            Decl.startDecl(builder)
            Decl.addKind(builder, DeclKind.InterfaceDecl)
            Decl.addIsTopLevel(builder, true)
            Decl.addFullPkgName(builder, packageNameOffset)
            Decl.addIdentifier(builder, toStringNameOffset)
            Decl.addInfoType(builder, DeclInfo.InterfaceInfo)
            Decl.addInfo(builder, interfaceInfoOffset)
            Decl.endDecl(builder)
        }

        val allDeclsOffset = Package.createAllDeclsVector(builder, intArrayOf(extendDeclOffset, interfaceDeclOffset))

        Package.startPackage(builder)
        Package.addFullPkgName(builder, packageNameOffset)
        Package.addModuleName(builder, moduleNameOffset)
        Package.addAllDecls(builder, allDeclsOffset)
        val packageOffset = Package.endPackage(builder)
        Package.finishPackageBuffer(builder, packageOffset)

        val pkg = Package.getRootAsPackage(java.nio.ByteBuffer.wrap(builder.sizedByteArray()))
        val header = CjoPackageHeader.fromPackage(pkg)

        assertEquals(listOf(0), header.topLevelExtendIndices)
        assertTrue(
            "ToString" in header.topLevelNameToIndices,
            "named declarations should remain indexed by name",
        )
    }
}
