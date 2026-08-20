package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name

/**
 * callable 从结构成员图到达使用点时保留的来源身份。
 *
 * 普通 declared/import/package 候选使用 [None]。由 extend 直接声明的成员记录
 * [sourceExtend]；由 extend 注入接口后继承到接收者的成员还记录最初的超类型边和
 * 当前 requirement 所属的实例化接口类型。可见性层据此判断具体导出依据，禁止从最终
 * symbol 的 containing declaration 反推已丢失的 extend 来源。
 */
data class CfirCallableLookupProvenance(
    /** 贡献该成员或接口父边的 extend 声明。 */
    val sourceExtend: CfirExtend?,
    /** 最初把接口注入目标类型、且已按 receiver 实例化的来源父边。 */
    val sourceSupertypeDescriptor: CfirInstantiatedSupertypeDescriptor?,
    /** 当前继承成员 requirement 所属的实例化接口类型。 */
    val requirementInterfaceType: ConeCangJieType?,
) {
    init {
        val descriptorExtend = (sourceSupertypeDescriptor?.origin as? CfirInstantiatedSupertypeOrigin.Extend)
            ?.sourceExtend
        require(descriptorExtend == null || descriptorExtend === sourceExtend) {
            "Callable lookup provenance has inconsistent extend owners"
        }
        require(requirementInterfaceType == null || sourceExtend != null) {
            "Requirement interface provenance requires an extend owner"
        }
    }

    companion object {
        /** 不经过 extend 接口边的普通结构候选。 */
        val None: CfirCallableLookupProvenance = CfirCallableLookupProvenance(
            sourceExtend = null,
            sourceSupertypeDescriptor = null,
            requirementInterfaceType = null,
        )

        /** extend 体内直接声明的成员。 */
        fun directExtendMember(extend: CfirExtend): CfirCallableLookupProvenance =
            CfirCallableLookupProvenance(
                sourceExtend = extend,
                sourceSupertypeDescriptor = null,
                requirementInterfaceType = null,
            )

        /** 由 extend 注入接口后沿继承图到达的 requirement/default member。 */
        fun inheritedThroughExtend(
            descriptor: CfirInstantiatedSupertypeDescriptor,
        ): CfirCallableLookupProvenance {
            val origin = descriptor.origin as? CfirInstantiatedSupertypeOrigin.Extend
                ?: error("Extend inheritance provenance requires an extend descriptor: $descriptor")
            return CfirCallableLookupProvenance(
                sourceExtend = origin.sourceExtend,
                sourceSupertypeDescriptor = descriptor,
                requirementInterfaceType = descriptor.type,
            )
        }
    }
}

/** callable symbol 与其结构来源组成的不可分候选。 */
data class CfirCallableWithLookupProvenance(
    val symbol: CfirCallableSymbol<*>,
    val provenance: CfirCallableLookupProvenance,
)

/**
 * 能够在普通成员查询之外保留 callable 结构来源的 scope。
 *
 * 该接口不做访问控制；它只让 tower、override 和 declaration checker 消费同一份
 * effective member graph，而不是各自重走父类型/extend 图。
 */
interface CfirCallableLookupProvenanceScope {
    fun processCallablesByNameWithLookupProvenance(
        name: Name,
        processor: (CfirCallableWithLookupProvenance) -> Unit,
    )
}

/** 对不提供来源能力的普通 scope 生成显式 [CfirCallableLookupProvenance.None]。 */
fun CfirScope.processCallablesByNameWithLookupProvenance(
    name: Name,
    processor: (CfirCallableWithLookupProvenance) -> Unit,
) {
    val provenanceScope = this as? CfirCallableLookupProvenanceScope
    if (provenanceScope != null) {
        provenanceScope.processCallablesByNameWithLookupProvenance(name, processor)
        return
    }
    processCallablesByName(name) { symbol ->
        processor(CfirCallableWithLookupProvenance(symbol, CfirCallableLookupProvenance.None))
    }
}
