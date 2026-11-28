package org.cangnova.cangjie.chir.cjmp

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113FunctionSourceIdentity
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113NominalTypeIdentity

/** 一条由 CFIR matcher 接受的 common/specific 声明关系。 */
data class CjmpChirDeclarationPair(
    val commonIdentity: ChirSemanticId,
    val specificIdentity: ChirSemanticId,
)

/**
 * 一条由 CFIR matcher 接受的类型参数关系。
 *
 * owner 身份与局部参数序号共同定位两侧 CHIR 声明中的实际泛型身份；参数名和序号本身不构成匹配依据。
 */
data class CjmpChirTypeParameterPair(
    val commonOwnerIdentity: ChirSemanticId,
    val commonIndex: Int,
    val specificOwnerIdentity: ChirSemanticId,
    val specificIndex: Int,
) {
    init {
        require(commonIndex >= 0) { "common type parameter index must not be negative" }
        require(specificIndex >= 0) { "specific type parameter index must not be negative" }
    }
}

/**
 * CHIR actualization 所需的、由 CFIR matcher 冻结的声明及泛型参数关系。
 *
 * 该计划只承载授权事实。CHIR 泛型身份仍由两侧已转换声明提供，linker 不按名称或同序位置推断关系。
 */
data class CjmpChirActualizationPlan(
    val declarationPairs: Set<CjmpChirDeclarationPair>,
    val typeParameterPairs: Set<CjmpChirTypeParameterPair> = emptySet(),
    /** common CJO matcher 身份到 RAW CHIR 名义定义源码身份的显式桥接事实。 */
    val commonNominalTypeIdentities: Map<ChirSemanticId, OfficialChirV113NominalTypeIdentity> = emptyMap(),
    /** common CJO 泛型 callable 的原始 mangling 到 RAW CHIR 函数身份的显式桥接事实。 */
    val commonFunctionSourceIdentities: Map<ChirSemanticId, OfficialChirV113FunctionSourceIdentity> = emptyMap(),
) {
    /** 兼容只有稳定共享身份的既有调用方。 */
    constructor(actualizedIdentities: Set<ChirSemanticId>) : this(
        declarationPairs = actualizedIdentities.mapTo(linkedSetOf()) { identity ->
            CjmpChirDeclarationPair(identity, identity)
        },
    )
}

/**
 * 将新计划降为旧 actualizer 契约可表达的共同身份集合。
 *
 * 旧实现不能表达跨身份映射或泛型参数映射；遇到这类计划时必须显式拒绝，避免静默丢失 matcher 事实。
 */
internal fun CjmpChirActualizationPlan.asLegacyActualizedIdentities(): Set<ChirSemanticId> {
    if (typeParameterPairs.isNotEmpty()) {
        throw UnsupportedOperationException("Legacy CHIR actualizers cannot consume matcher-approved type parameter mappings")
    }
    val crossIdentityPair = declarationPairs.firstOrNull { it.commonIdentity != it.specificIdentity }
    if (crossIdentityPair != null) {
        throw UnsupportedOperationException(
            "Legacy CHIR actualizers cannot consume cross-package identity pair " +
                "${crossIdentityPair.commonIdentity} -> ${crossIdentityPair.specificIdentity}",
        )
    }
    return declarationPairs.mapTo(linkedSetOf()) { it.commonIdentity }
}

/** 旧 linker 也无法在导入阶段解析 CJO 源身份到 RAW CHIR 身份。 */
internal fun CjmpChirActualizationPlan.asLegacyLinkerIdentities(): Set<ChirSemanticId> {
    if (commonNominalTypeIdentities.isNotEmpty() || commonFunctionSourceIdentities.isNotEmpty()) {
        throw UnsupportedOperationException("Legacy CHIR linkers cannot resolve official CJO source identities")
    }
    return asLegacyActualizedIdentities()
}
