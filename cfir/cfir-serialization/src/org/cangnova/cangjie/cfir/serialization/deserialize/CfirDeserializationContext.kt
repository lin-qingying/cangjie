package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.Package
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.resolve.providers.CfirPrimitiveDeclarationFactory
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.provider.CfirClassLikeSymbolLookup
import org.cangnova.cangjie.cfir.serialization.provider.CjoPackageContextProvider
import org.cangnova.cangjie.cfir.session.implicitSystemAnnotations
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import java.util.concurrent.ConcurrentHashMap

/**
 * 反序列化上下文。
 *
 * 该对象承载当前包数据、跨包加载能力以及面向 `FullId` 的统一解析入口。
 */
class CfirDeserializationContext(
    /** 当前反序列化的 FlatBuffers Package。 */
    pkg: Package,
    /** 当前包头。 */
    val header: CjoPackageHeader,
    /** 库模块元数据。 */
    val moduleData: CfirModuleData,
    /** `.cjo` 包管理器，用于跨包声明索引和包头装载。 */
    cjoManager: CjoManager,
    /** 反序列化期间的 class-like 符号查找入口，只读 `.cjo` 头部与 flatbuffer，不得进 PSI。 */
    classSymbolLookup: CfirClassLikeSymbolLookup,
    /** 实际读取 CJO 的路径；内存/旧调用方默认不加载 sidecar。 */
    val sourcePath: java.nio.file.Path? = null,
    /** 从 session-owned 平台配置读取隐式系统注解；绝不从 sourcePath 推断。 */
    implicitSystemAnnotations: Set<org.cangnova.cangjie.name.FqName> =
        moduleData.session.implicitSystemAnnotations,
    val mode: CjoDeserializationMode = CjoDeserializationMode.FULL,
    val binding: org.cangnova.cangjie.cfir.serialization.provider.CjoDeserializationBinding =
        (classSymbolLookup as? org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider)
            ?.deserializationBinding ?: cjoManager.deserializedSymbolLookups.bindingForModule(moduleData),
    /** 导入必须进入实际目标模块的 metadata provider；独立 reader 使用本 manager 的模块入口。 */
    packageContextProvider: CjoPackageContextProvider = binding,
    /** 实际 snapshot 或明确的内存 Package 身份，withMode 保留同一对象。 */
    val snapshotIdentity: Any = binding.publishedSnapshot(header, sourcePath) ?: pkg,
) {
    private val poolAuthority = pkg
    /** actual 输入只授权已发布 wire pool；根视图由该 snapshot 提供，不能由 caller 另指 bb_pos。 */
    val pkg: Package = if (snapshotIdentity is org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage)
        snapshotIdentity.pkg else pkg

    private val lookupAuthority = classSymbolLookup
    private val sourceManager = cjoManager

    /** 实际 lookup 由当前 context 绑定；ctor 的 named lookup 参数只验证原 producer authority。 */
    val classSymbolLookup: CfirClassLikeSymbolLookup = CjoSnapshotClassSymbolLookup(this)
    val packageContextProvider: CjoPackageContextProvider = binding.contextRouter(packageContextProvider)
    val cjoManager: CjoManager = binding.dependencyManager
    val implicitSystemAnnotations: Set<org.cangnova.cangjie.name.FqName> = implicitSystemAnnotations.toSet()

    /** 保留原完整位置参数构造入口；装配仍必须来自原 metadata 服务。 */
    constructor(
        pkg: Package, header: CjoPackageHeader, moduleData: CfirModuleData,
        cjoManager: CjoManager, classSymbolLookup: CfirClassLikeSymbolLookup,
        sourcePath: java.nio.file.Path?, implicitSystemAnnotations: Set<org.cangnova.cangjie.name.FqName>,
        mode: CjoDeserializationMode, legacyPackageContextProvider: CjoPackageContextProvider,
    ) : this(
        pkg = pkg, header = header, moduleData = moduleData, cjoManager = cjoManager,
        classSymbolLookup = classSymbolLookup, sourcePath = sourcePath,
        implicitSystemAnnotations = implicitSystemAnnotations, mode = mode,
        packageContextProvider = legacyPackageContextProvider,
    )

    init {
        require(sourceManager === binding.ownManager || sourceManager === binding.dependencyManager)
        binding.requireLookupAuthority(lookupAuthority)
        requireAuthority()
    }

    /** 先验证原服务和实际 pool/source，再允许 canonical publication；先到的 reader 没有重配权。 */
    internal fun requireAuthority() {
        binding.requireSessionAuthority(moduleData)
        require(moduleData in binding.moduleDataProvider.allModuleData)
        require(packageContextProvider === binding && cjoManager === binding.dependencyManager &&
            implicitSystemAnnotations == binding.implicitSystemAnnotations &&
            classSymbolLookup is CjoSnapshotClassSymbolLookup && classSymbolLookup.owns(this)) {
            "CJO context services differ from their original producer"
        }
        if (sourcePath != null) {
            require(binding.moduleDataProvider.getModuleData(sourcePath) === moduleData) {
                "CJO context source has another module owner"
            }
            val snapshot = checkNotNull(binding.publishedSnapshot(header, sourcePath)) {
                "CJO actual context has no published source snapshot"
            }
            require(snapshotIdentity === snapshot) { "CJO actual context has another snapshot identity" }
            val sourceBuffer = snapshot.pkg.byteBuffer
            val contextBuffer = poolAuthority.byteBuffer
            require(sourceBuffer.hasArray() && contextBuffer.hasArray() &&
                sourceBuffer.array() === contextBuffer.array() &&
                sourceBuffer.arrayOffset() == contextBuffer.arrayOffset()) {
                "CJO actual context does not contain its published wire pool"
            }
        } else {
            require(snapshotIdentity === pkg) { "CJO memory context must retain its explicit Package identity" }
        }
        require(header.fullPkgName == poolAuthority.fullPkgName && header.moduleName == poolAuthority.moduleName.orEmpty() &&
            header.fullPkgName == pkg.fullPkgName && header.moduleName == pkg.moduleName.orEmpty()) {
            "CJO context header does not describe its actual Package"
        }
    }

    /** 跨图入口在进入 target materialization 前统一核验 mode、来源 authority 和事务。 */
    internal fun validatedTarget(
        sourceBinding: org.cangnova.cangjie.cfir.serialization.provider.CjoDeserializationBinding,
        packageName: String,
        requestedMode: CjoDeserializationMode,
    ): CfirDeserializationContext {
        require(header.fullPkgName == packageName && mode == requestedMode) {
            "CJO import target has a different package or deserialization mode"
        }
        require(materialization === sourceBinding.dependencyManager.declarationMaterialization) {
            "CJO import target belongs to another metadata graph transaction"
        }
        requireAuthority()
        return canonical()
    }

    internal fun packageContextOrNull(packageName: String): CfirDeserializationContext? =
        packageContextProvider.getPackageContext(packageName, mode)?.validatedTarget(binding, packageName, mode)

    /**
     * 反序列化必须消费所属 session 的同一份语言设置；不能为 CJO 重新创建默认设置。
     */
    val languageVersionSettings: LanguageVersionSettings
        get() = binding.languageVersionSettings

    /** 在首次声明物化前完成快照与匹配计划；整代 context/provider/session 重建才会重新读取。 */
    val sidecar: org.cangnova.cangjie.cfir.serialization.cjd.CjdBinaryAnnotationOverlay? by lazy {
        sourcePath?.let { org.cangnova.cangjie.cfir.serialization.cjd.CjdBinaryAnnotationOverlay.load(this, it) }
    }
    /** `allTypes` 索引 -> 已反序列化类型。 */
    val typeCache = ConcurrentHashMap<Int, ConeCangJieType>()

    /** `allDecls` 索引 -> 已反序列化声明。 */
    val declCache = ConcurrentHashMap<Int, CfirDeserializedDeclaration>()

    internal val packageIndex: CjoPackageIndex by lazy { CjoPackageIndex(header, pkg) }
    internal val declarationEntries = mutableMapOf<Int, CjoDeclarationCacheEntry>()
    /** 当前 module/snapshot/mode 的 Lambda wire index 对应唯一匿名函数；不跨读取模式共用。 */
    internal val anonymousFunctionEntries = mutableMapOf<Int, CjoAnonymousFunctionCacheEntry>()
    internal val materialization: CjoDeclarationMaterialization
        get() = binding.dependencyManager.deserializedSymbolLookups.materialization()

    internal fun canonical(): CfirDeserializationContext = binding.dependencyManager.deserializedSymbolLookups.canonical(this)

    /** 同包不同模式独立物化；不允许用 FULL 的结果补 signature-only 缓存。 */
    internal fun withMode(requested: CjoDeserializationMode): CfirDeserializationContext {
        if (mode == requested) return canonical()
        val changed = CfirDeserializationContext(
            pkg = pkg, header = header, moduleData = moduleData, cjoManager = cjoManager,
            classSymbolLookup = binding.dependencyManager.deserializedSymbolLookups.forBinding(binding, moduleData, requested),
            sourcePath = sourcePath, implicitSystemAnnotations = implicitSystemAnnotations,
            mode = requested, binding = binding, snapshotIdentity = snapshotIdentity,
            packageContextProvider = packageContextProvider,
        )
        return changed.canonical()
    }

    /** 导入使用目标 provider 的 manager、module、语言设置与模式，不创建 consumer 的目标副本。 */
    internal fun importedContext(packageName: String): CfirDeserializationContext =
        checkNotNull(packageContextOrNull(packageName)) {
            "Cannot load CJO FullId target package $packageName"
        }.canonical()

    /** ClassId 只用于目标包内查询；目标模块由 metadata provider 先确定，禁止 session/PSI 查询。 */
    internal fun lookupClassLikeSymbol(classId: org.cangnova.cangjie.name.ClassId):
        org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>? {
        return classSymbolLookup.getClassLikeSymbolByClassId(classId)
    }

    /** primitive 没有普通 CJO Decl；其 module/session 由实际 std.core 元数据 owner 决定。 */
    internal fun lookupPrimitiveTypeSymbol(kind: PrimitiveTypeKind): CfirPrimitiveTypeSymbol {
        check(kind.isExposedBuiltinClassifier) { "CJO primitive qualifier has a non-language type" }
        val coreName = StandardNames.FqNames.core.asString()
        val owner = if (header.fullPkgName == coreName) this else importedContext(coreName)
        return CfirPrimitiveDeclarationFactory.getInstance(owner.moduleData.session, owner.moduleData)
            .getDeclaration(kind).symbol
    }

    internal fun headerRecordOrNull(index: Int): CfirDeserializedDeclaration? = when (val entry = declarationEntries[index]) {
        is CjoDeclarationCacheEntry.Header -> entry.record
        is CjoDeclarationCacheEntry.Ready -> entry.record
        is CjoDeclarationCacheEntry.Failed -> throw entry.failure
        else -> null
    }

    /** 导入包索引 -> 同次取得的 target context/pool 索引；物化不再按包名重新选目标。 */
    internal val importedPackageIndices = ConcurrentHashMap<Int, CjoImportedPackage>()

    /** FullId 统一解析器。 */
    internal val fullIdResolver: CjoFullIdResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CjoFullIdResolver(this)
    }

    /**
     * `CfirDeclDeserializer` / `CfirTypeDeserializer` 都带有调用栈态，
     * 必须按次创建，不能像 Kotlin 的无栈 member deserializer 那样直接复用实例。
     */
    internal fun createTypeDeserializer(): CfirTypeDeserializer = CfirTypeDeserializer(this)

    /** 创建一次性的声明反序列化器，并配套创建同上下文的类型反序列化器。 */
    internal fun createDeclDeserializer(): CfirDeclDeserializer =
        CfirDeclDeserializer(this, createTypeDeserializer())

}
