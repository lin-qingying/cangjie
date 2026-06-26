package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.CompositeTyInfo
import PackageFormat.Decl
import PackageFormat.DeclInfo
import PackageFormat.DeclKind
import PackageFormat.FullId
import PackageFormat.Imports
import PackageFormat.ImportSpec
import PackageFormat.Package
import PackageFormat.SemaTy
import PackageFormat.SemaTyInfo
import PackageFormat.TypeKind
import com.google.flatbuffers.FlatBufferBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证 CJO 包头读取逻辑对声明索引、文件列表和导入文本的解析。
 */
class CjoPackageHeaderTest {
    /**
     * 验证顶层匿名 extend 按独立索引记录，不污染具名声明索引。
     */
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

    /**
     * 验证所有源文件路径都会保留，供反编译多文件推断使用。
     */
    @Test
    fun `all files are preserved for decompiled multifile inference`() {
        val builder = FlatBufferBuilder(128)
        val packageNameOffset = builder.createString("sample.pkg")
        val moduleNameOffset = builder.createString("sample")
        val alphaFileOffset = builder.createString("src/sample/pkg/alpha.cj")
        val betaFileOffset = builder.createString("src/sample/pkg/beta.cj")
        val allFilesOffset = Package.createAllFilesVector(builder, intArrayOf(alphaFileOffset, betaFileOffset))

        Package.startPackage(builder)
        Package.addFullPkgName(builder, packageNameOffset)
        Package.addModuleName(builder, moduleNameOffset)
        Package.addAllFiles(builder, allFilesOffset)
        val packageOffset = Package.endPackage(builder)
        Package.finishPackageBuffer(builder, packageOffset)

        val pkg = Package.getRootAsPackage(java.nio.ByteBuffer.wrap(builder.sizedByteArray()))
        val header = CjoPackageHeader.fromPackage(pkg)

        assertEquals(listOf("src/sample/pkg/alpha.cj", "src/sample/pkg/beta.cj"), header.allFiles)
    }

    /**
     * 验证文件级导入条目会保留别名导入和星号导入文本。
     */
    @Test
    fun `file import entries preserve alias and star imports`() {
        val builder = FlatBufferBuilder(256)
        val packageNameOffset = builder.createString("sample.pkg")
        val moduleNameOffset = builder.createString("sample")
        val samplePrefixOffset = builder.createString("sample")
        val depPrefixOffset = builder.createString("dep")
        val starPrefixOffset = builder.createString("star")
        val thingIdentifierOffset = builder.createString("Thing")
        val aliasIdentifierOffset = builder.createString("AliasThing")
        val starIdentifierOffset = builder.createString("*")

        val aliasPrefixVector = ImportSpec.createPrefixPathsVector(
            builder,
            intArrayOf(samplePrefixOffset, depPrefixOffset),
        )
        val aliasImportOffset = run {
            ImportSpec.startImportSpec(builder)
            ImportSpec.addPrefixPaths(builder, aliasPrefixVector)
            ImportSpec.addIdentifier(builder, thingIdentifierOffset)
            ImportSpec.addAsIdentifier(builder, aliasIdentifierOffset)
            ImportSpec.addIsDecl(builder, false)
            ImportSpec.endImportSpec(builder)
        }

        val starPrefixVector = ImportSpec.createPrefixPathsVector(
            builder,
            intArrayOf(samplePrefixOffset, starPrefixOffset),
        )
        val starImportOffset = run {
            ImportSpec.startImportSpec(builder)
            ImportSpec.addPrefixPaths(builder, starPrefixVector)
            ImportSpec.addIdentifier(builder, starIdentifierOffset)
            ImportSpec.addIsDecl(builder, false)
            ImportSpec.endImportSpec(builder)
        }

        val importSpecsVector = Imports.createImportSpecsVector(builder, intArrayOf(aliasImportOffset, starImportOffset))
        val importsOffset = Imports.createImports(builder, importSpecsVector)
        val allFileImportsOffset = Package.createAllFileImportsVector(builder, intArrayOf(importsOffset))

        Package.startPackage(builder)
        Package.addFullPkgName(builder, packageNameOffset)
        Package.addModuleName(builder, moduleNameOffset)
        Package.addAllFileImports(builder, allFileImportsOffset)
        val packageOffset = Package.endPackage(builder)
        Package.finishPackageBuffer(builder, packageOffset)

        val pkg = Package.getRootAsPackage(java.nio.ByteBuffer.wrap(builder.sizedByteArray()))
        val header = CjoPackageHeader.fromPackage(pkg)

        assertEquals(listOf("sample.dep.Thing as AliasThing", "sample.star.*"), header.decompiledImportTexts)
    }

    /**
     * 验证导入前缀中的双冒号组织分隔符会被正确还原。
     */
    @Test
    fun `file import entries preserve double colon organization separator`() {
        val builder = FlatBufferBuilder(256)
        val packageNameOffset = builder.createString("sample.pkg")
        val moduleNameOffset = builder.createString("sample")

        val prefixVector = ImportSpec.createPrefixPathsVector(
            builder,
            intArrayOf(builder.createString("org"), builder.createString("sample")),
        )
        val importOffset = run {
            val identifierOffset = builder.createString("Thing")
            ImportSpec.startImportSpec(builder)
            ImportSpec.addPrefixPaths(builder, prefixVector)
            ImportSpec.addIdentifier(builder, identifierOffset)
            ImportSpec.addHasDoubleColon(builder, true)
            ImportSpec.addIsDecl(builder, false)
            ImportSpec.endImportSpec(builder)
        }

        val importSpecsVector = Imports.createImportSpecsVector(builder, intArrayOf(importOffset))
        val importsOffset = Imports.createImports(builder, importSpecsVector)
        val allFileImportsOffset = Package.createAllFileImportsVector(builder, intArrayOf(importsOffset))

        Package.startPackage(builder)
        Package.addFullPkgName(builder, packageNameOffset)
        Package.addModuleName(builder, moduleNameOffset)
        Package.addAllFileImports(builder, allFileImportsOffset)
        val packageOffset = Package.endPackage(builder)
        Package.finishPackageBuffer(builder, packageOffset)

        val pkg = Package.getRootAsPackage(java.nio.ByteBuffer.wrap(builder.sizedByteArray()))
        val header = CjoPackageHeader.fromPackage(pkg)

        assertEquals(listOf("org::sample.Thing"), header.decompiledImportTexts)
    }
}
