import os, time

R = 'D:/code/intellij/cangjie'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def robust_write(path, text):
    data = text.encode('utf-8')
    tmp = path + '.cjmp.tmp'
    last = None
    for _ in range(20):
        try:
            with open(tmp, 'wb') as f:
                f.write(data)
            os.replace(tmp, path)
            return
        except OSError as e:
            last = e
            time.sleep(0.5)
    raise last


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)})"
    return text.replace(old2, new2, 1)


# =====================================================================
# 1) 写侧元数据模型 + Package.options 写出（CjoPackageWriter.kt）
# =====================================================================
p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoPackageWriter.kt'
t = read(p)
t = replace_once(
    t,
    '''/** Official ModuleFormat.FileInfo input model. */
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
}''',
    '''/** Official ModuleFormat.FileInfo input model. */
data class CjoFileInfoMetadata(
    val fileId: UInt,
    val begin: CjoPositionMetadata,
    val end: CjoPositionMetadata,
    /**
     * CJMP：该文件的 `features` 指令（官方 `FileInfo.feature`）。
     *
     * 仅 CJMP 写出路径（[CjoSchemaProfile.OFFICIAL_CJMP]）填充；其余 profile 为 null，
     * 由 `validateSchemaProfile` 兜底拒绝。
     */
    val feature: CjoFeaturesDirectiveMetadata? = null,
) {
    fun write(builder: FlatBufferBuilder): Int {
        FileInfo.startFileInfo(builder)
        FileInfo.addFileID(builder, fileId)
        FileInfo.addBegin(builder, begin.write(builder))
        FileInfo.addEnd(builder, end.write(builder))
        feature?.let { FileInfo.addFeature(builder, it.write(builder)) }
        return FileInfo.endFileInfo(builder)
    }
}

/**
 * CJMP：单个源文件的 `features` 指令元数据（官方 `FileInfo.feature`）。
 *
 * 结构与官方一致：`FeaturesDirective -> featuresSet -> features[FeatureId] ->
 * identifiers[StringId].str`。每个 [features] 元素是一个 feature 的分段标识列表
 *（官方 `SrcIdentifier` 列表）。
 */
data class CjoFeaturesDirectiveMetadata(
    /** 每个 feature 的 identifier 分段列表；顺序与官方 `FeaturesSet.features` 一致。 */
    val features: List<List<String>> = emptyList(),
) {
    fun write(builder: FlatBufferBuilder): Int {
        val featureOffsets = features.map { identifiers ->
            val identifierOffsets = identifiers
                .map { identifier -> StringId.createStringId(builder, builder.createString(identifier)) }
                .toIntArray()
            FeatureId.createFeatureId(
                builder,
                FeatureId.createIdentifiersVector(builder, identifierOffsets),
            )
        }.toIntArray()
        val featuresSetOffset = FeaturesSet.createFeaturesSet(
            builder,
            FeaturesSet.createFeaturesVector(builder, featureOffsets),
        )
        return FeaturesDirective.createFeaturesDirective(builder, featuresSetOffset)
    }
}

/**
 * CJMP：common part cjo 内嵌的编译选项（官方 `Package.options`）。
 *
 * 官方 `ASTLoader::ValidateOptions` 用它比对 debug 与优化级别；不一致会导致后续
 * desugar/CHIR 差异，因此加载期必须拒绝。
 */
data class CjoModuleOptionMetadata(
    /** 官方 `Option::debug`。 */
    val debug: Boolean = false,
    /** 官方优化级别序号（O0=0, O1=1, O2=2, O3=3, Os=4, Oz=5）。 */
    val optLevel: UByte = 0u,
) {
    fun write(builder: FlatBufferBuilder): Int = Option.createOption(builder, debug, optLevel)
}''',
    'writer metadata models')
t = replace_once(
    t,
    '''    /** Official `Package.allDependentStdPkgs` package names. */
    val allDependentStdPkgs: List<String> = emptyList(),''',
    '''    /** Official `Package.allDependentStdPkgs` package names. */
    val allDependentStdPkgs: List<String> = emptyList(),
    /**
     * CJMP：内嵌的编译选项（官方 `Package.options`）。
     *
     * 仅 [CjoSchemaProfile.OFFICIAL_CJMP] / [CjoSchemaProfile.REPOSITORY_EXTENDED] 允许写出。
     */
    val options: CjoModuleOptionMetadata? = null,''',
    'writer metadata field')
t = replace_once(
    t,
    '''        validateCompositeValueReferences(metadata)''',
    '''        val optionsOffset = metadata.options?.write(builder) ?: 0
        validateCompositeValueReferences(metadata)''',
    'writer options offset')
t = replace_once(
    t,
    '''        if (allDependentStdPkgsOffset != 0) {
            Package.addAllDependentStdPkgs(builder, allDependentStdPkgsOffset)
        }''',
    '''        if (allDependentStdPkgsOffset != 0) {
            Package.addAllDependentStdPkgs(builder, allDependentStdPkgsOffset)
        }
        if (optionsOffset != 0) {
            Package.addOptions(builder, optionsOffset)
        }''',
    'writer addOptions')
robust_write(p, t)
print('1 writer: metadata models + Package.options')

# =====================================================================
# 2) 包头解析：cjoVersion / options / fileFeatures / isCjmpCommonPart
# =====================================================================
p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoPackageHeader.kt'
t = read(p)
t = replace_once(
    t,
    '''import PackageFormat.DeclKind
import PackageFormat.ImportSpec
import PackageFormat.Package
import org.cangnova.cangjie.name.Name''',
    '''import PackageFormat.DeclKind
import PackageFormat.ImportSpec
import PackageFormat.Package
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.Name''',
    'header import')
t = replace_once(
    t,
    '''    /**
     * `FullId.decl` 跨包引用键到 `allDecls` 索引的映射。''',
    '''    /**
     * cjo 格式版本（官方 `Package.cjoVersion`）。
     *
     * `null` 表示该 cjo 未声明格式版本——加载门按"缺版本即拒"处理（计划 §8.3）。
     */
    val cjoVersion: CjoModuleVersion?,
    /**
     * CJMP：内嵌的编译选项（官方 `Package.options`）；非 CJMP 来源为 `null`。
     */
    val options: CjoModuleOptionInfo?,
    /**
     * CJMP：文件级 features（官方 `FileInfo.feature`），键与 `allFiles` 条目同名。
     */
    val fileFeatures: Map<String, List<List<String>>>,
    /**
     * 该包是否携带 CJMP 内容。
     *
     * 判据（官方 common part cjo 特征）：带 options/fileFeatures，或任一声明带
     * `COMMON`/`FROM_COMMON_PART`/`SPECIFIC`/`COMMON_WITH_DEFAULT` 属性位。
     */
    val isCjmpCommonPart: Boolean,
    /**
     * `FullId.decl` 跨包引用键到 `allDecls` 索引的映射。''',
    'header fields')
t = replace_once(
    t,
    '''            val classNames = mutableSetOf<Name>()''',
    '''            val cjoVersion = pkg.cjoVersion?.let { version ->
                CjoModuleVersion(version.majorNum, version.minorNum, version.patchNum)
            }
            val options = pkg.options?.let { option ->
                CjoModuleOptionInfo(debug = option.debug, optLevel = option.optLevel)
            }
            val fileFeatures = linkedMapOf<String, List<List<String>>>()
            for (fileIndex in 0 until pkg.allFileInfoLength) {
                val info = pkg.allFileInfo(fileIndex) ?: continue
                val directive = info.feature ?: continue
                val fileName = if (fileIndex < pkg.allFilesLength) pkg.allFiles(fileIndex) else null
                if (fileName.isNullOrBlank()) continue
                val features = buildList {
                    val featureSet = directive.featuresSet ?: return@buildList
                    for (featureIndex in 0 until featureSet.featuresLength) {
                        val feature = featureSet.features(featureIndex) ?: continue
                        val identifiers = buildList {
                            for (identifierIndex in 0 until feature.identifiersLength) {
                                feature.identifiers(identifierIndex)?.str?.let(::add)
                            }
                        }
                        add(identifiers)
                    }
                }
                fileFeatures[fileName] = features
            }
            var hasCjmpAttributes = false
            val classNames = mutableSetOf<Name>()''',
    'header parse new fields')
t = replace_once(
    t,
    '''                if (decl.kind != DeclKind.ExtendDecl && fullIdReferenceKey != null) {''',
    '''                if (!hasCjmpAttributes && declHasCjmpAttributes(decl)) {
                    hasCjmpAttributes = true
                }

                if (decl.kind != DeclKind.ExtendDecl && fullIdReferenceKey != null) {''',
    'header cjmp scan')
t = replace_once(
    t,
    '''                kind = pkg.kind,
                access = pkg.access,''',
    '''                cjoVersion = cjoVersion,
                options = options,
                fileFeatures = fileFeatures,
                isCjmpCommonPart = hasCjmpAttributes || options != null || fileFeatures.isNotEmpty(),
                kind = pkg.kind,
                access = pkg.access,''',
    'header ctor args')
t = t.rstrip('\n') + '''

/**
 * 声明是否带 CJMP 属性位（官方 `Attribute::COMMON / FROM_COMMON_PART / SPECIFIC /
 * COMMON_WITH_DEFAULT`）。
 *
 * 位下标使用官方 AST AttributePack 布局（见 `CfirDeclDeserializer.AttrBit`）。
 */
private fun declHasCjmpAttributes(decl: PackageFormat.Decl): Boolean {
    val cjmpBits = intArrayOf(
        Attribute.COMMON.ordinal,
        Attribute.FROM_COMMON_PART.ordinal,
        Attribute.SPECIFIC.ordinal,
        Attribute.COMMON_WITH_DEFAULT.ordinal,
    )
    for (wordIndex in 0 until decl.attributesLength) {
        val word = decl.attributes(wordIndex)
        for (bit in cjmpBits) {
            if (wordIndex == bit / 64 && (word shr (bit % 64)) and 1uL == 1uL) return true
        }
    }
    return false
}
'''
robust_write(p, t)
print('2 header: parse + cjmp detection')

print('phase4_3b-part1 complete')
