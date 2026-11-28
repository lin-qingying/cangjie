package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.extensionSignatureKey
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.cfir.types.extendLookupKinds
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import java.util.concurrent.ConcurrentHashMap

/**
 * 物化 extend 的 provider：二进制 extend 若有对应 decompiled PSI，则以带 PSI 的物化声明对外提供。
 *
 * 索引在**二进制声明**上建立（便宜），查询命中后才惰性物化；`getAllExtends` 不物化（统计/全量
 * 遍历场景物化 505 个 std extend 成本过高）。找不到 PSI 或物化失败时保留原二进制声明。
 */
internal class LLPsiBackedExtendProvider(
    private val binaryProvider: CfirExtendProvider,
    private val materialize: (CfirExtend) -> CfirExtend?,
) : CfirExtendProvider {
    /** 二进制声明（去重、含所属包），不触发物化。 */
    private val binaries: List<Pair<CfirExtend, FqName?>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val seen = mutableSetOf<String>()
        binaryProvider.getAllExtends().mapNotNull { binary ->
            val packageFqName = binaryProvider.getPackageFqName(binary)
            if (binary.source != null) {
                binary to packageFqName
            } else {
                // 只对二进制来源按完整结构键去重；源码 extend 原样保留。
                if (seen.add(LLExtendStructuralKeys.of(binary))) binary to packageFqName else null
            }
        }
    }

    /** 二进制声明上的查询索引。 */
    private val index: Index by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val byTargetKey = linkedMapOf<CfirExtendTargetKey, MutableList<CfirExtend>>()
        val byPackage = linkedMapOf<FqName, MutableList<CfirExtend>>()
        val byCallableSymbol = linkedMapOf<CfirCallableSymbol<*>, CfirExtend>()
        val byDeclarationPackage = linkedMapOf<CfirExtend, FqName>()
        val byStructuralKey = linkedMapOf<String, CfirExtend>()
        binaries.forEach { (extend, packageFqName) ->
            if (packageFqName != null) {
                byPackage.getOrPut(packageFqName) { mutableListOf() }.add(extend)
                byDeclarationPackage[extend] = packageFqName
            }
            byStructuralKey.putIfAbsent(extend.extensionSignatureKey(), extend)
            extend.extendedTypeRef.coneTypeOrNull?.expandedExtendTargetKey?.let { targetKey ->
                byTargetKey.getOrPut(targetKey) { mutableListOf() }.add(extend)
            }
            extend.declarations.forEach { declaration ->
                val callable = declaration as? CfirCallableDeclaration ?: return@forEach
                byCallableSymbol[callable.symbol] = extend
            }
        }
        Index(
            byTargetKey = byTargetKey.mapValues { (_, values) -> values.distinct() },
            byPackage = byPackage.mapValues { (_, values) -> values.distinct() },
            byCallableSymbol = byCallableSymbol,
            byDeclarationPackage = byDeclarationPackage,
            byStructuralKey = byStructuralKey,
        )
    }

    /** 二进制声明的物化结果缓存。 */
    private val effectiveCache = ConcurrentHashMap<CfirExtend, CfirExtend>()

    /** 物化成员 symbol -> 物化 extend。 */
    private val materializedCallableOwners = ConcurrentHashMap<CfirCallableSymbol<*>, CfirExtend>()

    /** 物化 extend -> 所属包（物化声明不在二进制索引里）。 */
    private val materializedPackages = ConcurrentHashMap<CfirExtend, FqName>()

    /** 按需物化单条二进制 extend（结果与包、成员归属一起缓存）。 */
    private fun effective(binary: CfirExtend): CfirExtend {
        effectiveCache[binary]?.let { return it }
        val value = if (binary.source != null) {
            binary
        } else {
            materialize(binary)?.also { materializedExtend ->
                binaryProvider.getPackageFqName(binary)?.let { packageFqName ->
                    materializedPackages[materializedExtend] = packageFqName
                }
                materializedExtend.declarations.forEach { declaration ->
                    (declaration as? CfirCallableDeclaration)?.let { callable ->
                        materializedCallableOwners[callable.symbol] = materializedExtend
                    }
                }
            } ?: binary
        }
        effectiveCache[binary] = value
        return value
    }

    /**
     * 批量枚举不物化：该入口主要服务于统计/全量遍历，物化 505 个 std extend 成本过高；
     * 成员解析/导航走按目标的查询，那里才按需物化。
     */
    override fun getAllExtends(): List<CfirExtend> = binaries.map { (extend, _) -> extend }

    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
        index.byTargetKey[targetKey].orEmpty().map(::effective)

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        getExtendsForTarget(CfirExtendTargetKey.ClassLike(classId))

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        index.byPackage[packageFqName].orEmpty().map(::effective)

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
        kind.extendLookupKinds.flatMap { lookupKind -> getExtendsForClass(lookupKind.classId) }.distinct()

    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
        materializedCallableOwners[symbol] ?: binaryProvider.getContainingExtend(symbol)

    override fun getPackageFqName(extend: CfirExtend): FqName? =
        index.byDeclarationPackage[extend] ?: materializedPackages[extend] ?: binaryProvider.getPackageFqName(extend)

    override fun getContainingFile(extend: CfirExtend): CfirFile? =
        binaryProvider.getContainingFile(extend)

    override fun getExtendByStructuralKey(key: String): CfirExtend? =
        index.byStructuralKey[key]?.let(::effective) ?: binaryProvider.getExtendByStructuralKey(key)

    private data class Index(
        val byTargetKey: Map<CfirExtendTargetKey, List<CfirExtend>>,
        val byPackage: Map<FqName, List<CfirExtend>>,
        val byCallableSymbol: Map<CfirCallableSymbol<*>, CfirExtend>,
        val byDeclarationPackage: Map<CfirExtend, FqName>,
        val byStructuralKey: Map<String, CfirExtend>,
    )
}
