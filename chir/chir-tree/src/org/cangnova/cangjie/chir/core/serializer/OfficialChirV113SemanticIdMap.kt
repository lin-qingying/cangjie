package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import java.util.Collections

/**
 * 为一个官方 v1.1.3 包中的四个顶层 ID 域建立稳定且互不混淆的仓内身份。
 *
 * 注册阶段先收集所有 TYPE、VALUE、EXPRESSION 与 DEFINITION 身份；随后转换阶段才能解析跨域前向引用。
 * 数字只在各自域内有意义，因此仓内身份始终同时包含包身份、域和官方 ID。
 */
class OfficialChirV113SemanticIdMap(
    private val packageSemanticId: ChirSemanticId,
    semanticPackage: OfficialChirV113SemanticPackage,
) {
    private val identitiesByDomain: Map<OfficialChirV113TableDomain, Map<UInt, ChirSemanticId>> =
        buildMap {
            register(packageSemanticId, OfficialChirV113TableDomain.TYPE, semanticPackage.types)
            register(packageSemanticId, OfficialChirV113TableDomain.VALUE, semanticPackage.values)
            register(packageSemanticId, OfficialChirV113TableDomain.EXPRESSION, semanticPackage.expressions)
            register(packageSemanticId, OfficialChirV113TableDomain.DEFINITION, semanticPackage.definitions)
        }

    /** 返回指定官方表身份对应的仓内语义身份；域或编号缺失时拒绝转换。 */
    fun idOf(domain: OfficialChirV113TableDomain, officialId: UInt): ChirSemanticId =
        identitiesByDomain[domain]?.get(officialId)
            ?: throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 reference $domain#$officialId is absent from the registered package graph",
            )

    /** 按引用自身声明的域解析身份，禁止把相同数字从另一个域误解析。 */
    fun idOf(reference: OfficialChirV113SemanticValue.Reference): ChirSemanticId {
        if (reference.id !in 1..UInt.MAX_VALUE.toLong()) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 ${reference.domain} reference ${reference.id} is outside its ID domain",
            )
        }
        return idOf(reference.domain, reference.id.toUInt())
    }

    /** 为转换过程中新增的仓内节点分配与官方身份同域、同包且不可碰撞的身份。 */
    fun syntheticId(domain: OfficialChirV113TableDomain, discriminator: String): ChirSemanticId {
        require(discriminator.isNotBlank()) { "synthetic CHIR identity discriminator must not be blank" }
        return ChirSemanticId(
            "official:v113:" + packageSemanticId.value + ":" + domain.name.lowercase() + ":synthetic:" + discriminator,
        )
    }

    /** 返回四个域的只读身份视图，供完整图转换与引用闭包审计使用。 */
    fun allIdentities(): Map<OfficialChirV113TableDomain, Map<UInt, ChirSemanticId>> = identitiesByDomain

    private fun MutableMap<OfficialChirV113TableDomain, Map<UInt, ChirSemanticId>>.register(
        packageSemanticId: ChirSemanticId,
        domain: OfficialChirV113TableDomain,
        indexedTables: List<OfficialChirV113IndexedTable>,
    ) {
        val identities = LinkedHashMap<UInt, ChirSemanticId>(indexedTables.size)
        indexedTables.forEach { indexed ->
            if (indexed.domain != domain || indexed.id == 0u) {
                throw OfficialChirV113FormatException(
                    "Official CHIR v1.1.3 ${indexed.domain}#${indexed.id} is in the wrong top-level ID domain",
                )
            }
            val identity = ChirSemanticId(
                "official:v113:" + packageSemanticId.value + ":" + domain.name.lowercase() + ":" + indexed.id,
            )
            if (identities.put(indexed.id, identity) != null) {
                throw OfficialChirV113FormatException(
                    "Official CHIR v1.1.3 $domain contains duplicate top-level ID ${indexed.id}",
                )
            }
        }
        put(domain, Collections.unmodifiableMap(identities))
    }
}
