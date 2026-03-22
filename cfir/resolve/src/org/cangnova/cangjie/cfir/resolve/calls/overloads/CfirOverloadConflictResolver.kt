package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

/**
 * 重载冲突解析器。
 * 当前采用三轮消歧策略：
 * 1. `findMostSpecific`：逐个参数比较特化程度，优先更具体的候选
 * 2. `discriminateGenerics`：非泛型优先于泛型
 * 3. `discriminateByDefaults`：使用默认值更少的候选优先
 * 对齐 K2 `ConeOverloadConflictResolver` 的核心思路。
 */
class CfirOverloadConflictResolver(
    private val typeRelations: CfirTypeRelations,
) : CfirCallConflictResolver() {

    override fun chooseMaximallySpecificCandidates(
        candidates: Set<CfirCandidate>,
    ): Set<CfirCandidate> {
        if (candidates.size <= 1) return candidates

        val signatures = candidates.map { CfirFlatSignature.create(it) }

        // 第 1 轮：按参数特化程度选择
        val mostSpecific = findMostSpecific(signatures)
        if (mostSpecific.size <= 1) return mostSpecific.map { it.origin }.toSet()

        // 第 2 轮：非泛型优先于泛型
        val afterGenerics = discriminateGenerics(mostSpecific)
        if (afterGenerics.size <= 1) return afterGenerics.map { it.origin }.toSet()

        // 第 3 轮：默认值更少的候选优先
        val afterDefaults = discriminateByDefaults(afterGenerics)
        if (afterDefaults.size <= 1) return afterDefaults.map { it.origin }.toSet()

        val afterQuestFallback = discriminateByQuestFallback(afterDefaults)
        if (afterQuestFallback.size <= 1) return afterQuestFallback.map { it.origin }.toSet()

        val afterIdealNumeric = discriminateByIdealNumericCompatibility(afterQuestFallback)
        if (afterIdealNumeric.size <= 1) return afterIdealNumeric.map { it.origin }.toSet()

        val afterExtendParticipation = discriminateByExtendParticipation(afterIdealNumeric)
        return afterExtendParticipation.map { it.origin }.toSet()
    }

    /**
     * 第 1 轮：找出最特定的候选集合。
     * 如果 A 的每个参数类型都是 B 对应参数类型的子类型，
     * 则认为 A 比 B 更特定，并去掉被覆盖的候选。
     */
    private fun findMostSpecific(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        if (signatures.size <= 1) return signatures

        val dominated = mutableSetOf<CfirFlatSignature>()

        for (i in signatures.indices) {
            if (signatures[i] in dominated) continue
            for (j in signatures.indices) {
                if (i == j || signatures[j] in dominated) continue
                if (isMoreSpecific(signatures[i], signatures[j])) {
                    dominated.add(signatures[j])
                }
            }
        }

        val result = signatures.filter { it !in dominated }
        return result.ifEmpty { signatures }
    }

    /**
     * 判断 `specific` 是否比 `general` 更特定。
     * 规则是：
     * - `specific` 的每个参数类型都要是 `general` 对应参数类型的子类型
     * - 且至少存在一个参数是严格子类型
     */
    private fun isMoreSpecific(specific: CfirFlatSignature, general: CfirFlatSignature): Boolean {
        val specificTypes = specific.valueParameterTypes
        val generalTypes = general.valueParameterTypes
        val commonSize = minOf(specificTypes.size, generalTypes.size)
        if (commonSize == 0) return false

        var hasStrictSubtype = false

        for (i in 0 until commonSize) {
            val specType = specificTypes[i] ?: return false
            val genType = generalTypes[i] ?: return false

            if (!typeRelations.isSubtype(specType, genType)) return false
            if (!typeRelations.isSubtype(genType, specType)) {
                hasStrictSubtype = true
            }
        }

        return hasStrictSubtype
    }

    /**
     * 第 2 轮：非泛型优先于泛型。
     */
    private fun discriminateGenerics(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val nonGeneric = signatures.filter { !it.isGeneric }
        return if (nonGeneric.isNotEmpty()) nonGeneric else signatures
    }

    /**
     * 第 3 轮：默认值更少的候选优先。
     * 当前两轮仍无法区分时，优先选择更“精确匹配”的候选。
     */
    private fun discriminateByDefaults(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val minDefaults = signatures.minOf { it.numDefaults }
        val fewestDefaults = signatures.filter { it.numDefaults == minDefaults }
        return fewestDefaults.ifEmpty { signatures }
    }

    private fun discriminateByQuestFallback(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val withoutFallback = signatures.filter { !it.usedQuestFallback }
        return if (withoutFallback.isNotEmpty()) withoutFallback else signatures
    }

    private fun discriminateByIdealNumericCompatibility(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val withoutIdealNumeric = signatures.filter { !it.usedIdealNumericCompatibility }
        return if (withoutIdealNumeric.isNotEmpty()) withoutIdealNumeric else signatures
    }

    private fun discriminateByExtendParticipation(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val withoutExtend = signatures.filter { !it.usedExtendParticipation }
        return if (withoutExtend.isNotEmpty()) withoutExtend else signatures
    }
}

