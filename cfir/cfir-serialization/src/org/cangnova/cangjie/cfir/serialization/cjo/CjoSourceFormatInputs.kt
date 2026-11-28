@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.metadata.model.Attribute as CfirAttribute
import org.cangnova.cangjie.metadata.model.AttributePack
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.CjPsiSourceFile
import java.nio.file.Path
import java.util.Collections
import java.util.IdentityHashMap

/** SOURCE 文件自己的格式事实；原文件对象仅是身份，编码期不能读取其字段。 */
internal data class CjoFrozenSourceFileFormat(
    val file: CfirFile,
    val index: UInt,
    val name: String,
    val sourcePath: String?,
    val info: CjoFileInfoMetadata?,
)

/** SOURCE header/名称/位置/属性在 Preparation 完成捕获；不是一份可变 CFIR 镜像。 */
internal data class CjoFrozenSourceDeclarationFormat(
    val identifier: String,
    val kind: UShort,
    val sourcePath: String?,
    val sourceOffset: Int?,
    val isLocal: Boolean,
    val begin: CjoPositionMetadata?,
    val end: CjoPositionMetadata?,
    val identifierPosition: CjoPositionMetadata?,
    val attributes: List<ULong>,
    val overflow: CangjieOverflowStrategy?,
    val fastNative: Boolean,
    val isAnnotation: Boolean,
    val annotationTargets: UByte,
    val annotationTargets2: UByte,
    val runtimeVisible: Boolean,
    val nonExhaustive: Boolean,
    val isVar: Boolean,
    val isNamed: Boolean,
    val isMember: Boolean,
    val isMutable: Boolean,
    val getters: List<CfirPropertyAccessor>,
    val setters: List<CfirPropertyAccessor>,
    val builtin: CfirBuiltInTypeKind?,
    val hasInitializer: Boolean,
)

/**
 * SOURCE 的最后格式输入边界。字节列由实际已解析文件文本和冻结 UTF-16 行映射计算，
 * e/P 端点来自它们自己的 span；Context 只查完成记录并分配输出索引。
 */
internal class CjoSourceFormatInputs(
    private val sourceFiles: List<CfirFile>,
    private val analysis: CfirDeclarationExportAnalysis,
    private val options: CjoExportOptions,
    builtinRoots: List<CfirBuiltInDeclaration>,
) {
    val languageVersion = sourceFiles.first().moduleData.session.languageVersionSettings.languageVersion
    val writesCjmpCommonPart = languageVersion >= LanguageVersion.CANGJIE_1_1_0 && options.serializingCommon
    val packageName = sourceFiles.map { it.packageDirective.packageFqName.asString() }.distinct().single()
    val moduleName = sourceFiles.first().moduleData.name.asString()
    val moduleOptions: CjoModuleOptionMetadata? = if (writesCjmpCommonPart) sourceFiles.first().moduleData.session.cjmpSettings.let {
        CjoModuleOptionMetadata(it.moduleDebug, it.moduleOptLevel.cjoValue)
    } else null
    private val builtins = Collections.newSetFromMap(IdentityHashMap<CfirBuiltInDeclaration, Boolean>()).apply { addAll(builtinRoots) }
    private val fileFormats = IdentityHashMap<CfirFile, CjoFrozenSourceFileFormat>()
    private val fileTexts = IdentityHashMap<CfirFile, String>()
    private val byteOffsets = IdentityHashMap<CfirFile, IntArray>()
    private val sourcePositions = IdentityHashMap<CfirFile, MutableMap<CfirSourceSpan?, Pair<CjoPositionMetadata?, CjoPositionMetadata?>>>()
    private val sourceDeclarations = IdentityHashMap<CfirDeclaration, CjoFrozenSourceDeclarationFormat>()
    val files: List<CjoFrozenSourceFileFormat>
    val declarations: Map<CfirDeclaration, CjoFrozenSourceDeclarationFormat>

    init {
        sourceFiles.forEachIndexed { ordinal, file ->
            val path = file.sourceFile?.path
            val name = if (writesCjmpCommonPart && path != null) path else "${packageName}/${file.name}"
            val index = ordinal.toUInt() + 1u
            fileFormats[file] = CjoFrozenSourceFileFormat(file, index, name, path?.let { Path.of(it).toAbsolutePath().normalize().toString() }, null)
            val coordinates = analysis.sourceCoordinatesOf(file)
            if (coordinates != null) {
                val source = checkNotNull(file.sourceFile) { "SOURCE byte coordinates require the actual source file" }
                val text = if (source is CjPsiSourceFile) source.psiFile.text
                    else source.getContentsAsStream().reader(Charsets.UTF_8).use { it.readText() }
                check(text.length == coordinates.lastOffset) { "SOURCE text and its captured line mapping belong to different generations" }
                fileTexts[file] = text
                byteOffsets[file] = utf8Offsets(text)
                val feature = file.featuresDirective?.takeIf { writesCjmpCommonPart }?.let { directive ->
                    CjoFeaturesDirectiveMetadata(directive.featureIds.map { immutable(it.split('.')) })
                }
                val info = if (languageVersion >= LanguageVersion.CANGJIE_1_1_0)
                    CjoFileInfoMetadata(index, checkNotNull(point(file, 0)), checkNotNull(point(file, coordinates.lastOffset)), feature) else null
                fileFormats[file] = fileFormats.getValue(file).copy(info = info)
            }
        }
        analysis.capturedDeclarations().forEach { declaration ->
            if (declaration.cjoDeclarationOrigin == null && (analysis.isSourceDeclaration(declaration) ||
                declaration in builtins || declaration is CfirTypeParameter && analysis.owningParentOf(declaration) in builtins)) {
                sourceDeclarations[declaration] = declaration(declaration)
            }
        }
        fun pattern(value: CfirSourcePattern, owner: CfirDeclaration) {
            capturePositions(owner, value.span)
            value.patterns.forEach { pattern(it, owner) }
        }
        analysis.canonicalSourceGraphs().forEach { graph ->
            graph.expressions.values.forEach { record ->
                if (record.importedPosition == null) capturePositions(record.context, record.span)
                record.payload.patterns().forEach { pattern(it, record.context) }
            }
            graph.declarationPatterns.forEach { (owner, value) -> pattern(value, owner) }
        }
        files = immutable(sourceFiles.map { fileFormats.getValue(it) })
        declarations = Collections.unmodifiableMap(IdentityHashMap(sourceDeclarations))
    }

    fun positions(owner: CfirDeclaration, span: CfirSourceSpan?): Pair<CjoPositionMetadata?, CjoPositionMetadata?> {
        val file = analysis.sourceFileOf(owner) ?: return null to null
        return sourcePositions.getValue(file).getValue(span)
    }

    /** 最后检查只拒绝代次变化，不重新生成输出、不收集依赖、不修改原声明。 */
    fun validateUnchanged() {
        check(sourceFiles.all { it.moduleData.session.languageVersionSettings.languageVersion == languageVersion &&
            it.packageDirective.packageFqName.asString() == packageName } && sourceFiles.first().moduleData.name.asString() == moduleName)
        if (moduleOptions != null) sourceFiles.forEach { file ->
            val current = file.moduleData.session.cjmpSettings
            check(moduleOptions.debug == current.moduleDebug && moduleOptions.optLevel == current.moduleOptLevel.cjoValue)
        }
        sourceDeclarations.forEach { (source, frozen) -> check(declaration(source) == frozen) {
            "SOURCE declaration format changed after preparation: ${source.symbol}"
        } }
        files.forEach { input ->
            check(input.sourcePath == input.file.sourceFile?.path?.let { Path.of(it).toAbsolutePath().normalize().toString() })
            check(input.name == if (writesCjmpCommonPart && input.file.sourceFile?.path != null) input.file.sourceFile!!.path else "${packageName}/${input.file.name}")
            val feature = input.file.featuresDirective?.takeIf { writesCjmpCommonPart }?.let {
                CjoFeaturesDirectiveMetadata(it.featureIds.map { name -> name.split('.') })
            }
            if (input.info != null) check(input.info.feature == feature)
            fileTexts[input.file]?.let { frozen ->
                val source = checkNotNull(input.file.sourceFile)
                val text = if (source is CjPsiSourceFile) source.psiFile.text else source.getContentsAsStream().reader(Charsets.UTF_8).use { it.readText() }
                check(text == frozen) { "SOURCE file text changed after format preparation" }
            }
        }
    }

    private fun capturePositions(owner: CfirDeclaration, span: CfirSourceSpan?) {
        val file = analysis.sourceFileOf(owner) ?: return
        check(fileFormats.containsKey(file)) { "A SOURCE position has no invocation-owned file" }
        sourcePositions.getOrPut(file) { HashMap() }.getOrPut(span) {
            if (span == null) {
                null to null
            } else point(file, span.beginOffset) to point(file, span.endOffset)
        }
    }

    private fun point(file: CfirFile, offset: Int): CjoPositionMetadata? {
        val coordinates = analysis.sourceCoordinatesOf(file) ?: return null
        if (offset < 0) return null
        val actual = offset.coerceAtMost(coordinates.lastOffset)
        val (line, column) = coordinates.lineAndColumn(actual)
        val bytes = byteOffsets.getValue(file)
        check(bytes[actual] >= 0 && bytes[actual - column] >= 0) { "SOURCE position splits a UTF-16 code point" }
        return CjoPositionMetadata(fileFormats.getValue(file).index, line = line + 1,
            column = bytes[actual] - bytes[actual - column] + 1)
    }

    private fun declaration(declaration: CfirDeclaration): CjoFrozenSourceDeclarationFormat {
        val file = analysis.sourceFileOf(declaration)
        val source = metadataOwner(declaration).source
        val range = identifierRange(declaration)
        val annotation = declaration.annotationInfo
        val targets = annotation?.annotationTargets.orEmpty().fold(0) { mask, target -> mask or (1 shl target.bitPosition) }
        val owner = if (declaration is CfirValueParameter || declaration is CfirTypeParameter) analysis.ownerOf(declaration) else declaration
        return CjoFrozenSourceDeclarationFormat(name(declaration), kind(declaration),
            file?.let { fileFormats.getValue(it).sourcePath }, declaration.source?.startOffset,
            (owner as? CfirCallableDeclaration)?.isLocal == true,
            source?.let { if (file != null) point(file, it.startOffset) else null },
            source?.let { if (file != null) point(file, it.endOffset) else null },
            range?.let { if (file != null) point(file, it.startOffset) else null },
            sourceAttributes(declaration), annotation?.overflowStrategy ?: declaration.interopInfo?.overflowStrategy,
            declaration.interopInfo?.isFastNative == true, annotation?.isAnnotation == true,
            targets.toUByte(), (targets ushr 8).toUByte(), annotation?.runtimeVisible == true,
            (declaration as? CfirEnum)?.isNonExhaustive == true,
            (declaration as? CfirVariable)?.isVar == true, (declaration as? CfirValueParameter)?.isNamed == true,
            (declaration as? CfirVariable)?.isMemberParameter == true,
            (declaration as? CfirProperty)?.status?.isMut == true,
            immutable((declaration as? CfirProperty)?.getter?.let(::listOf).orEmpty()),
            immutable((declaration as? CfirProperty)?.setter?.let(::listOf).orEmpty()),
            (declaration as? CfirBuiltInDeclaration)?.kind,
            declaration is CfirVariable && analysis.initializerOf(declaration) != null)
    }

    private fun name(declaration: CfirDeclaration): String = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.name.asString()
        is CfirExtend, is CfirPatternVariable -> ""
        is CfirConstructor -> if (analysis.statusOf(declaration).isStatic) "static.init" else "init"
        is CfirFinalizer -> "~init"
        is CfirPropertyAccessor -> checkNotNull(identifierText(declaration)) { "A property accessor has no actual source token" }
        is CfirCallableDeclaration -> declaration.symbol.name.asString()
        is CfirTypeParameter -> declaration.name.asString()
        else -> error("Unsupported SOURCE declaration format")
    }

    private fun kind(declaration: CfirDeclaration): UShort = when (declaration) {
        is CfirBuiltInDeclaration -> PackageFormat.DeclKind.BuiltInDecl
        is CfirClass -> PackageFormat.DeclKind.ClassDecl
        is CfirInterface -> PackageFormat.DeclKind.InterfaceDecl
        is CfirStruct -> PackageFormat.DeclKind.StructDecl
        is CfirEnum -> PackageFormat.DeclKind.EnumDecl
        is CfirTypeAlias -> PackageFormat.DeclKind.TypeAliasDecl
        is CfirProperty -> PackageFormat.DeclKind.PropDecl
        is CfirEnumConstructor -> if (analysis.precedingFactsOf(declaration).enumConstructorHasParameterList ||
            analysis.valueParametersOf(declaration).isNotEmpty()) PackageFormat.DeclKind.FuncDecl else PackageFormat.DeclKind.VarDecl
        is CfirFunction -> PackageFormat.DeclKind.FuncDecl
        is CfirValueParameter -> PackageFormat.DeclKind.FuncParam
        is CfirTypeParameter -> PackageFormat.DeclKind.GenericParamDecl
        is CfirPatternVariable -> PackageFormat.DeclKind.VarWithPatternDecl
        is CfirVariable -> PackageFormat.DeclKind.VarDecl
        is CfirExtend -> PackageFormat.DeclKind.ExtendDecl
        else -> error("Unsupported SOURCE declaration kind")
    }

    private fun identifierRange(declaration: CfirDeclaration) =
        declaration.source?.let(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME::markOrNull)?.singleOrNull()

    private fun identifierText(declaration: CfirDeclaration): String? {
        val source = declaration.source ?: return null
        val range = identifierRange(declaration) ?: return null
        val text = when (source) {
            is CjPsiSourceElement -> source.psi.text
            is CjLightSourceElement -> source.treeStructure.toString(source.lighterASTNode)
            else -> return null
        }
        val begin = range.startOffset - source.startOffset
        val end = range.endOffset - source.startOffset
        if (begin < 0 || end > text.length || begin > end) return null
        return text.subSequence(begin, end).toString()
    }

    private fun simplePatternOwner(binding: CfirPatternBindingVariable): CfirPatternVariable? = analysis.simplePatternOwnerOf(binding)
    private fun metadataOwner(declaration: CfirDeclaration): CfirDeclaration =
        (declaration as? CfirPatternBindingVariable)?.let(::simplePatternOwner) ?: declaration
    private fun patternMetadataOwner(declaration: CfirDeclaration): CfirDeclaration = metadataOwner(declaration)

    private fun addCjmpAttributes(
        values: MutableSet<CfirAttribute>,
        declaration: CfirDeclaration,
        status: org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus,
    ) {
        if (writesCjmpCommonPart) values += CfirAttribute.FROM_COMMON_PART
        if (status.isSpecific) values += CfirAttribute.SPECIFIC
        if (!status.isCommon) return
        values += CfirAttribute.COMMON
        val hasDefault = when (declaration) {
            is CfirPatternBindingVariable -> simplePatternOwner(declaration)?.cjmpHasCommonDefault() == true
            else -> declaration.cjmpHasCommonDefault()
        }
        if (status.isCommonWithDefault || hasDefault) {
            values += CfirAttribute.COMMON_WITH_DEFAULT
        }
    }


    private fun sourceAttributes(declaration: CfirDeclaration): List<ULong> {
        val topLevel = analysis.isTopLevel(declaration)
        if (declaration is CfirTypeParameter) {
            val owner = analysis.owningParentOf(declaration) as? CfirBuiltInDeclaration
            if (owner != null && builtins.contains(owner)) {
                check(owner.typeParameters.any { it === declaration }) {
                    "A synthetic std.core BuiltInDecl type parameter must remain owned by its declaration"
                }
                // 官方 AddBuiltIn*Decl 通过 MakeOwnedNode 标记内建泛型参数为
                // COMPILER_ADD。INITIALIZATION_CHECKED 是初始化检查 pass 的瞬时状态，
                // 不能因为声明来自 builtin provider 就伪造到 CJO 中。
                return setOf(CfirAttribute.COMPILER_ADD).toAttributeWords()
            }
        }
        if (declaration is CfirBuiltInDeclaration) {
            check(builtins.contains(declaration) && topLevel) {
                "A synthetic BuiltInDecl can only be written as an injected std.core package root"
            }
            check(declaration.status.visibility == declaration.kind.visibility) {
                "A synthetic BuiltInDecl root must retain its official visibility"
            }
            // 只写官方 AddBuiltIn*Decl 的声明属性；初始化检查的防循环状态必须由对应 pass 发布。
            return buildSet {
                add(CfirAttribute.GLOBAL)
                if (declaration.typeParameters.isNotEmpty()) add(CfirAttribute.GENERIC)
                when (declaration.kind.visibility) {
                    Visibilities.Internal -> add(CfirAttribute.INTERNAL)
                    Visibilities.Public -> add(CfirAttribute.PUBLIC)
                    else -> error("Unsupported builtin CJO visibility: ${declaration.kind.visibility}")
                }
            }.toAttributeWords()
        }
        val values = linkedSetOf<CfirAttribute>()
        val preceding = analysis.precedingFactsOf(declaration)
        if (preceding.isInitialized) values += CfirAttribute.INITIALIZED
        if (preceding.isCompilerAdded) values += CfirAttribute.COMPILER_ADD
        if (preceding.hasInitial) values += CfirAttribute.HAS_INITIAL
        if (preceding.isImplicitlyAdded) values += CfirAttribute.IMPLICIT_ADD
        if (preceding.hasNoReflectInfo) values += CfirAttribute.NO_REFLECT_INFO
        if (analysis.precedingFactsOf(declaration).isImplicitlyUsed) values += CfirAttribute.IMPLICIT_USED
        if (analysis.precedingFactsOf(declaration).isToolAdded) values += CfirAttribute.TOOL_ADD
        // GLOBAL 属于语言声明；输出顶层索引可以排除由参数拥有的全局 helper。
        if (preceding.isGlobal) values += CfirAttribute.GLOBAL
        if (declaration is CfirValueParameter) {
            val hasInitial = analysis.hasInitialDefaultOf(declaration)
            if (hasInitial) values += CfirAttribute.HAS_INITIAL
        }
        val status = (declaration as? CfirMemberDeclaration)?.status ?: return values.toAttributeWords()
        // CJO 中构造器同样写为 FuncDecl，必须保留官方身份位供反序列化恢复声明种类。
        if (declaration is CfirConstructor) {
            values += CfirAttribute.CONSTRUCTOR
            if (declaration.isPrimary && declaration.origin != CfirDeclarationOrigin.Synthetic.ImplicitConstructor)
                values += CfirAttribute.PRIMARY_CONSTRUCTOR
        } else if (declaration is CfirEnumConstructor) {
            values += CfirAttribute.ENUM_CONSTRUCTOR
        } else if (declaration is CfirFinalizer) {
            // native 的 finalizer 仍是 FuncDecl；身份来自独立 FINALIZER 位，不来自 Unit 或 body。
            values += CfirAttribute.FINALIZER
        }
        if (status.isC || declaration.interopInfo?.resolvedAbi?.kind == CfirAbiKind.C) values += CfirAttribute.C
        if (status.isForeign) values += CfirAttribute.FOREIGN
        if (status.isStatic) values += CfirAttribute.STATIC
        if (status.isAbstract) values += CfirAttribute.ABSTRACT
        // SetDefaultFunc 的独立 DEFAULT 位必须原样保存，不能由 body 或 COMMON_WITH_DEFAULT 重建。
        if (status.isDefault) values += CfirAttribute.DEFAULT
        if (status.isOpen) values += CfirAttribute.OPEN
        if (status.isSealed) values += CfirAttribute.SEALED
        if (status.isOverride) values += CfirAttribute.OVERRIDE
        if (status.isRedef) values += CfirAttribute.REDEF
        if (status.isUnsafe) values += CfirAttribute.UNSAFE
        if (status.isMut) values += CfirAttribute.MUT
        addCjmpAttributes(values, declaration, status)
        val hasGenericTypeParameters = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.typeParameters.filterIsInstance<CfirTypeParameter>().isNotEmpty()
            is CfirConstructor -> false
            is CfirFunction -> declaration.typeParameters.isNotEmpty()
            is CfirExtend -> declaration.typeParameters.isNotEmpty()
            else -> false
        }
        // Attribute.GENERIC 同时标记 class-like 与 extend owner，供 CJO 消费端的实例化筛选使用。
        if (hasGenericTypeParameters) values += CfirAttribute.GENERIC
        val metadataOwner = patternMetadataOwner(declaration)
        if (metadataOwner.annotationInfo?.isIntrinsic == true) values += CfirAttribute.INTRINSIC
        if (metadataOwner.annotationInfo?.isMockSupported == true) values += CfirAttribute.MOCK_SUPPORTED
        when (declaration.status.visibility) {
            Visibilities.Public -> values += CfirAttribute.PUBLIC
            Visibilities.Private -> values += CfirAttribute.PRIVATE
            Visibilities.Protected -> values += CfirAttribute.PROTECTED
            Visibilities.Internal -> values += CfirAttribute.INTERNAL
            else -> Unit
        }
        if (preceding.isImported) values += CfirAttribute.IMPORTED
        if (declaration is CfirFunction && preceding.isGenericInstantiated && analysis.hasOwnGenericNode(declaration)) values += CfirAttribute.UNREACHABLE
        return values.toAttributeWords()
    }


    private fun Collection<CfirAttribute>.toAttributeWords(): List<ULong> {
        // Native SaveAttributes总是保存完整bitset宽度，零属性也不能变成缺失/空vector。
        return AttributePack().apply { this@toAttributeWords.forEach { setAttr(it) } }.toULongList()
    }


    private fun <T> immutable(values: List<T>): List<T> = Collections.unmodifiableList(values.toList())

    private fun utf8Offsets(text: String): IntArray {
        val offsets = IntArray(text.length + 1)
        var cursor = 0
        var bytes = 0
        while (cursor < text.length) {
            offsets[cursor] = bytes
            val codePoint = Character.codePointAt(text, cursor)
            val width = Character.charCount(codePoint)
            check(!Character.isSurrogate(text[cursor]) || width == 2) { "SOURCE byte coordinates require valid UTF-16 text" }
            if (width == 2) offsets[cursor + 1] = -1
            bytes += when {
                codePoint <= 0x7f -> 1
                codePoint <= 0x7ff -> 2
                codePoint <= 0xffff -> 3
                else -> 4
            }
            cursor += width
        }
        offsets[text.length] = bytes
        return offsets
    }
}

