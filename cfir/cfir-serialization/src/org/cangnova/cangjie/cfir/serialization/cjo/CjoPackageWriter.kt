package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.CjoVersion
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.ImportSpec
import PackageFormat.Imports
import PackageFormat.Package
import PackageFormat.PackageAccessLevel
import PackageFormat.PackageKind
import com.google.flatbuffers.FlatBufferBuilder
import java.nio.file.Files
import java.nio.file.Path

/**
 * 写出 `.cjo` 包级元数据。
 *
 * 该 writer 只覆盖包头、导入、源文件列表和顶层导出声明索引，供宏 artifact
 * resolver、反序列化索引和后续 orchestration 复用。完整 CFIR/CHIR body
 * 序列化仍由后续二进制产物管线负责，不能在这里伪造。
 */
object CjoPackageWriter {
    fun write(path: Path, metadata: CjoPackageMetadata): Path {
        Files.createDirectories(path.parent)
        Files.write(path, toByteArray(metadata))
        return path
    }

    fun toByteArray(metadata: CjoPackageMetadata): ByteArray {
        val builder = FlatBufferBuilder(metadata.initialBufferSize)
        val packageNameOffset = builder.createString(metadata.fullPackageName)
        val moduleNameOffset = builder.createString(metadata.moduleName)
        val versionOffset = metadata.version?.let(builder::createString) ?: 0
        val packageDepInfoOffset = metadata.packageDependencyInfo?.let(builder::createString) ?: 0
        val importsOffset = metadata.imports
            .takeIf(List<String>::isNotEmpty)
            ?.map(builder::createString)
            ?.toIntArray()
            ?.let { Package.createImportsVector(builder, it) }
            ?: 0
        val allFilesOffset = metadata.allFiles
            .takeIf(List<String>::isNotEmpty)
            ?.map(builder::createString)
            ?.toIntArray()
            ?.let { Package.createAllFilesVector(builder, it) }
            ?: 0
        val allFileImportsOffset = metadata.fileImports
            .takeIf(List<CjoPackageFileImports>::isNotEmpty)
            ?.map { it.write(builder) }
            ?.toIntArray()
            ?.let { Package.createAllFileImportsVector(builder, it) }
            ?: 0
        val allDeclsOffset = metadata.declarations
            .takeIf(List<CjoPackageDeclaration>::isNotEmpty)
            ?.map { it.write(builder, metadata.fullPackageName, packageNameOffset) }
            ?.toIntArray()
            ?.let { Package.createAllDeclsVector(builder, it) }
            ?: 0

        Package.startPackage(builder)
        metadata.cjoVersion?.let { version ->
            val cjoVersionOffset = CjoVersion.createCjoVersion(
                builder,
                version.major,
                version.minor,
                version.patch,
            )
            Package.addCjoVersion(builder, cjoVersionOffset)
        }
        Package.addFullPkgName(builder, packageNameOffset)
        Package.addModuleName(builder, moduleNameOffset)
        if (versionOffset != 0) {
            Package.addVersion(builder, versionOffset)
        }
        if (packageDepInfoOffset != 0) {
            Package.addPkgDepInfo(builder, packageDepInfoOffset)
        }
        if (importsOffset != 0) {
            Package.addImports(builder, importsOffset)
        }
        if (allFilesOffset != 0) {
            Package.addAllFiles(builder, allFilesOffset)
        }
        if (allFileImportsOffset != 0) {
            Package.addAllFileImports(builder, allFileImportsOffset)
        }
        if (allDeclsOffset != 0) {
            Package.addAllDecls(builder, allDeclsOffset)
        }
        Package.addKind(builder, metadata.kind)
        Package.addAccess(builder, metadata.access)
        val packageOffset = Package.endPackage(builder)
        Package.finishPackageBuffer(builder, packageOffset)
        return builder.sizedByteArray()
    }

    private fun CjoPackageFileImports.write(builder: FlatBufferBuilder): Int {
        val importOffsets = imports.map { it.write(builder) }.toIntArray()
        val importSpecsOffset = Imports.createImportSpecsVector(builder, importOffsets)
        return Imports.createImports(builder, importSpecsOffset)
    }

    private fun CjoPackageImport.write(builder: FlatBufferBuilder): Int {
        val prefixPathsOffset = prefixPaths
            .map(builder::createString)
            .toIntArray()
            .let { ImportSpec.createPrefixPathsVector(builder, it) }
        val identifierOffset = builder.createString(identifier)
        val aliasOffset = alias?.let(builder::createString) ?: 0

        ImportSpec.startImportSpec(builder)
        ImportSpec.addPrefixPaths(builder, prefixPathsOffset)
        ImportSpec.addIdentifier(builder, identifierOffset)
        if (aliasOffset != 0) {
            ImportSpec.addAsIdentifier(builder, aliasOffset)
        }
        ImportSpec.addIsDecl(builder, isDecl)
        ImportSpec.addHasDoubleColon(builder, hasDoubleColon)
        ImportSpec.addWithImplicitExport(builder, withImplicitExport)
        return ImportSpec.endImportSpec(builder)
    }

    private fun CjoPackageDeclaration.write(
        builder: FlatBufferBuilder,
        defaultPackageName: String,
        defaultPackageNameOffset: Int,
    ): Int {
        val declarationPackageName = fullPackageName ?: defaultPackageName
        val packageNameOffset = if (declarationPackageName == defaultPackageName) {
            defaultPackageNameOffset
        } else {
            builder.createString(declarationPackageName)
        }
        val identifierOffset = builder.createString(identifier)
        val exportIdOffset = exportId?.let(builder::createString) ?: 0
        val mangledNameOffset = mangledName?.let(builder::createString) ?: 0

        Decl.startDecl(builder)
        Decl.addKind(builder, kind)
        Decl.addIsTopLevel(builder, isTopLevel)
        Decl.addFullPkgName(builder, packageNameOffset)
        Decl.addIdentifier(builder, identifierOffset)
        if (exportIdOffset != 0) {
            Decl.addExportId(builder, exportIdOffset)
        }
        if (mangledNameOffset != 0) {
            Decl.addMangledName(builder, mangledNameOffset)
        }
        return Decl.endDecl(builder)
    }
}

data class CjoPackageMetadata(
    val fullPackageName: String,
    val moduleName: String,
    val kind: UByte = PackageKind.Normal,
    val access: UByte = PackageAccessLevel.PUBLIC,
    val version: String? = null,
    val cjoVersion: CjoFormatVersion? = null,
    val packageDependencyInfo: String? = null,
    val imports: List<String> = emptyList(),
    val allFiles: List<String> = emptyList(),
    val fileImports: List<CjoPackageFileImports> = emptyList(),
    val declarations: List<CjoPackageDeclaration> = emptyList(),
    val initialBufferSize: Int = 1024,
) {
    init {
        require(fullPackageName.isNotBlank()) { "CJO package name must not be blank." }
        require(moduleName.isNotBlank()) { "CJO module name must not be blank." }
    }
}

data class CjoFormatVersion(
    val major: UByte,
    val minor: UByte,
    val patch: UByte,
)

data class CjoPackageDeclaration(
    val identifier: String,
    val kind: UShort = DeclKind.FuncDecl,
    val isTopLevel: Boolean = true,
    val fullPackageName: String? = null,
    val exportId: String? = null,
    val mangledName: String? = null,
) {
    init {
        require(identifier.isNotBlank()) { "CJO declaration identifier must not be blank." }
    }
}

data class CjoPackageFileImports(
    val imports: List<CjoPackageImport>,
)

data class CjoPackageImport(
    val prefixPaths: List<String>,
    val identifier: String,
    val alias: String? = null,
    val isDecl: Boolean = false,
    val hasDoubleColon: Boolean = false,
    val withImplicitExport: Boolean = true,
) {
    init {
        require(prefixPaths.isNotEmpty()) { "CJO import prefix must not be empty." }
        require(prefixPaths.all(String::isNotBlank)) { "CJO import prefix must not contain blank segments." }
        require(identifier.isNotBlank()) { "CJO import identifier must not be blank." }
    }
}
