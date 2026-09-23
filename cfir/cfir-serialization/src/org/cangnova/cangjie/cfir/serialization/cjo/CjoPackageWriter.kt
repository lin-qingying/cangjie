@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.CjoVersion
import PackageFormat.Anno
import PackageFormat.AnnoArg
import PackageFormat.AnnoKind
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.DeclInfo
import PackageFormat.Expr
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.FullId
import PackageFormat.ImportSpec
import PackageFormat.LitConstInfo
import PackageFormat.Imports
import PackageFormat.Package
import PackageFormat.FileInfo
import PackageFormat.PackageAccessLevel
import PackageFormat.PackageKind
import PackageFormat.ReferenceInfo
import PackageFormat.FuncInfo
import PackageFormat.VarInfo
import PackageFormat.VarWithPatternInfo
import PackageFormat.PropInfo
import PackageFormat.OverflowPolicy
import PackageFormat.FuncBody
import PackageFormat.FuncParamList
import PackageFormat.ExtendInfo
import PackageFormat.ClassInfo
import PackageFormat.InterfaceInfo
import PackageFormat.StructInfo
import PackageFormat.EnumInfo
import PackageFormat.Generic
import PackageFormat.Constraint
import PackageFormat.AliasInfo
import PackageFormat.SemaTy
import PackageFormat.SemaTyInfo
import PackageFormat.ConstValue
import PackageFormat.Int64Value
import PackageFormat.ArrayValue
import PackageFormat.CompositeValueIndex
import PackageFormat.CompositeValue
import PackageFormat.ArrayTyInfo
import PackageFormat.CompositeTyInfo
import PackageFormat.FuncTyInfo
import PackageFormat.GenericTyInfo
import PackageFormat.MemberValue
import PackageFormat.Pattern
import PackageFormat.PatternKind
import PackageFormat.Position
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.serialization.CjoConstants
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
    /** Encode a Pattern from a declaration-info model without exposing builder ownership. */
    internal fun writePattern(builder: FlatBufferBuilder, pattern: CjoPatternMetadata): Int =
        pattern.write(builder)
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
        validateSchemaProfile(metadata)
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
        val allFileInfoOffset = metadata.fileInfo
            .takeIf(List<CjoFileInfoMetadata>::isNotEmpty)
            ?.map { it.write(builder) }
            ?.toIntArray()
            ?.let { Package.createAllFileInfoVector(builder, it) }
            ?: 0
        val allDependentStdPkgsOffset = metadata.allDependentStdPkgs
            .takeIf(List<String>::isNotEmpty)
            ?.map(builder::createString)
            ?.toIntArray()
            ?.let { Package.createAllDependentStdPkgsVector(builder, it) }
            ?: 0
        validateCompositeValueReferences(metadata)
        val allDeclsOffset = metadata.declarations
            .takeIf(List<CjoPackageDeclaration>::isNotEmpty)
            ?.map { it.write(builder, metadata.fullPackageName, packageNameOffset) }
            ?.toIntArray()
            ?.let { Package.createAllDeclsVector(builder, it) }
            ?: 0
        val allTypesOffset = metadata.types
            .takeIf(List<CjoTypeMetadata>::isNotEmpty)
            ?.map { it.write(builder) }
            ?.toIntArray()
            ?.let { Package.createAllTypesVector(builder, it) }
            ?: 0
        validateExpressionReferences(metadata)
        validateDeclarationAndTypeReferences(metadata)
        val allExprsOffset = metadata.expressions
            .takeIf(List<CjoExpressionMetadata>::isNotEmpty)
            ?.map { it.write(builder) }
            ?.toIntArray()
            ?.let { Package.createAllExprsVector(builder, it) }
            ?: 0
        val allValuesOffset = metadata.values
            .takeIf(List<CjoCompositeValueMetadata>::isNotEmpty)
            ?.map { it.write(builder) }
            ?.toIntArray()
            ?.let { Package.createAllValuesVector(builder, it) }
            ?: 0

        Package.startPackage(builder)
        (metadata.cjoVersion ?: CjoFormatVersion(
            CjoConstants.VERSION_MAJOR.toUByte(),
            CjoConstants.VERSION_MINOR.toUByte(),
            CjoConstants.VERSION_PATCH.toUByte(),
        )).let { version ->
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
        if (allFileInfoOffset != 0) {
            Package.addAllFileInfo(builder, allFileInfoOffset)
        }
        if (allDependentStdPkgsOffset != 0) {
            Package.addAllDependentStdPkgs(builder, allDependentStdPkgsOffset)
        }
        if (allDeclsOffset != 0) {
            Package.addAllDecls(builder, allDeclsOffset)
        }
        if (allTypesOffset != 0) {
            Package.addAllTypes(builder, allTypesOffset)
        }
        if (allExprsOffset != 0) {
            Package.addAllExprs(builder, allExprsOffset)
        }
        if (allValuesOffset != 0) {
            Package.addAllValues(builder, allValuesOffset)
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
        val genericOffset = generic?.write(builder)
        val genericDeclOffset = genericDecl?.let { value ->
            val declOffset = value.decl?.let(builder::createString) ?: 0
            FullId.createFullId(builder, value.pkgId, declOffset, value.index)
        } ?: 0
        val mangledBeforeSemaOffset = mangledBeforeSema?.let(builder::createString) ?: 0
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
        val dependencyOffsets = dependencies
            .takeIf(List<CjoAnnotationTargetMetadata>::isNotEmpty)
            ?.map { dependency ->
                val declOffset = dependency.decl?.let(builder::createString) ?: 0
                FullId.createFullId(builder, dependency.pkgId, declOffset, dependency.index)
            }
            ?.toIntArray()
            ?.let { Decl.createDependenciesVector(builder, it) }
            ?: 0
        val infoOffset = info?.write(builder)

        Decl.startDecl(builder)
        Decl.addKind(builder, kind)
        Decl.addIsTopLevel(builder, isTopLevel)
        Decl.addFullPkgName(builder, packageNameOffset)
        Decl.addIdentifier(builder, identifierOffset)
        if (genericDeclOffset != 0) {
            Decl.addGenericDecl(builder, genericDeclOffset)
        }
        begin?.let { Decl.addBegin(builder, it.write(builder)) }
        end?.let { Decl.addEnd(builder, it.write(builder)) }
        identifierPosition?.let { Decl.addIdentifierPos(builder, it.write(builder)) }
        if (type != 0u) {
            Decl.addType(builder, type)
        }
        if (exportIdOffset != 0) {
            Decl.addExportId(builder, exportIdOffset)
        }
        if (mangledNameOffset != 0) {
            Decl.addMangledName(builder, mangledNameOffset)
        }
        if (mangledBeforeSemaOffset != 0) {
            Decl.addMangledBeforeSema(builder, mangledBeforeSemaOffset)
        }
        declarationHash?.let { Decl.addHash(builder, it.write(builder)) }
        if (genericOffset != null) {
            Decl.addGeneric(builder, genericOffset)
        }
        if (attributeOffsets != 0) {
            Decl.addAttributes(builder, attributeOffsets)
        }
        if (annotationOffsets != 0) {
            Decl.addAnnotations(builder, annotationOffsets)
        }
        if (dependencyOffsets != 0) {
            Decl.addDependencies(builder, dependencyOffsets)
        }
        if (infoOffset != null) {
            Decl.addInfoType(builder, infoOffset.first)
            Decl.addInfo(builder, infoOffset.second)
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

    /**
     * 验证 ModuleFormat 的 1-based expression reference 约束。
     *
     * `AnnoArg.expr` 不是常量值，也不是 `allExprs` 的 0-based 下标；官方
     * reader 将 0/UINT_MAX 视为无引用，其余值减一后索引 `Package.allExprs`。
     * writer 在 FlatBuffers 构造前拒绝悬空引用，避免生成 reader 静默丢参的
     * “看似成功” CJO。
     */
    private fun validateExpressionReferences(metadata: CjoPackageMetadata) {
        val expressionCount = metadata.expressions.size.toUInt()
        fun validate(rawIndex: UInt, owner: String) {
            if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) {
                // Both values are official “no expression” sentinels.  They
                // are valid only as an explicit absent reference and do not
                // need an allExprs entry.
                return
            }
            require(rawIndex <= expressionCount) {
                "$owner references expression $rawIndex, but allExprs has $expressionCount entries"
            }
        }

        metadata.declarations.forEach { declaration ->
            declaration.annotations.forEach { annotation ->
                annotation.arguments.forEachIndexed { index, argument ->
                    validate(argument.expr, "${declaration.identifier}.${annotation.identifier} argument #$index")
                }
            }
        }
        metadata.expressions.forEachIndexed { index, expression ->
            expression.operands.forEachIndexed { operandIndex, operand ->
                validate(operand, "allExprs[$index].operands[$operandIndex]")
            }
        }
    }

    /**
     * Keep the repository's extended ModuleFormat explicit at the writer boundary.
     * The official v1.0.0 branch has only four AnnoKind values and no Anno.target;
     * the v1.1.3 branch has the additional official platform kinds and target
     * fields.  Silently emitting repository extensions as official bytes would
     * make ABI claims and downstream compatibility unverifiable.
     */
    private fun validateSchemaProfile(metadata: CjoPackageMetadata) {
        if (metadata.schemaProfile == CjoSchemaProfile.REPOSITORY_EXTENDED) return
        if (metadata.schemaProfile == CjoSchemaProfile.OFFICIAL_V1_0_0) {
            require(metadata.fileInfo.isEmpty()) {
                "Official v1.0.0 CJO cannot encode Package.allFileInfo"
            }
            require(metadata.allDependentStdPkgs.isEmpty()) {
                "Official v1.0.0 CJO cannot encode Package.allDependentStdPkgs"
            }
            require(metadata.declarations.none { declaration ->
                declaration.annotations.any { annotation -> annotation.target != null }
            }) { "Official v1.0.0 CJO cannot encode Anno.target" }
            require(metadata.declarations.none { declaration -> declaration.dependencies.isNotEmpty() }) {
                "Official v1.0.0 CJO cannot encode declaration dependencies"
            }
        }
        val officialKinds = when (metadata.schemaProfile) {
            CjoSchemaProfile.OFFICIAL_V1_0_0 -> setOf(
                AnnoKind.Deprecated, AnnoKind.TestRegistration, AnnoKind.Frozen, AnnoKind.Custom,
            )
            CjoSchemaProfile.OFFICIAL_V1_1_3 -> setOf(
                AnnoKind.Deprecated, AnnoKind.TestRegistration, AnnoKind.Frozen, AnnoKind.Custom,
                AnnoKind.JavaMirror, AnnoKind.JavaImpl, AnnoKind.ObjCMirror, AnnoKind.ObjCImpl,
                AnnoKind.ForeignName, AnnoKind.JavaHasDefault, AnnoKind.Annotation,
            )
            CjoSchemaProfile.REPOSITORY_EXTENDED -> emptySet()
        }
        require(metadata.annotationsUseOnlyOfficialKinds(officialKinds)) {
            "Official CJO profile cannot encode annotation kind outside its language schema"
        }
    }

    private fun CjoPackageMetadata.annotationsUseOnlyOfficialKinds(officialKinds: Set<UShort>): Boolean =
        declarations.flatMap { it.annotations }.all { it.kind in officialKinds }

    /** 验证声明和类型引用使用 ModuleFormat 的 1-based 规则。 */
    private fun validateDeclarationAndTypeReferences(metadata: CjoPackageMetadata) {
        fun validate(raw: UInt, count: Int, owner: String) {
            if (raw == 0u || raw == UInt.MAX_VALUE) return
            require(raw <= count.toUInt()) { "$owner references $raw, but the pool has $count entries" }
        }
        metadata.declarations.forEachIndexed { index, declaration ->
            validate(declaration.type, metadata.types.size, "declarations[$index].type")
            declaration.generic?.let { generic ->
                generic.typeParameters.forEachIndexed { parameterIndex, parameter ->
                    validate(parameter, metadata.declarations.size,
                        "declarations[$index].generic.typeParameters[$parameterIndex]")
                }
                generic.constraints.forEachIndexed { constraintIndex, constraint ->
                    validate(constraint.type, metadata.types.size,
                        "declarations[$index].generic.constraints[$constraintIndex].type")
                    constraint.upperBounds.forEachIndexed { boundIndex, bound ->
                        validate(bound, metadata.types.size,
                            "declarations[$index].generic.constraints[$constraintIndex].uppers[$boundIndex]")
                    }
                }
            }
            when (val info = declaration.info) {
                null -> Unit
                is CjoFunctionInfo -> {
                    info.body.parameterLists.forEachIndexed { listIndex, list ->
                        list.forEachIndexed { parameterIndex, parameter ->
                            validate(parameter, metadata.declarations.size,
                                "declarations[$index].info.paramLists[$listIndex].params[$parameterIndex]")
                        }
                    }
                    info.body.desugaredParameterLists.forEachIndexed { listIndex, list ->
                        list.forEachIndexed { desugarIndex, desugar ->
                            validate(desugar, metadata.declarations.size,
                                "declarations[$index].info.paramLists[$listIndex].desugars[$desugarIndex]")
                        }
                    }
                    validate(info.body.returnType, metadata.types.size, "declarations[$index].info.funcBody.retType")
                }
                is CjoVariableInfo ->
                    validate(info.initializer, metadata.expressions.size,
                        "declarations[$index].info.varInfo.initializer")
                is CjoVarWithPatternInfo -> {
                    validate(info.initializer, metadata.expressions.size,
                        "declarations[$index].info.varWithPatternInfo.initializer")
                    validatePatternReferences(
                        info.irrefutablePattern,
                        metadata,
                        "declarations[$index].info.varWithPatternInfo.pattern",
                    )
                }
                is CjoPropertyInfo -> {
                    info.getters.forEachIndexed { getterIndex, getter ->
                        validate(getter, metadata.declarations.size,
                            "declarations[$index].info.propInfo.getters[$getterIndex]")
                    }
                    info.setters.forEachIndexed { setterIndex, setter ->
                        validate(setter, metadata.declarations.size,
                            "declarations[$index].info.propInfo.setters[$setterIndex]")
                    }
                }
                is CjoExtendInfo -> {
                    info.inheritedTypes.forEachIndexed { inheritedIndex, inheritedType ->
                        validate(inheritedType, metadata.types.size,
                            "declarations[$index].info.extendInfo.inheritedTypes[$inheritedIndex]")
                    }
                    info.body.forEachIndexed { bodyIndex, bodyDeclaration ->
                        validate(bodyDeclaration, metadata.declarations.size,
                            "declarations[$index].info.extendInfo.body[$bodyIndex]")
                    }
                }
                is CjoClassInfo -> validateInheritableInfo(info, metadata, index, "classInfo")
                is CjoInterfaceInfo -> validateInheritableInfo(info, metadata, index, "interfaceInfo")
                is CjoStructInfo -> validateInheritableInfo(info, metadata, index, "structInfo")
                is CjoEnumInfo -> validateInheritableInfo(info, metadata, index, "enumInfo")
                is CjoAliasInfo -> validate(info.aliasedType, metadata.types.size,
                    "declarations[$index].info.aliasInfo.aliasedTy")
            }
        }
        metadata.types.forEachIndexed { index, type ->
            type.typeArguments.forEachIndexed { argumentIndex, argument ->
                validate(argument, metadata.types.size, "types[$index].typeArgs[$argumentIndex]")
            }
            val composite = type.semanticInfo as? CjoCompositeTypeInfoMetadata
            if (composite != null) {
                when {
                    composite.packageId == -2 -> validate(
                        composite.declarationIndex,
                        metadata.declarations.size,
                        "types[$index].compositeDecl",
                    )
                    composite.packageId >= 0 -> {
                        require(composite.packageId < metadata.imports.size) {
                            "types[$index] references import ${composite.packageId}, but imports has ${metadata.imports.size} entries"
                        }
                        require(!composite.declarationKey.isNullOrBlank()) {
                            "types[$index] imported composite reference is missing its declaration key"
                        }
                    }
                    else -> error("types[$index] uses unsupported composite package id ${composite.packageId}")
                }
            }
        }
        metadata.values.forEachIndexed { valueIndex, value ->
            value.fields.forEachIndexed { fieldIndex, field ->
                validate(field.type, metadata.types.size, "allValues[$valueIndex].fields[$fieldIndex].type")
            }
        }
    }

    private fun validateInheritableInfo(
        info: CjoInheritableInfo,
        metadata: CjoPackageMetadata,
        declarationIndex: Int,
        infoName: String,
    ) {
        info.inheritedTypes.forEachIndexed { inheritedIndex, inheritedType ->
            require(inheritedType in 1u..metadata.types.size.toUInt()) {
                "declarations[$declarationIndex].info.$infoName.inheritedTypes[$inheritedIndex] " +
                    "references $inheritedType, but allTypes has ${metadata.types.size} entries"
            }
        }
        info.body.forEachIndexed { bodyIndex, bodyDeclaration ->
            require(bodyDeclaration in 1u..metadata.declarations.size.toUInt()) {
                "declarations[$declarationIndex].info.$infoName.body[$bodyIndex] " +
                    "references $bodyDeclaration, but allDecls has ${metadata.declarations.size} entries"
            }
        }
    }

    private fun validatePatternReferences(
        pattern: CjoPatternMetadata,
        metadata: CjoPackageMetadata,
        owner: String,
    ) {
        fun validateType(index: Int, type: UInt) {
            require(type in 1u..metadata.types.size.toUInt()) {
                "$owner.types[$index] references $type, but allTypes has ${metadata.types.size} entries"
            }
        }

        fun validateExpression(index: Int, expr: UInt) {
            require(expr in 1u..metadata.expressions.size.toUInt()) {
                "$owner.exprs[$index] references $expr, but allExprs has ${metadata.expressions.size} entries"
            }
        }

        fun validateDeclaration(index: Int, declaration: UInt) {
            require(declaration in 1u..metadata.declarations.size.toUInt()) {
                "$owner.exprs[$index] references declaration $declaration, but allDecls has ${metadata.declarations.size} entries"
            }
        }

        fun requireSize(field: String, actual: Int, expected: Int) {
            require(actual == expected) {
                "$owner.$field must contain $expected entries for pattern kind ${pattern.kind}, got $actual"
            }
        }

        // ModuleFormat uses the same vector field for different reference
        // domains.  In particular VarPattern.exprs contains a declaration
        // index, while ConstPattern/EnumPattern.exprs contains allExprs
        // indices.  Validate by the official PatternKind instead of applying
        // one expression-pool rule to every pattern.
        when (pattern.kind) {
            PatternKind.VarPattern -> {
                requireSize("types", pattern.types.size, 1)
                requireSize("exprs", pattern.exprs.size, 1)
                require(pattern.patterns.isEmpty() && pattern.values.isEmpty()) {
                    "$owner VarPattern may only contain one type and one declaration reference"
                }
                pattern.types.forEachIndexed(::validateType)
                pattern.exprs.forEachIndexed(::validateDeclaration)
            }

            PatternKind.ConstPattern -> {
                require(pattern.exprs.size in 1..2) {
                    "$owner ConstPattern must contain one or two expression references"
                }
                requireSize("types", pattern.types.size, 1)
                require(pattern.patterns.isEmpty() && pattern.values.isEmpty()) {
                    "$owner ConstPattern may not contain child patterns or values"
                }
                pattern.types.forEachIndexed(::validateType)
                pattern.exprs.forEachIndexed(::validateExpression)
            }

            PatternKind.EnumPattern -> {
                requireSize("types", pattern.types.size, 1)
                requireSize("exprs", pattern.exprs.size, 1)
                require(pattern.values.isEmpty()) {
                    "$owner EnumPattern may not contain constant values"
                }
                pattern.types.forEachIndexed(::validateType)
                pattern.exprs.forEachIndexed(::validateExpression)
                pattern.patterns.forEachIndexed { index, child ->
                    validatePatternReferences(child, metadata, "$owner.patterns[$index]")
                }
            }

            PatternKind.WildcardPattern -> {
                requireSize("types", pattern.types.size, 1)
                require(pattern.patterns.isEmpty() && pattern.exprs.isEmpty() && pattern.values.isEmpty()) {
                    "$owner WildcardPattern may only contain one type"
                }
                pattern.types.forEachIndexed(::validateType)
            }

            PatternKind.TuplePattern -> {
                requireSize("types", pattern.types.size, 1)
                require(pattern.exprs.isEmpty() && pattern.values.isEmpty()) {
                    "$owner TuplePattern may only contain child patterns and one type"
                }
                pattern.types.forEachIndexed(::validateType)
                pattern.patterns.forEachIndexed { index, child ->
                    validatePatternReferences(child, metadata, "$owner.patterns[$index]")
                }
            }

            PatternKind.TypePattern -> {
                requireSize("types", pattern.types.size, 1)
                requireSize("patterns", pattern.patterns.size, 1)
                require(pattern.exprs.isEmpty() && pattern.values.isEmpty()) {
                    "$owner TypePattern may only contain one type and one child pattern"
                }
                pattern.types.forEachIndexed(::validateType)
                validatePatternReferences(pattern.patterns.single(), metadata, "$owner.patterns[0]")
            }

            PatternKind.ExceptTypePattern -> {
                require(pattern.types.isNotEmpty()) {
                    "$owner ExceptTypePattern must contain at least one type"
                }
                requireSize("patterns", pattern.patterns.size, 1)
                require(pattern.exprs.isEmpty() && pattern.values.isEmpty()) {
                    "$owner ExceptTypePattern may only contain types and one child pattern"
                }
                pattern.types.forEachIndexed(::validateType)
                validatePatternReferences(pattern.patterns.single(), metadata, "$owner.patterns[0]")
            }

            else -> error("Unsupported CJO pattern kind ${pattern.kind} at $owner")
        }
    }

    /** CompositeValueIndex is a direct 0-based index into the official allValues vector. */
    private fun validateCompositeValueReferences(metadata: CjoPackageMetadata) {
        fun validate(value: CjoConstValueInfo, owner: String) {
            when (value) {
                is CjoCompositeConstValue -> require(value.index < metadata.values.size.toUInt()) {
                    "$owner references allValues[${value.index}], but allValues has ${metadata.values.size} entries"
                }
                is CjoArrayConstValue -> value.elements.forEachIndexed { index, nested ->
                    validate(nested, "$owner.array[$index]")
                }
                else -> Unit
            }
        }
        metadata.declarations.forEach { declaration ->
            when (val info = declaration.info) {
                is CjoVariableInfo -> info.constValue?.let { validate(it, declaration.identifier) }
                is CjoVarWithPatternInfo -> validatePatternValues(
                    info.irrefutablePattern,
                    ::validate,
                    declaration.identifier,
                )
                else -> Unit
            }
        }
        metadata.values.forEachIndexed { valueIndex, composite ->
            composite.fields.forEachIndexed { fieldIndex, field ->
                validate(field.value, "allValues[$valueIndex].fields[$fieldIndex]")
            }
        }
    }

    private fun validatePatternValues(
        pattern: CjoPatternMetadata,
        validate: (CjoConstValueInfo, String) -> Unit,
        owner: String,
    ) {
        pattern.values.forEachIndexed { index, value -> validate(value, "$owner.pattern.values[$index]") }
        pattern.patterns.forEachIndexed { index, child ->
            validatePatternValues(child, validate, "$owner.patterns[$index]")
        }
    }

    /** 写出一个 reader 当前支持的语义表达式节点。 */
    private fun CjoExpressionMetadata.write(builder: FlatBufferBuilder): Int {
        val operandsOffset = operands
            .takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()
            ?.let { Expr.createOperandsVector(builder, it) }
            ?: 0
        val (infoType, infoOffset) = when {
            literal != null -> {
                val valueOffset = literal.value?.let(builder::createString) ?: 0
                ExprInfo.LitConstInfo to LitConstInfo.createLitConstInfo(
                    builder,
                    valueOffset,
                    literal.constKind,
                    literal.stringKind,
                )
            }
            reference != null -> {
                val referenceOffset = builder.createString(reference.reference)
                val targetOffset = reference.target?.let { target ->
                    val declOffset = target.decl?.let(builder::createString) ?: 0
                    FullId.createFullId(builder, target.pkgId, declOffset, target.index)
                } ?: 0
                val instantiationOffset = reference.instantiatedTypes
                    .takeIf(List<UInt>::isNotEmpty)
                    ?.toUIntArray()
                    ?.let { ReferenceInfo.createInstTysVector(builder, it) }
                    ?: 0
                ExprInfo.ReferenceInfo to ReferenceInfo.createReferenceInfo(
                    builder,
                    referenceOffset,
                    targetOffset,
                    instantiationOffset,
                    reference.matchedParentType,
                )
            }
            else -> ExprInfo.NONE to 0
        }
        Expr.startExpr(builder)
        Expr.addKind(builder, kind)
        if (operandsOffset != 0) Expr.addOperands(builder, operandsOffset)
        Expr.addType(builder, type)
        if (infoOffset != 0) {
            Expr.addInfoType(builder, infoType)
            Expr.addInfo(builder, infoOffset)
        }
        return Expr.endExpr(builder)
    }

    /** Write one official `Package.allValues` composite value entry. */
    private fun CjoPatternMetadata.write(builder: FlatBufferBuilder): Int {
        val patternOffsets = patterns.map { it.write(builder) }.toIntArray()
        val patternsOffset = patternOffsets.takeIf(IntArray::isNotEmpty)
            ?.let { Pattern.createPatternsVector(builder, it) } ?: 0
        val typesOffset = types.takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()?.let { Pattern.createTypesVector(builder, it) } ?: 0
        val exprsOffset = exprs.takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()?.let { Pattern.createExprsVector(builder, it) } ?: 0
        val encodedValues = values.map { it.write(builder) }
        val valuesTypesOffset = encodedValues.map { it.first }.toUByteArray()
            .takeIf(UByteArray::isNotEmpty)
            ?.let { Pattern.createValuesTypeVector(builder, it) } ?: 0
        val valuesOffset = encodedValues.map { it.second }.toIntArray()
            .takeIf(IntArray::isNotEmpty)
            ?.let { Pattern.createValuesVector(builder, it) } ?: 0
        val beginOffset = begin?.write(builder) ?: 0
        val endOffset = end?.write(builder) ?: 0

        Pattern.startPattern(builder)
        if (beginOffset != 0) Pattern.addBegin(builder, beginOffset)
        if (endOffset != 0) Pattern.addEnd(builder, endOffset)
        if (patternsOffset != 0) Pattern.addPatterns(builder, patternsOffset)
        if (typesOffset != 0) Pattern.addTypes(builder, typesOffset)
        if (exprsOffset != 0) Pattern.addExprs(builder, exprsOffset)
        if (valuesTypesOffset != 0) Pattern.addValuesType(builder, valuesTypesOffset)
        if (valuesOffset != 0) Pattern.addValues(builder, valuesOffset)
        Pattern.addKind(builder, kind)
        Pattern.addMatchBeforeRuntime(builder, matchBeforeRuntime)
        Pattern.addNeedRuntimeTypeCheck(builder, needRuntimeTypeCheck)
        return Pattern.endPattern(builder)
    }

    /** Write one official `Package.allValues` composite value entry. */
    private fun CjoCompositeValueMetadata.write(builder: FlatBufferBuilder): Int {
        val fieldOffsets = fields.map { field ->
            val fieldNameOffset = builder.createString(field.name)
            val encoded = field.value.write(builder)
            MemberValue.createMemberValue(
                builder,
                fieldNameOffset,
                field.type,
                encoded.first,
                encoded.second,
            )
        }.toIntArray()
        val fieldsOffset = fieldOffsets
            .takeIf(IntArray::isNotEmpty)
            ?.let { CompositeValue.createFieldsVector(builder, it) }
            ?: 0
        return CompositeValue.createCompositeValue(builder, type, fieldsOffset)
    }

    /** 写出 ModuleFormat 的 SemaTy 基础节点。 */
    private fun CjoTypeMetadata.write(builder: FlatBufferBuilder): Int {
        val argumentsOffset = typeArguments
            .takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()
            ?.let { SemaTy.createTypeArgsVector(builder, it) }
            ?: 0
        val semanticInfo = semanticInfo?.write(builder)
        return SemaTy.createSemaTy(
            builder,
            kind,
            argumentsOffset,
            semanticInfo?.first ?: infoType,
            semanticInfo?.second ?: infoOffset,
        )
    }
}

/** Official ModuleFormat.Position input model used by pattern metadata. */
data class CjoPositionMetadata(
    val file: UInt = 0u,
    val pkgId: UInt = 0u,
    val line: Int = 0,
    val column: Int = 0,
    val ignore: Boolean = false,
) {
    fun write(builder: FlatBufferBuilder): Int =
        Position.createPosition(builder, file, pkgId, line, column, ignore)
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
    /** 结构化 CJO 格式版本；缺省时写出官方 ModuleFormat 0.1.0。 */
    val cjoVersion: CjoFormatVersion? = CjoFormatVersion(
        CjoConstants.VERSION_MAJOR.toUByte(),
        CjoConstants.VERSION_MINOR.toUByte(),
        CjoConstants.VERSION_PATCH.toUByte(),
    ),
    /** Wire profile; official v1.0.0 is the safe default, extensions are opt-in. */
    val schemaProfile: CjoSchemaProfile = CjoSchemaProfile.OFFICIAL_V1_0_0,
    /** 包依赖信息的原始字符串。 */
    val packageDependencyInfo: String? = null,
    /** 包级导入文本列表。 */
    val imports: List<String> = emptyList(),
    /** package 包含的源文件名列表。 */
    val allFiles: List<String> = emptyList(),
    /** 按源文件组织的结构化 import 列表。 */
    val fileImports: List<CjoPackageFileImports> = emptyList(),
    /** Official `Package.allFileInfo` entries used by CJMP and source mapping. */
    val fileInfo: List<CjoFileInfoMetadata> = emptyList(),
    /** Official `Package.allDependentStdPkgs` package names. */
    val allDependentStdPkgs: List<String> = emptyList(),
    /** 需要写入 `allDecls` 的声明索引项。 */
    val declarations: List<CjoPackageDeclaration> = emptyList(),
    /** `Package.allTypes` 中的 1-based 类型池。 */
    val types: List<CjoTypeMetadata> = emptyList(),
    /** `Package.allExprs` 表达式池；索引由声明/注解中的 1-based 引用使用。 */
    val expressions: List<CjoExpressionMetadata> = emptyList(),
    /** Official `Package.allValues` composite constant pool. */
    val values: List<CjoCompositeValueMetadata> = emptyList(),
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

/** Official ModuleFormat.DeclHash input model. */
data class CjoDeclHashMetadata(
    val instVar: ULong,
    val virt: ULong,
    val sig: ULong,
    val srcUse: ULong,
    val bodyHash: ULong,
) {
    fun write(builder: FlatBufferBuilder): Int =
        PackageFormat.DeclHash.createDeclHash(builder, instVar, virt, sig, srcUse, bodyHash)
}

/** Official v1.0.0 wire subset versus the verified official v1.1.3 ModuleFormat profile. */
enum class CjoSchemaProfile {
    OFFICIAL_V1_0_0,
    OFFICIAL_V1_1_3,
    REPOSITORY_EXTENDED,
}

/** Official ModuleFormat.FileInfo input model. */
data class CjoFileInfoMetadata(
    val fileId: UInt,
    val begin: CjoPositionMetadata,
    val end: CjoPositionMetadata,
) {
    fun write(builder: FlatBufferBuilder): Int {
        FileInfo.startFileInfo(builder)
        FileInfo.addFileID(builder, fileId)
        FileInfo.addBegin(builder, begin.write(builder))
        FileInfo.addEnd(builder, end.write(builder))
        return FileInfo.endFileInfo(builder)
    }
}

/** One official `CompositeValue` entry and its recursively encoded members. */
data class CjoCompositeValueMetadata(
    val type: UInt,
    val fields: List<CjoCompositeValueFieldMetadata> = emptyList(),
)

/** One official `MemberValue` entry in a composite constant. */
data class CjoCompositeValueFieldMetadata(
    val name: String,
    val type: UInt,
    val value: CjoConstValueInfo,
) {
    init {
        require(name.isNotBlank()) { "CJO composite value field name must not be blank." }
    }
}

/** Official ModuleFormat.Pattern input model for VarWithPatternInfo. */
data class CjoPatternMetadata(
    val kind: Byte = PackageFormat.PatternKind.InvalidPattern,
    val begin: CjoPositionMetadata? = null,
    val end: CjoPositionMetadata? = null,
    val patterns: List<CjoPatternMetadata> = emptyList(),
    val types: List<UInt> = emptyList(),
    val exprs: List<UInt> = emptyList(),
    val values: List<CjoConstValueInfo> = emptyList(),
    val matchBeforeRuntime: Boolean = false,
    val needRuntimeTypeCheck: Boolean = false,
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
    /** Official Decl.genericDecl FullId. */
    val genericDecl: CjoAnnotationTargetMetadata? = null,
    /** Official declaration source begin position. */
    val begin: CjoPositionMetadata? = null,
    /** Official declaration source end position. */
    val end: CjoPositionMetadata? = null,
    /** Official declaration identifier position. */
    val identifierPosition: CjoPositionMetadata? = null,
    /** 跨包引用优先使用的 export id。 */
    val exportId: String? = null,
    /** 可选 mangled name。 */
    val mangledName: String? = null,
    /** Official Decl.mangledBeforeSema (the AST writer raw-mangle field). */
    val mangledBeforeSema: String? = null,
    /** Official incremental declaration hash. */
    val declarationHash: CjoDeclHashMetadata? = null,
    /** `Decl.type` 的 1-based `Package.allTypes` 引用。 */
    val type: UInt = 0u,
    /** 官方 Decl.attributes 位图；顺序和位宽按 ModuleFormat 保留。 */
    val attributes: List<ULong> = emptyList(),
    /** 已解析的声明注解 metadata；writer 不根据源码短名推断 kind。 */
    val annotations: List<CjoAnnotationMetadata> = emptyList(),
    /** Official Decl.generic table. */
    val generic: CjoGenericMetadata? = null,
    /** ModuleFormat 声明 info union；由 semantic producer 显式提供。 */
    val info: CjoDeclarationInfo? = null,
    /** Repository-only common/specific dependency references. */
    val dependencies: List<CjoAnnotationTargetMetadata> = emptyList(),
) {
    init {
        require(identifier.isNotBlank() || kind == DeclKind.ExtendDecl) {
            "CJO declaration identifier must not be blank except for official ExtendDecl."
        }
        require(
            when (info) {
                null -> true
                is CjoFunctionInfo -> kind == DeclKind.FuncDecl
                is CjoVariableInfo -> kind == DeclKind.VarDecl
                is CjoPropertyInfo -> kind == DeclKind.PropDecl
                is CjoVarWithPatternInfo -> kind == DeclKind.VarWithPatternDecl
                is CjoExtendInfo -> kind == DeclKind.ExtendDecl
                is CjoClassInfo -> kind == DeclKind.ClassDecl
                is CjoInterfaceInfo -> kind == DeclKind.InterfaceDecl
                is CjoStructInfo -> kind == DeclKind.StructDecl
                is CjoEnumInfo -> kind == DeclKind.EnumDecl
                is CjoAliasInfo -> kind == DeclKind.TypeAliasDecl
            },
        ) { "CJO declaration '$identifier' has an info table incompatible with kind $kind." }
    }
}

/** ModuleFormat.Decl.info 的平台中立输入契约。 */
sealed interface CjoDeclarationInfo {
    /** 返回官方 union 类型与 table offset。 */
    fun write(builder: FlatBufferBuilder): Pair<UByte, Int>
}

/** ExtendInfo 的官方字段；引用均为当前 Package 的 1-based 索引。 */
data class CjoExtendInfo(
    val inheritedTypes: List<UInt> = emptyList(),
    val body: List<UInt> = emptyList(),
) : CjoDeclarationInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val inheritedTypesOffset = inheritedTypes.takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()?.let { ExtendInfo.createInheritedTypesVector(builder, it) } ?: 0
        val bodyOffset = body.takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()?.let { ExtendInfo.createBodyVector(builder, it) } ?: 0
        return DeclInfo.ExtendInfo to ExtendInfo.createExtendInfo(builder, inheritedTypesOffset, bodyOffset)
    }
}

/** AliasInfo is the official declaration-info union for a type alias. */
data class CjoAliasInfo(val aliasedType: UInt) : CjoDeclarationInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        DeclInfo.AliasInfo to AliasInfo.createAliasInfo(builder, aliasedType)
}

/** Shared inheritance/body metadata for class-like declaration tables. */
sealed interface CjoInheritableInfo {
    val inheritedTypes: List<UInt>
    val body: List<UInt>
}

data class CjoGenericMetadata(
    val typeParameters: List<UInt>,
    val constraints: List<CjoConstraintMetadata> = emptyList(),
) {
    fun write(builder: FlatBufferBuilder): Int {
        val parameters = Generic.createTypeParametersVector(builder, typeParameters.toUIntArray())
        val constraintTables = constraints.map { it.write(builder) }.toIntArray()
        val constraintVector = Generic.createConstraintsVector(builder, constraintTables)
        return Generic.createGeneric(builder, parameters, constraintVector)
    }
}

data class CjoConstraintMetadata(
    val type: UInt,
    val upperBounds: List<UInt> = emptyList(),
    val isImplicitlyIntroduced: Boolean = false,
) {
    fun write(builder: FlatBufferBuilder): Int {
        val uppers = Constraint.createUppersVector(builder, upperBounds.toUIntArray())
        Constraint.startConstraint(builder)
        Constraint.addType(builder, type)
        Constraint.addUppers(builder, uppers)
        Constraint.addIsImplicitlyIntroduced(builder, isImplicitlyIntroduced)
        return Constraint.endConstraint(builder)
    }
}

data class CjoClassInfo(
    override val inheritedTypes: List<UInt> = emptyList(),
    override val body: List<UInt> = emptyList(),
    val isAnnotation: Boolean = false,
    val annotationTargets: UByte = 0u,
    val runtimeVisible: Boolean = false,
    val annotationTargets2: UByte = 0u,
) : CjoDeclarationInfo, CjoInheritableInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val inherited = inheritedTypes.toUIntArray().let { ClassInfo.createInheritedTypesVector(builder, it) }
        val members = body.toUIntArray().let { ClassInfo.createBodyVector(builder, it) }
        return DeclInfo.ClassInfo to ClassInfo.createClassInfo(
            builder, inherited, members, 0, isAnnotation, annotationTargets, runtimeVisible, annotationTargets2,
        )
    }
}

data class CjoInterfaceInfo(
    override val inheritedTypes: List<UInt> = emptyList(),
    override val body: List<UInt> = emptyList(),
) : CjoDeclarationInfo, CjoInheritableInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val inherited = inheritedTypes.toUIntArray().let { InterfaceInfo.createInheritedTypesVector(builder, it) }
        val members = body.toUIntArray().let { InterfaceInfo.createBodyVector(builder, it) }
        return DeclInfo.InterfaceInfo to InterfaceInfo.createInterfaceInfo(builder, inherited, members)
    }
}

data class CjoStructInfo(
    override val inheritedTypes: List<UInt> = emptyList(),
    override val body: List<UInt> = emptyList(),
) : CjoDeclarationInfo, CjoInheritableInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val inherited = inheritedTypes.toUIntArray().let { StructInfo.createInheritedTypesVector(builder, it) }
        val members = body.toUIntArray().let { StructInfo.createBodyVector(builder, it) }
        return DeclInfo.StructInfo to StructInfo.createStructInfo(builder, inherited, members, 0)
    }
}

data class CjoEnumInfo(
    override val inheritedTypes: List<UInt> = emptyList(),
    override val body: List<UInt> = emptyList(),
    val hasArguments: Boolean = false,
    val nonExhaustive: Boolean = false,
) : CjoDeclarationInfo, CjoInheritableInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val inherited = inheritedTypes.toUIntArray().let { EnumInfo.createInheritedTypesVector(builder, it) }
        val members = body.toUIntArray().let { EnumInfo.createBodyVector(builder, it) }
        EnumInfo.startEnumInfo(builder)
        EnumInfo.addInheritedTypes(builder, inherited)
        EnumInfo.addBody(builder, members)
        EnumInfo.addHasArguments(builder, hasArguments)
        EnumInfo.addNonExhaustive(builder, nonExhaustive)
        return DeclInfo.EnumInfo to EnumInfo.endEnumInfo(builder)
    }
}

/** FuncInfo 的官方字段；函数体和 AutoDiff body 不在本 writer 范围内。 */
data class CjoFunctionInfo(
    val overflowStrategy: CangjieOverflowStrategy? = null,
    val operation: UByte = 0u,
    val body: CjoFunctionBodyInfo = CjoFunctionBodyInfo(),
    val isConst: Boolean = false,
    val isInline: Boolean = false,
    val isFastNative: Boolean = false,
) : CjoDeclarationInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        DeclInfo.FuncInfo to FuncInfo.createFuncInfo(
            builder,
            body.write(builder),
            overflowStrategy.toCjoOverflowPolicy(),
            operation,
            0,
            isConst,
            isInline,
            isFastNative,
        )
}

/** Function body metadata required even for bodyless library declarations. */
data class CjoFunctionBodyInfo(
    val parameterLists: List<List<UInt>> = emptyList(),
    /** Per-parameter-list desugared declaration references from FuncParamList.desugars. */
    val desugaredParameterLists: List<List<UInt>> = emptyList(),
    val returnType: UInt = 0u,
    val body: UInt = 0u,
    val always: Boolean = false,
    val captureKind: UByte = 0u,
) {
    fun write(builder: FlatBufferBuilder): Int {
        require(desugaredParameterLists.size == parameterLists.size) {
            "FuncBody desugared parameter-list count must match parameter-list count"
        }
        val lists = parameterLists.mapIndexed { listIndex, parameters ->
            val paramsOffset = FuncParamList.createParamsVector(builder, parameters.toUIntArray())
            val desugars = desugaredParameterLists[listIndex]
            require(desugars.size == parameters.size) {
                "FuncParamList[$listIndex] desugared parameter count must match parameter count"
            }
            val desugarsOffset = desugars.takeIf(List<UInt>::isNotEmpty)
                ?.toUIntArray()
                ?.let { FuncParamList.createDesugarsVector(builder, it) }
                ?: 0
            FuncParamList.createFuncParamList(builder, paramsOffset, desugarsOffset)
        }.toIntArray()
        val listsOffset = FuncBody.createParamListsVector(builder, lists)
        return FuncBody.createFuncBody(builder, listsOffset, returnType, body, always, captureKind)
    }
}

/** Basic SemaTy wire model; richer type union info is added by the semantic producer. */
data class CjoTypeMetadata(
    val kind: UShort,
    val typeArguments: List<UInt> = emptyList(),
    val infoType: UByte = SemaTyInfo.NONE,
    val infoOffset: Int = 0,
    /** Semantic type info; encoded by the same FlatBufferBuilder as SemaTy. */
    val semanticInfo: CjoTypeInfoMetadata? = null,
)

/**
 * Builder-owned semantic information for ModuleFormat.SemaTy.
 *
 * The old [CjoTypeMetadata.infoOffset] remains for hand-built wire fixtures;
 * live CFIR producers must use this model and never pass an offset belonging
 * to another FlatBufferBuilder.
 */
sealed interface CjoTypeInfoMetadata {
    fun write(builder: FlatBufferBuilder): Pair<UByte, Int>
}

data class CjoArrayTypeInfoMetadata(val dimsOrSize: Long) : CjoTypeInfoMetadata {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        SemaTyInfo.ArrayTyInfo to ArrayTyInfo.createArrayTyInfo(builder, dimsOrSize)
}

data class CjoFunctionTypeInfoMetadata(
    val returnType: UInt,
    val isC: Boolean,
    val hasVariableLenArg: Boolean,
) : CjoTypeInfoMetadata {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        SemaTyInfo.FuncTyInfo to FuncTyInfo.createFuncTyInfo(builder, returnType, isC, hasVariableLenArg)
}

data class CjoCompositeTypeInfoMetadata(
    val declarationIndex: UInt,
    val packageId: Int = -2,
    val isThisType: Boolean = false,
    /** Imported-package FullId reference key; null means current-package index form. */
    val declarationKey: String? = null,
) : CjoTypeInfoMetadata {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val declarationOffset = declarationKey?.let(builder::createString) ?: 0
        val fullId = FullId.createFullId(builder, packageId, declarationOffset, declarationIndex)
        return SemaTyInfo.CompositeTyInfo to CompositeTyInfo.createCompositeTyInfo(builder, fullId, isThisType)
    }
}

data class CjoGenericTypeInfoMetadata(
    val declarationIndex: UInt,
    val upperBounds: List<UInt> = emptyList(),
    val packageId: Int = -2,
) : CjoTypeInfoMetadata {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val fullId = FullId.createFullId(builder, packageId, 0, declarationIndex)
        val bounds = upperBounds.toUIntArray()
            .takeIf { it.isNotEmpty() }
            ?.let { GenericTyInfo.createUpperBoundsVector(builder, it) }
            ?: 0
        return SemaTyInfo.GenericTyInfo to GenericTyInfo.createGenericTyInfo(builder, fullId, bounds)
    }
}

/** VarInfo 的官方字段；initializer/value union 由已有 expression pool 提供。 */
data class CjoVarWithPatternInfo(
    val isVar: Boolean = false,
    val isConst: Boolean = false,
    val irrefutablePattern: CjoPatternMetadata,
    val initializer: UInt = 0u,
) : CjoDeclarationInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        DeclInfo.VarWithPatternInfo to VarWithPatternInfo.createVarWithPatternInfo(
            builder,
            isVar,
            isConst,
            CjoPackageWriter.writePattern(builder, irrefutablePattern),
            initializer,
        )
}

data class CjoVariableInfo(
    val isVar: Boolean = false,
    val isConst: Boolean = false,
    val isMemberParam: Boolean = false,
    val initializer: UInt = 0u,
    val constValue: CjoConstValueInfo? = null,
) : CjoDeclarationInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val serializedValue = constValue?.write(builder)
        return DeclInfo.VarInfo to VarInfo.createVarInfo(
            builder,
            isVar,
            isConst,
            isMemberParam,
            initializer,
            serializedValue?.first ?: ConstValue.NONE,
            serializedValue?.second ?: 0,
        )
    }
}

/** ConstValue union producer; each variant owns its exact ModuleFormat tag. */
sealed interface CjoConstValueInfo {
    fun write(builder: FlatBufferBuilder): Pair<UByte, Int>
}

data class CjoInt8ConstValue(val value: Byte) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.Int8Value to PackageFormat.Int8Value.createInt8Value(builder, value)
}

data class CjoUInt8ConstValue(val value: UByte) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.UInt8Value to PackageFormat.UInt8Value.createUInt8Value(builder, value)
}

data class CjoInt16ConstValue(val value: Short) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.Int16Value to PackageFormat.Int16Value.createInt16Value(builder, value)
}

data class CjoUInt16ConstValue(val value: UShort) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.UInt16Value to PackageFormat.UInt16Value.createUInt16Value(builder, value)
}

data class CjoInt32ConstValue(val value: Int) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.Int32Value to PackageFormat.Int32Value.createInt32Value(builder, value)
}

data class CjoUInt32ConstValue(val value: UInt) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.UInt32Value to PackageFormat.UInt32Value.createUInt32Value(builder, value)
}

data class CjoInt64ConstValue(val value: Long) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.Int64Value to Int64Value.createInt64Value(builder, value)
}

data class CjoUInt64ConstValue(val value: ULong) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.UInt64Value to PackageFormat.UInt64Value.createUInt64Value(builder, value)
}

data class CjoFloat32ConstValue(val value: Float) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.Float32Value to PackageFormat.Float32Value.createFloat32Value(builder, value)
}

data class CjoFloat64ConstValue(val value: Double) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.Float64Value to PackageFormat.Float64Value.createFloat64Value(builder, value)
}

data class CjoStringConstValue(val value: String) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.StringValue to builder.createString(value)
}

data class CjoArrayConstValue(val elements: List<CjoConstValueInfo>) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val encoded = elements.map { it.write(builder) }
        val valueTypes = ArrayValue.createValTypeVector(builder, encoded.map { it.first }.toUByteArray())
        val values = ArrayValue.createValVector(builder, encoded.map { it.second }.toIntArray())
        return ConstValue.ArrayValue to ArrayValue.createArrayValue(builder, valueTypes, values)
    }
}

data class CjoCompositeConstValue(val index: UInt) : CjoConstValueInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> =
        ConstValue.CompositeValue to CompositeValueIndex.createCompositeValueIndex(builder, index)
}

/** PropInfo 的官方字段；getter/setter 引用使用 expression/declaration 索引。 */
data class CjoPropertyInfo(
    val isConst: Boolean = false,
    val isMutable: Boolean = false,
    val setters: List<UInt> = emptyList(),
    val getters: List<UInt> = emptyList(),
) : CjoDeclarationInfo {
    override fun write(builder: FlatBufferBuilder): Pair<UByte, Int> {
        val settersOffset = setters.takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()?.let { PropInfo.createSettersVector(builder, it) } ?: 0
        val gettersOffset = getters.takeIf(List<UInt>::isNotEmpty)
            ?.toUIntArray()?.let { PropInfo.createGettersVector(builder, it) } ?: 0
        return DeclInfo.PropInfo to PropInfo.createPropInfo(
            builder, isConst, isMutable, settersOffset, gettersOffset,
        )
    }
}

/** 将公共 overflow 策略写入官方 FuncInfo.overflowPolicy。 */
private fun CangjieOverflowStrategy?.toCjoOverflowPolicy(): UByte = when (this) {
    null, CangjieOverflowStrategy.NA -> OverflowPolicy.NA
    CangjieOverflowStrategy.CHECKED -> OverflowPolicy.Checked
    CangjieOverflowStrategy.WRAPPING -> OverflowPolicy.Wrapping
    CangjieOverflowStrategy.THROWING -> OverflowPolicy.Throwing
    CangjieOverflowStrategy.SATURATING -> OverflowPolicy.Saturating
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

/** `Package.allExprs` 中一个可被注解参数引用的表达式节点。 */
data class CjoExpressionMetadata(
    /** `PackageFormat.ExprKind` 的稳定数值。 */
    val kind: UShort = ExprKind.InvalidExpr,
    /** `allTypes` 中的 1-based 类型引用；未知时为 0。 */
    val type: UInt = 0u,
    /** 子表达式的 1-based `allExprs` 引用。 */
    val operands: List<UInt> = emptyList(),
    /** literal expression 的结构化值。 */
    val literal: CjoLiteralExpressionMetadata? = null,
    /** reference expression 的结构化引用。 */
    val reference: CjoReferenceExpressionMetadata? = null,
) {
    init {
        require(literal == null || reference == null) { "CJO expression cannot be both literal and reference" }
        require(kind != ExprKind.LitConstExpr || literal != null) {
            "LitConstExpr must carry LitConstInfo"
        }
        require(kind != ExprKind.RefExpr || reference != null) {
            "RefExpr must carry ReferenceInfo"
        }
    }
}

/** `LitConstInfo` 的公共输入模型。 */
data class CjoLiteralExpressionMetadata(
    val value: String? = null,
    val constKind: UByte,
    val stringKind: UByte = PackageFormat.StringKind.Normal,
)

/** `ReferenceInfo` 的公共输入模型。 */
data class CjoReferenceExpressionMetadata(
    val reference: String,
    val target: CjoExpressionTargetMetadata? = null,
    val instantiatedTypes: List<UInt> = emptyList(),
    val matchedParentType: UInt = 0u,
)

/** expression reference target 的 FullId。 */
data class CjoExpressionTargetMetadata(
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
