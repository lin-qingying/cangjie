package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantIdentity
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantGraph
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantNode
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import java.math.BigInteger
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.metadata.model.AttributePack
import java.nio.file.Path
import java.util.Collections
import java.util.IdentityHashMap

/** 原位置已绑定自己的 FILE/import 域；编码只分配输出编号，不再读包或诊断路径。 */
internal data class CjoFrozenSourcePosition(
    val file: CjoSnapshotTableEntry?,
    val importedPackage: String?,
    val savedFile: UInt,
    val line: Int,
    val column: Int,
    val ignore: Boolean,
)

internal sealed interface CjoFrozenDeclarationTarget {
    data object Invalid : CjoFrozenDeclarationTarget
    data class Current(val declaration: CfirDeclaration) : CjoFrozenDeclarationTarget
    data class External(val packageName: String, val declarationKey: String) : CjoFrozenDeclarationTarget
    data class Package(val packageName: String, val index: UInt) : CjoFrozenDeclarationTarget
}

/** 保存字段的语义定位与原 FullId 标量独立；CURRENT 的 index 仅在输出时按实际声明重映射。 */
internal data class CjoFrozenSourceReference(
    val target: CjoFrozenDeclarationTarget?,
    val declarationKey: String?,
    val index: UInt,
)

internal data class CjoFrozenConstraint(
    val type: ConeCangJieType,
    val upperBounds: List<ConeCangJieType>,
    val isImplicitlyIntroduced: Boolean,
    val begin: CjoFrozenSourcePosition?,
    val end: CjoFrozenSourcePosition?,
)

internal data class CjoFrozenGeneric(
    val parameters: List<CfirTypeParameter>,
    val constraints: List<CjoFrozenConstraint>,
)

/** FuncInfo 非内容标量和原 F 是否存在；参数/F/body 使用完成的 canonical/header 产品。 */
internal data class CjoFrozenFunctionInfo(
    val operation: UByte,
    val hasFunctionBody: Boolean,
    val header: CfirSourceImportedFunctionHeader?,
    val autoDiff: CjoAutoDiffInfo?,
    val isConst: Boolean,
    val isInline: Boolean,
    val isFastNative: Boolean,
    val overflowPolicy: UByte?,
)

internal data class CjoFrozenNominalInfo(
    val inheritedTypes: List<ConeCangJieType>,
    val body: List<CfirDeclaration>,
    val autoDiff: CjoAutoDiffInfo?,
    val isAnnotation: Boolean = false,
    val annotationTargets: UByte = 0u,
    val annotationTargets2: UByte = 0u,
    val runtimeVisible: Boolean = false,
    val hasArguments: Boolean = false,
    val nonExhaustive: Boolean = false,
    val ellipsis: CjoFrozenSourcePosition? = null,
)

/** 全部为复制的原 header 事实及真实绑定；不包含 Decl/Expr/FuncBody/Position/ByteBuffer。 */
internal data class CjoFrozenDeclarationFormat(
    val source: CjoLoadedPackage,
    val origin: CjoSnapshotTableEntry,
    val identifier: String,
    val kind: UShort,
    val infoType: UByte,
    val isTopLevel: Boolean,
    val fullPackageName: String?,
    val exportId: String?,
    val mangledName: String?,
    val mangledBeforeSema: String?,
    val hash: CjoDeclHashMetadata?,
    val type: ConeCangJieType?,
    val begin: CjoFrozenSourcePosition?,
    val end: CjoFrozenSourcePosition?,
    val identifierPosition: CjoFrozenSourcePosition?,
    val attributes: List<ULong>,
    val genericDecl: CjoFrozenDeclarationTarget?,
    val generic: CjoFrozenGeneric?,
    val dependencies: List<CjoFrozenDeclarationTarget>,
    val function: CjoFrozenFunctionInfo?,
    val nominal: CjoFrozenNominalInfo?,
    val isVar: Boolean,
    val isConst: Boolean,
    val isMemberParameter: Boolean,
    val isNamedParameter: Boolean,
    val isMutable: Boolean,
    val getters: List<CfirDeclaration>,
    val setters: List<CfirDeclaration>,
    val aliasedType: ConeCangJieType?,
    val hasConstantValue: Boolean,
    val isLocal: Boolean,
    val builtin: CfirBuiltInTypeKind?,
    val hasInitializer: Boolean,
)

internal sealed interface CjoFrozenConstantStorage {
    data class Scalar(val value: CjoConstValueInfo) : CjoFrozenConstantStorage
    data object Array : CjoFrozenConstantStorage
    data class Composite(val domain: CfirSession, val fields: List<CjoFrozenConstantField>) : CjoFrozenConstantStorage
}

internal data class CjoFrozenConstantField(
    val name: String,
    val type: ConeCangJieType,
    val value: CfirExportConstantIdentity,
)

/**
 * 输出的最后输入边界。raw 包只在创建这个产品时读取；SOURCE/imported encoder 只查这些
 * 不可变事实。来源对象仅作不透明身份，不能通过它在编码阶段重新读取 pkg。
 */
internal class CjoFrozenSourceInputs private constructor(
    val packages: Map<Path, CjoLoadedPackage>,
    val formats: Map<CfirDeclaration, CjoFrozenDeclarationFormat>,
    val declarationOrigins: Map<CfirDeclaration, CjoSnapshotTableEntry>,
    val canonicalDeclarations: Map<CfirDeclaration, CfirDeclaration>,
    val externalDeclarations: Map<CfirDeclaration, CjoFrozenDeclarationTarget.External>,
    val fileNames: Map<CjoSnapshotTableEntry, String>,
    val positions: Map<CfirSourcePositionIdentity, CjoFrozenSourcePosition>,
    val constants: Map<CfirExportConstantIdentity, CjoFrozenConstantStorage>,
    val savedReferences: Map<CfirSourceSavedReference, CjoFrozenSourceReference>,
    private val typeTargets: Map<CfirSession, Map<ConeCangJieType, CjoFrozenDeclarationTarget>>,
    private val constantDomains: Map<CfirExportConstantIdentity, CfirSession>,
    private val invalidTypeInputs: Map<CfirSession, Set<ConeCangJieType>>,
) {
    fun typeTarget(type: ConeCangJieType, domain: CfirSession): CjoFrozenDeclarationTarget? = typeTargets[domain]?.get(type)
    fun constantDomain(identity: CfirExportConstantIdentity): CfirSession = constantDomains.getValue(identity)
    fun isSavedInvalid(type: ConeCangJieType, domain: CfirSession): Boolean = type in invalidTypeInputs[domain].orEmpty()
    companion object {
        fun capture(
            analysis: CfirDeclarationExportAnalysis,
            snapshots: Collection<CjoSourceExportSnapshot>,
            loadedPackages: Collection<CjoLoadedPackage>,
            options: CjoExportOptions,
            graphs: Collection<CfirSourceGraph>,
        ): CjoFrozenSourceInputs = Capture(analysis, snapshots, loadedPackages, options, graphs).run()
    }

    private class Capture(
        private val analysis: CfirDeclarationExportAnalysis,
        snapshots: Collection<CjoSourceExportSnapshot>,
        loadedPackages: Collection<CjoLoadedPackage>,
        private val options: CjoExportOptions,
        private val graphs: Collection<CfirSourceGraph>,
    ) {
        private val sources = snapshots.toList()
        private val sourcesByDeclaration = IdentityHashMap<CfirDeclaration, CjoSourceExportSnapshot>().apply {
            snapshots.forEach { snapshot ->
                snapshot.declarations.values.forEach { declaration ->
                    check(put(declaration, snapshot).let { it == null || it.binderIdentity === snapshot.binderIdentity }) {
                        "A CJO declaration must belong to one completed binder"
                    }
                }
            }
        }
        private val packages = CjoExportSourceSnapshots.uniqueLoadedPackages(loadedPackages + snapshots.map { it.source })
            .associateBy { it.sourcePath.toAbsolutePath().normalize() }.toMutableMap()
        private val formats = IdentityHashMap<CfirDeclaration, CjoFrozenDeclarationFormat>()
        private val origins = IdentityHashMap<CfirDeclaration, CjoSnapshotTableEntry>()
        private val canonicalDeclarations = IdentityHashMap<CfirDeclaration, CfirDeclaration>()
        private val constantDomains = HashMap<CfirExportConstantIdentity, CfirSession>()
        private val externalDeclarations = IdentityHashMap<CfirDeclaration, CjoFrozenDeclarationTarget.External>()
        private val fileNames = linkedMapOf<CjoSnapshotTableEntry, String>()
        private val positions = IdentityHashMap<CfirSourcePositionIdentity, CjoFrozenSourcePosition>()
        private val constants = linkedMapOf<CfirExportConstantIdentity, CjoFrozenConstantStorage>()
        private val savedReferences = IdentityHashMap<CfirSourceSavedReference, CjoFrozenSourceReference>()
        private val typeTargets = IdentityHashMap<CfirSession, Map<ConeCangJieType, CjoFrozenDeclarationTarget>>()
        private val invalidTypeInputs = IdentityHashMap<CfirSession, MutableSet<ConeCangJieType>>()

        fun run(): CjoFrozenSourceInputs {
            analysis.capturedDeclarations().forEach { declaration ->
                val canonical = if (declaration is CfirTypeParameter) declaration.symbol.cfir
                    else if (declaration is CfirCallableDeclaration) declaration.unwrapSubstitutionOverrides() else declaration
                canonicalDeclarations[declaration] = canonical
                val origin = canonical.cjoDeclarationOrigin ?: return@forEach
                check(declaration.cjoDeclarationOrigin?.let { it == origin } != false) {
                    "A declaration view has different CJO provenance from its canonical definition"
                }
                val path = Path.of(origin.packagePath).toAbsolutePath().normalize()
                val source = checkNotNull(CjoExportSourceSnapshots.loadedPackage(
                    canonical.moduleData.session.symbolProvider, path,
                )) { "Export input has no actual loaded source for $path" }
                check(packages.putIfAbsent(path, source).let { it == null || it === source }) {
                    "Export provenance cannot substitute another context at $path"
                }
                origins[declaration] = CjoSnapshotTableEntry(source, CjoSnapshotTableDomain.DECLARATION, origin.declarationIndex)
                val raw = checkNotNull(source.pkg.allDecls(origin.declarationIndex))
                externalDeclarations[declaration] = CjoFrozenDeclarationTarget.External(
                    source.header.fullPkgName,
                    raw.exportId?.takeIf(String::isNotBlank) ?: checkNotNull(raw.identifier),
                )
                val snapshot = sourcesByDeclaration[canonical] ?: return@forEach
                check(snapshot.source === source && snapshot.declarations[origin.declarationIndex] === canonical) {
                    "A format header does not belong to its actual canonical declaration"
                }
                // 替换视图保留引用来源，格式头仅归同一加载快照中的真实声明。
                formats.getOrPut(canonical) { declaration(snapshot, canonical, raw, origin.declarationIndex) }
            }
            sources.forEach { snapshot ->
                val targets = IdentityHashMap<ConeCangJieType, CjoFrozenDeclarationTarget>()
                snapshot.types.forEach { (index, type) ->
                    val wire = checkNotNull(snapshot.source.pkg.allTypes(index))
                    if (wire.kind == PackageFormat.TypeKind.Invalid) invalidTypeInputs.getOrPut(snapshot.domain) {
                        Collections.newSetFromMap(IdentityHashMap())
                    }.add(type)
                    if (wire.infoType == PackageFormat.SemaTyInfo.CompositeTyInfo) {
                        val info = wire.info(PackageFormat.CompositeTyInfo()) as PackageFormat.CompositeTyInfo
                        targets[type] = target(snapshot, checkNotNull(info.declPtr))
                    }
                }
                val previous = typeTargets[snapshot.domain]
                if (previous == null) typeTargets[snapshot.domain] = immutable(targets)
                else {
                    val combined = IdentityHashMap(previous)
                    targets.forEach { (type, target) ->
                        check(combined.put(type, target).let { it == null || it == target })
                    }
                    typeTargets[snapshot.domain] = immutable(combined)
                }
                snapshot.positions.forEach { (identity, value) -> positions[identity] = position(snapshot.source, value) }
                snapshot.constants.forEach { (value, storage) ->
                    val identity = CfirExportConstantIdentity.of(value)
                    val frozen = when (storage) {
                        is CjoSavedConstantStorage.Scalar -> CjoFrozenConstantStorage.Scalar(storage.value)
                        CjoSavedConstantStorage.Array -> CjoFrozenConstantStorage.Array
                        is CjoSavedConstantStorage.Composite -> CjoFrozenConstantStorage.Composite(snapshot.domain, immutable(storage.fields.map {
                            CjoFrozenConstantField(it.name, it.type, CfirExportConstantIdentity.of(it.value))
                        }))
                    }
                    check(constants.put(identity, frozen).let { it == null || it == frozen })
                    check(constantDomains.put(identity, snapshot.domain).let { it == null || it === snapshot.domain })
                }
            }
            if (options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA) {
                analysis.capturedDeclarations().filterIsInstance<CfirVariable>().filter(analysis::isSourceDeclaration)
                    .forEach { owner -> analysis.constantValueOf(owner)?.let { captureSourceConstant(it, owner) } }
            }
            if (options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA) {
                val binders = IdentityHashMap<Any, CjoSourceExportSnapshot>()
                sources.forEach { snapshot ->
                    check(binders.put(snapshot.binderIdentity, snapshot).let { it == null || it.source === snapshot.source })
                }
                graphs.forEach { graph -> graph.expressions.values.forEach recordLoop@{ record ->
                    val saved = when (val payload = record.payload) {
                        is CfirSourceExpressionPayload.Array -> payload.savedInitializer
                        is CfirSourceExpressionPayload.ArrayLiteral -> payload.savedInitializer
                        else -> null
                    } ?: return@recordLoop
                    if (!savedReferences.containsKey(saved)) {
                        val raw = saved as? CjoSavedSourceReference ?: error("Unknown SOURCE saved-reference owner")
                        val snapshot = checkNotNull(binders[raw.binderIdentity]) { "A saved reference has no completed source binder snapshot" }
                        val target = when (val target = raw.target) {
                            CjoSavedSourceReference.Target.Missing -> null
                            CjoSavedSourceReference.Target.Invalid -> CjoFrozenDeclarationTarget.Invalid
                            is CjoSavedSourceReference.Target.Current -> {
                                val declaration = snapshot.declarations.getValue(target.rawIndex)
                                check(snapshot.source.pkg.allDecls(target.rawIndex)?.kind == target.declarationKind)
                                check(formats.containsKey(declaration)) { "A saved-only current reference has no frozen declaration header" }
                                CjoFrozenDeclarationTarget.Current(declaration)
                            }
                            is CjoSavedSourceReference.Target.External -> CjoFrozenDeclarationTarget.External(target.packageName, target.declarationKey)
                            is CjoSavedSourceReference.Target.Package -> CjoFrozenDeclarationTarget.Package(target.packageName, target.index)
                        }
                        savedReferences[saved] = CjoFrozenSourceReference(target, raw.declarationKey, raw.index)
                    }
                } }
            }
            return CjoFrozenSourceInputs(immutable(packages), immutable(formats), immutable(origins), immutable(canonicalDeclarations),
                immutable(externalDeclarations), immutable(fileNames), immutable(positions), immutable(constants),
                immutable(savedReferences),
                immutable(typeTargets), immutable(constantDomains), immutable(invalidTypeInputs.mapValues { (_, inputs) ->
                    Collections.unmodifiableSet(Collections.newSetFromMap(IdentityHashMap<ConeCangJieType, Boolean>()).apply { addAll(inputs) })
                }))
        }

        /** 只为 SOURCE 已证明的表示完成 storage；未知布局在本次 typed 输出准备期明确拒绝。 */
        private fun captureSourceConstant(graph: CfirExportConstantGraph, owner: CfirVariable) {
            graph.nodes.forEach { node ->
                if (constants.containsKey(node.identity)) return@forEach
                val input = analysis.typeElement(node.type, owner)
                val storage = when (node) {
                    is CfirExportConstantNode.Primitive -> CjoFrozenConstantStorage.Scalar(sourceScalar(node, input.expandedType))
                    is CfirExportConstantNode.Array -> {
                        check(input.expandedType.arrayElementType != null) { "A SOURCE Array constant has no completed element type" }
                        CjoFrozenConstantStorage.Array
                    }
                    is CfirExportConstantNode.VArray -> {
                        val type = input.expandedType as? ConeVArrayType
                            ?: error("A SOURCE VArray constant has no completed VArray type")
                        check(type.size == node.elements.size.toLong()) { "A SOURCE VArray constant has the wrong element count" }
                        CjoFrozenConstantStorage.Array
                    }
                    is CfirExportConstantNode.Tuple -> error("SOURCE Tuple has no proven CJO storage representation")
                    is CfirExportConstantNode.Enum -> error("SOURCE enum value has no proven CJO discriminator/payload layout")
                    is CfirExportConstantNode.Object -> error("SOURCE object value has no proven CJO field layout")
                }
                constants[node.identity] = storage
                constantDomains[node.identity] = input.domain
            }
        }

        private fun sourceScalar(node: CfirExportConstantNode.Primitive, type: ConeCangJieType): CjoConstValueInfo {
            if (node.kind == CfirLiteralKind.STRING) {
                val text = node.value as? String ?: error("A SOURCE String storage has no completed text")
                check((type is ConeClassLikeType || type is ConeStructType) &&
                    (type as ConeLookupTagBasedType).classId == StdlibClassIds.String) {
                    "A SOURCE String storage requires its actual String type and completed text"
                }
                return CjoStringConstValue(text)
            }
            val kind = (type as? ConePrimitiveType)?.kind ?: error("A SOURCE scalar has no proven primitive storage type")
            if (node.kind == CfirLiteralKind.INT || node.kind == CfirLiteralKind.BYTE) {
                val value = node.value as? BigInteger ?: error("A SOURCE integer has no completed canonical integer value")
                fun signed(width: Int): Long {
                    val bound = BigInteger.ONE.shiftLeft(width - 1)
                    check(value >= bound.negate() && value < bound) { "SOURCE integer exceeds its actual signed storage width" }
                    return value.longValueExact()
                }
                fun unsigned(width: Int): ULong {
                    check(value.signum() >= 0 && value < BigInteger.ONE.shiftLeft(width)) { "SOURCE integer exceeds its actual unsigned storage width" }
                    return value.toString().toULong()
                }
                return when (kind) {
                    PrimitiveTypeKind.INT8 -> CjoInt8ConstValue(signed(8).toByte())
                    PrimitiveTypeKind.INT16 -> CjoInt16ConstValue(signed(16).toShort())
                    PrimitiveTypeKind.INT32 -> CjoInt32ConstValue(signed(32).toInt())
                    PrimitiveTypeKind.INT64 -> CjoInt64ConstValue(signed(64))
                    PrimitiveTypeKind.UINT8 -> CjoUInt8ConstValue(unsigned(8).toUByte())
                    PrimitiveTypeKind.UINT16 -> CjoUInt16ConstValue(unsigned(16).toUShort())
                    PrimitiveTypeKind.UINT32 -> CjoUInt32ConstValue(unsigned(32).toUInt())
                    PrimitiveTypeKind.UINT64 -> CjoUInt64ConstValue(unsigned(64))
                    else -> error("SOURCE scalar has no proven integer union representation")
                }
            }
            check(node.kind == CfirLiteralKind.FLOAT) { "SOURCE scalar kind has no proven ConstValue union tag" }
            return when (kind) {
                PrimitiveTypeKind.FLOAT64 -> CjoFloat64BitsConstValue((node.value as? Double
                    ?: error("SOURCE Float64 requires its completed binary64 value")).toRawBits())
                PrimitiveTypeKind.FLOAT32 -> {
                    val value = node.value as? Double ?: error("SOURCE Float32 has no canonical completed floating value")
                    val binary32 = value.toFloat()
                    check(!value.isNaN() && binary32.toDouble().toRawBits() == value.toRawBits()) {
                        "SOURCE Float32 has no exact completed binary32 representation"
                    }
                    val bits = binary32.toRawBits()
                    CjoFloat32BitsConstValue(bits)
                }
                PrimitiveTypeKind.FLOAT16 -> error("SOURCE Float16 has no completed binary16 storage bits")
                else -> error("SOURCE scalar has no proven floating union representation")
            }
        }

        private fun type(snapshot: CjoSourceExportSnapshot, reference: UInt): ConeCangJieType =
            snapshot.types.getValue(reference.toInt() - 1)

        private fun declaration(snapshot: CjoSourceExportSnapshot, reference: UInt): CfirDeclaration =
            snapshot.declarations.getValue(reference.toInt() - 1)

        private fun target(snapshot: CjoSourceExportSnapshot, reference: PackageFormat.FullId): CjoFrozenDeclarationTarget =
            when (reference.pkgId) {
                PackageIndex.INVALID.value -> CjoFrozenDeclarationTarget.Invalid
                PackageIndex.CURRENT.value -> CjoFrozenDeclarationTarget.Current(declaration(snapshot, reference.index))
                PackageIndex.PACKAGE_REFERENCE.value -> CjoFrozenDeclarationTarget.Package(checkNotNull(reference.decl), reference.index)
                else -> {
                    require(reference.pkgId >= 0 && reference.pkgId < snapshot.source.pkg.importsLength)
                    CjoFrozenDeclarationTarget.External(checkNotNull(snapshot.source.pkg.imports(reference.pkgId)), checkNotNull(reference.decl))
                }
            }

        private fun position(source: CjoLoadedPackage, saved: CjoSavedSourcePosition): CjoFrozenSourcePosition {
            val file = if (saved.pkgId == 0u && saved.file != 0u) {
                require(saved.file <= source.pkg.allFilesLength.toUInt())
                CjoSnapshotTableEntry(source, CjoSnapshotTableDomain.FILE, saved.file.toInt() - 1).also {
                    fileNames[it] = checkNotNull(source.pkg.allFiles(it.rawIndex))
                }
            } else null
            val importedPackage = if (saved.pkgId != 0u) {
                require(saved.pkgId <= source.pkg.importsLength.toUInt())
                checkNotNull(source.pkg.imports(saved.pkgId.toInt() - 1))
            } else null
            return CjoFrozenSourcePosition(file, importedPackage, saved.file, saved.line, saved.column, saved.ignore)
        }

        private fun position(source: CjoLoadedPackage, raw: PackageFormat.Position?): CjoFrozenSourcePosition? =
            raw?.let { position(source, CjoSavedSourcePosition(it.file, it.pkgId, it.line, it.column, it.ignore)) }

        private fun declaration(
            snapshot: CjoSourceExportSnapshot,
            declaration: CfirDeclaration,
            raw: PackageFormat.Decl,
            rawIndex: Int,
        ): CjoFrozenDeclarationFormat {
            val source = snapshot.source
            fun body(length: Int, get: (Int) -> UInt) = immutable((0 until length).map { declaration(snapshot, get(it)) })
            fun types(length: Int, get: (Int) -> UInt) = immutable((0 until length).map { type(snapshot, get(it)) })
            var function: CjoFrozenFunctionInfo? = null
            var nominal: CjoFrozenNominalInfo? = null
            var isVar = false
            var isConst = false
            var isMember = false
            var isNamed = false
            var isMutable = false
            var hasValue = false
            var hasInitializer = false
            var getters = emptyList<CfirDeclaration>()
            var setters = emptyList<CfirDeclaration>()
            var alias: ConeCangJieType? = null
            when (raw.infoType) {
                PackageFormat.DeclInfo.FuncInfo -> {
                    val info = raw.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo
                    val header = if (info.funcBody != null) checkNotNull(snapshot.headers[declaration]) else null
                    function = CjoFrozenFunctionInfo(info.op, info.funcBody != null, header, info.adInfo?.let(::autoDiff),
                        info.isConst, info.isInline, info.isFastNative, info.overflowPolicy)
                }
                PackageFormat.DeclInfo.ClassInfo -> {
                    val info = raw.info(PackageFormat.ClassInfo()) as PackageFormat.ClassInfo
                    nominal = CjoFrozenNominalInfo(types(info.inheritedTypesLength, info::inheritedTypes), body(info.bodyLength, info::body),
                        info.adInfo?.let(::autoDiff), info.isAnno, info.annoTargets, info.annoTargets2, info.runtimeVisible)
                }
                PackageFormat.DeclInfo.StructInfo -> {
                    val info = raw.info(PackageFormat.StructInfo()) as PackageFormat.StructInfo
                    nominal = CjoFrozenNominalInfo(types(info.inheritedTypesLength, info::inheritedTypes), body(info.bodyLength, info::body), info.adInfo?.let(::autoDiff))
                }
                PackageFormat.DeclInfo.InterfaceInfo -> {
                    val info = raw.info(PackageFormat.InterfaceInfo()) as PackageFormat.InterfaceInfo
                    nominal = CjoFrozenNominalInfo(types(info.inheritedTypesLength, info::inheritedTypes), body(info.bodyLength, info::body), null)
                }
                PackageFormat.DeclInfo.ExtendInfo -> {
                    val info = raw.info(PackageFormat.ExtendInfo()) as PackageFormat.ExtendInfo
                    nominal = CjoFrozenNominalInfo(types(info.inheritedTypesLength, info::inheritedTypes), body(info.bodyLength, info::body), null)
                }
                PackageFormat.DeclInfo.EnumInfo -> {
                    val info = raw.info(PackageFormat.EnumInfo()) as PackageFormat.EnumInfo
                    nominal = CjoFrozenNominalInfo(types(info.inheritedTypesLength, info::inheritedTypes), body(info.bodyLength, info::body),
                        info.adInfo?.let(::autoDiff), hasArguments = info.hasArguments, nonExhaustive = info.nonExhaustive,
                        ellipsis = position(source, info.ellipsisPos))
                }
                PackageFormat.DeclInfo.VarInfo -> {
                    val info = raw.info(PackageFormat.VarInfo()) as PackageFormat.VarInfo
                    isVar = info.isVar; isConst = info.isConst; isMember = info.isMemberParam
                    hasValue = info.valueType != PackageFormat.ConstValue.NONE
                    hasInitializer = info.initializer != 0u
                }
                PackageFormat.DeclInfo.VarWithPatternInfo -> {
                    val info = raw.info(PackageFormat.VarWithPatternInfo()) as PackageFormat.VarWithPatternInfo
                    isVar = info.isVar; isConst = info.isConst
                }
                PackageFormat.DeclInfo.ParamInfo -> {
                    val info = raw.info(PackageFormat.ParamInfo()) as PackageFormat.ParamInfo
                    isNamed = info.isNamedParam; isMember = info.isMemberParam
                }
                PackageFormat.DeclInfo.PropInfo -> {
                    val info = raw.info(PackageFormat.PropInfo()) as PackageFormat.PropInfo
                    isConst = info.isConst; isMutable = info.isMutable
                    getters = body(info.gettersLength, info::getters); setters = body(info.settersLength, info::setters)
                }
                PackageFormat.DeclInfo.AliasInfo -> alias = type(snapshot, (raw.info(PackageFormat.AliasInfo()) as PackageFormat.AliasInfo).aliasedTy)
            }
            val generic = raw.generic?.let { info ->
                CjoFrozenGeneric(immutable((0 until info.typeParametersLength).map { declaration(snapshot, info.typeParameters(it)) as CfirTypeParameter }),
                    immutable((0 until info.constraintsLength).map { ordinal ->
                        val constraint = checkNotNull(info.constraints(ordinal))
                        CjoFrozenConstraint(type(snapshot, constraint.type), types(constraint.uppersLength, constraint::uppers),
                            constraint.isImplicitlyIntroduced, position(source, constraint.begin), position(source, constraint.end))
                    }))
            }
            return CjoFrozenDeclarationFormat(source, CjoSnapshotTableEntry(source, CjoSnapshotTableDomain.DECLARATION, rawIndex),
                checkNotNull(raw.identifier), raw.kind, raw.infoType, raw.isTopLevel, raw.fullPkgName, raw.exportId, raw.mangledName,
                raw.mangledBeforeSema, raw.hash?.let { CjoDeclHashMetadata(it.instVar, it.virt, it.sig, it.srcUse, it.bodyHash) },
                raw.type.takeIf { it != 0u }?.let { type(snapshot, it) }, position(source, raw.begin), position(source, raw.end),
                position(source, raw.identifierPos), immutable(AttributePack(List(raw.attributesLength, raw::attributes)).toULongList()),
                raw.genericDecl?.let { target(snapshot, it) }, generic,
                immutable((0 until raw.dependenciesLength).map { target(snapshot, checkNotNull(raw.dependencies(it))) }),
                function, nominal, isVar, isConst, isMember, isNamed, isMutable, getters, setters, alias, hasValue,
                if (declaration is CfirValueParameter || declaration is CfirTypeParameter)
                    (analysis.ownerOf(declaration) as? CfirCallableDeclaration)?.isLocal == true
                else (declaration as? CfirCallableDeclaration)?.isLocal == true,
                (declaration as? CfirBuiltInDeclaration)?.kind, hasInitializer)
        }

        private fun autoDiff(info: PackageFormat.AutoDiffInfo): CjoAutoDiffInfo = CjoAutoDiffInfo(
            isDiff = info.isDiff, isAdjoint = info.isAdj, primal = info.primal,
            excepts = immutable((0 until info.exceptsLength).map { checkNotNull(info.excepts(it)) }),
            includes = immutable((0 until info.includesLength).map { checkNotNull(info.includes(it)) }),
            stage = info.stage,
        )

        private fun <T> immutable(values: Collection<T>): List<T> = Collections.unmodifiableList(values.toList())
        private fun <K, V> immutable(values: Map<K, V>): Map<K, V> =
            Collections.unmodifiableMap(if (values is IdentityHashMap) IdentityHashMap(values) else LinkedHashMap(values))
    }
}
