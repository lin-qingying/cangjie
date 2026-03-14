package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * 重载冲突解析器。
 *
 * 四轮消歧算法：
 * 1. filterOverrides — 去除被覆盖的方法（偏序关系）
 * 2. findMostSpecific — 逐参数位置比较子类型，选择更特定的候选
 * 3. discriminateGenerics — 非泛型优于泛型
 * 4. discriminateByDefaults — 少默认值参数的候选优先
 *
 * 对齐 K2 ConeOverloadConflictResolver。
 */
class CfirOverloadConflictResolver(
    private val subtypeChecker: ConeSubtypeChecker,
) : CfirCallConflictResolver() {

    override fun chooseMaximallySpecificCandidates(
        candidates: Set<CfirCandidate>,
    ): Set<CfirCandidate> {
        if (candidates.size <= 1) return candidates

        val signatures = candidates.map { CfirFlatSignature.create(it) }

        // 第 1 轮：按参数特化度选择
        val mostSpecific = findMostSpecific(signatures)
        if (mostSpecific.size <= 1) return mostSpecific.map { it.origin }.toSet()

        // 第 2 轮：非泛型优于泛型
        val afterGenerics = discriminateGenerics(mostSpecific)
        if (afterGenerics.size <= 1) return afterGenerics.map { it.origin }.toSet()

        // 第 3 轮：少默认值参数优先
        val afterDefaults = discriminateByDefaults(afterGenerics)
        return afterDefaults.map { it.origin }.toSet()
    }

    /**
     * 第 1 轮：找到最特定的候选集合。
     *
     * 逐参数位置比较，如果 A 的每个参数类型都是 B 的对应参数类型的子类型，
     * 则 A 比 B 更特定。去除所有被更特定候选覆盖的候选。
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
     * 判断 specific 是否比 general 更特定。
     *
     * 规则：specific 的每个参数类型是 general 对应参数类型的子类型，
     * 且至少有一个参数类型是严格子类型。
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

            if (!subtypeChecker.isSubtypeOf(specType, genType)) return false
            if (!subtypeChecker.isSubtypeOf(genType, specType)) {
                hasStrictSubtype = true
            }
        }

        return hasStrictSubtype
    }

    /**
     * 第 2 轮：非泛型优于泛型。
     *
     * 如果混合了泛型和非泛型候选，优先选择非泛型。
     */
    private fun discriminateGenerics(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val nonGeneric = signatures.filter { !it.isGeneric }
        return if (nonGeneric.isNotEmpty()) nonGeneric else signatures
    }

    /**
     * 第 3 轮：少默认值参数的候选优先。
     *
     * 在参数特化度和泛型/非泛型都无法区分时，
     * 选择使用更少默认值参数的候选（更精确匹配）。
     */
    private fun discriminateByDefaults(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val minDefaults = signatures.minOf { it.numDefaults }
        val fewestDefaults = signatures.filter { it.numDefaults == minDefaults }
        return fewestDefaults.ifEmpty { signatures }
    }
}
