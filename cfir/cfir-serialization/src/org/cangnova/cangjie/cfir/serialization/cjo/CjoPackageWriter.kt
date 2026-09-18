@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.CjoVersion
import PackageFormat.Anno
import PackageFormat.AnnoArg
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.FullId
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
 * 该 writer 覆盖包头、导入、源文件列表、顶层导出声明索引以及 ModuleFormat
 * 已定义的声明 attributes/annotation metadata。完整 CFIR/CHIR body 序列化
 * 仍由后续二进制产物管线负责，不能在这里伪造。
 */
@OptIn(ExperimentalUnsignedTypes::class)
object CjoPackageWriter {
    /** 将 [metadata] 写入指定 `.cjo` 路径，并返回最终写入路径。 */
    fun write(path: Path, metadata: CjoPackageMetadata): Path {
        Files.createDirectories(path.parent)
        Files.write(path, toByteArray(metadata))
        return path
    }

    /**
     * 把包级元数据编码为 FlatBuffers 字节数组。
     *
     * 该方法写入包头、导入/源文件索引、声明索引、attributes 和结构化注解 metadata。
     */
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

    /** 写出单个源文件关联的 import 列表，并返回 FlatBuffers table offset。 */
    private fun CjoPackageFileImports.write(builder: FlatBufferBuilder): Int {
        val importOffsets = imports.map { it.write(builder) }.toIntArray()
        val importSpecsOffset = Imports.createImportSpecsVector(builder, importOffsets)
        return Imports.createImports(builder, importSpecsOffset)
    }

    /** 写出单条 import 规格，并返回 FlatBuffers table offset。 */
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

    /**
     * 写出顶层声明索引项，并返回 FlatBuffers table offset。
     *
     * [defaultPackageNameOffset] 用于复用包名字符串，避免同包声明重复写入相同字符串。
     */
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
        val attributeOffsets = attributes.toULongArray()
            .takeIf { it.isNotEmpty() }
            ?.let { Decl.createAttributesVector(builder, it) }
            ?: 0
        val annotationOffsets = annotations
            .takeIf(List<CjoAnnotationMetadata>::isNotEmpty)
            ?.map { it.write(builder) }
            ?.toIntArray()
            ?.let { Decl.createAnnotationsVector(builder, it) }
            ?: 0

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
        if (attributeOffsets != 0) {
            Decl.addAttributes(builder, attributeOffsets)
        }
        if (annotationOffsets != 0) {
            Decl.addAnnotations(builder, annotationOffsets)
        }
        return Decl.endDecl(builder)
    }

    /** 将结构化 annotation metadata 写入官方 ModuleFormat.Anno。 */
    private fun CjoAnnotationMetadata.write(builder: FlatBufferBuilder): Int {
        val identifierOffset = builder.createString(identifier)
        val argOffsets = arguments.map { argument ->
            val nameOffset = argument.name?.let(builder::createString) ?: 0
            AnnoArg.createAnnoArg(builder, nameOffset, argument.expr)
        }.toIntArray()
        val argsOffset = argOffsets
            .takeIf { it.isNotEmpty() }
            ?.let { Anno.createArgsVector(builder, it) }
            ?: 0
        val targetOffset = target?.let { value ->
            val declOffset = value.decl?.let(builder::createString) ?: 0
            FullId.createFullId(builder, value.pkgId, declOffset, value.index)
        } ?: 0
        return Anno.createAnno(builder, kind, identifierOffset, argsOffset, targetOffset)
    }
}

/**
 * `.cjo` 包级写出元数据。
 *
 * 该模型对应 [CjoPackageWriter] 支持的最小包头字段集合。
 */
data class CjoPackageMetadata(
    /** 完整包名。 */
    val fullPackageName: String,
    /** 模块名。 */
    val moduleName: String,
    /** 包种类，默认普通包。 */
    val kind: UByte = PackageKind.Normal,
    /** 包访问级别，默认 public。 */
    val access: UByte = PackageAccessLevel.PUBLIC,
    /** 旧格式版本字符串。 */
    val version: String? = null,
    /** 结构化 CJO 格式版本。 */
    val cjoVersion: CjoFormatVersion? = null,
    /** 包依赖信息的原始字符串。 */
    val packageDependencyInfo: String? = null,
    /** 包级导入文本列表。 */
    val imports: List<String> = emptyList(),
    /** package 包含的源文件名列表。 */
    val allFiles: List<String> = emptyList(),
    /** 按源文件组织的结构化 import 列表。 */
    val fileImports: List<CjoPackageFileImports> = emptyList(),
    /** 需要写入 `allDecls` 的声明索引项。 */
    val declarations: List<CjoPackageDeclaration> = emptyList(),
    /** FlatBuffers builder 初始缓冲区大小。 */
    val initialBufferSize: Int = 1024,
) {
    init {
        require(fullPackageName.isNotBlank()) { "CJO package name must not be blank." }
        require(moduleName.isNotBlank()) { "CJO module name must not be blank." }
    }
}

/** CJO FlatBuffers 格式版本号。 */
data class CjoFormatVersion(
    /** 主版本。 */
    val major: UByte,
    /** 次版本。 */
    val minor: UByte,
    /** 补丁版本。 */
    val patch: UByte,
)

/** `.cjo` 包头中的声明索引项。 */
data class CjoPackageDeclaration(
    /** 声明 identifier。 */
    val identifier: String,
    /** FlatBuffers 声明种类。 */
    val kind: UShort = DeclKind.FuncDecl,
    /** 是否为顶层声明。 */
    val isTopLevel: Boolean = true,
    /** 声明所属完整包名；为空时使用包默认名。 */
    val fullPackageName: String? = null,
    /** 跨包引用优先使用的 export id。 */
    val exportId: String? = null,
    /** 可选 mangled name。 */
    val mangledName: String? = null,
    /** 官方 Decl.attributes 位图；顺序和位宽按 ModuleFormat 保留。 */
    val attributes: List<ULong> = emptyList(),
    /** 已解析的声明注解 metadata；writer 不根据源码短名推断 kind。 */
    val annotations: List<CjoAnnotationMetadata> = emptyList(),
) {
    init {
        require(identifier.isNotBlank()) { "CJO declaration identifier must not be blank." }
    }
}

/** ModuleFormat.Anno 的稳定输入模型。 */
data class CjoAnnotationMetadata(
    /** PackageFormat.AnnoKind 的数值，不在 writer 内重新映射源码名称。 */
    val kind: UShort,
    /** 官方 annotation identifier。 */
    val identifier: String,
    /** 已绑定的参数名与常量表达式索引。 */
    val arguments: List<CjoAnnotationArgumentMetadata> = emptyList(),
    /** 可选的目标 FullId。 */
    val target: CjoAnnotationTargetMetadata? = null,
) {
    init {
        require(identifier.isNotBlank()) { "CJO annotation identifier must not be blank." }
    }
}

/** ModuleFormat.AnnoArg 的参数映射事实。 */
data class CjoAnnotationArgumentMetadata(
    val name: String? = null,
    val expr: UInt,
)

/** ModuleFormat.FullId 的目标引用。 */
data class CjoAnnotationTargetMetadata(
    val pkgId: Int = 0,
    val decl: String? = null,
    val index: UInt = 0u,
)

/** 单个源文件携带的 import 列表。 */
data class CjoPackageFileImports(
    /** 文件内 import 规格集合。 */
    val imports: List<CjoPackageImport>,
)

/** 写入 `.cjo` 的单条 import 规格。 */
data class CjoPackageImport(
    /** import 前缀路径。 */
    val prefixPaths: List<String>,
    /** import 成员名；all-under import 使用 `*`。 */
    val identifier: String,
    /** 可选 alias 名称。 */
    val alias: String? = null,
    /** 是否为 declaration import。 */
    val isDecl: Boolean = false,
    /** 是否使用双冒号连接首段路径。 */
    val hasDoubleColon: Boolean = false,
    /** 是否带隐式导出标记。 */
    val withImplicitExport: Boolean = true,
) {
    init {
        require(prefixPaths.isNotEmpty()) { "CJO import prefix must not be empty." }
        require(prefixPaths.all(String::isNotBlank)) { "CJO import prefix must not contain blank segments." }
        require(identifier.isNotBlank()) { "CJO import identifier must not be blank." }
    }
}
