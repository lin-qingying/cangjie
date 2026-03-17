package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * 閲嶈浇鍐茬獊瑙ｆ瀽鍣ㄣ€? *
 * 鍥涜疆娑堟绠楁硶锛? * 1. filterOverrides 鈥?鍘婚櫎琚鐩栫殑鏂规硶锛堝亸搴忓叧绯伙級
 * 2. findMostSpecific 鈥?閫愬弬鏁颁綅缃瘮杈冨瓙绫诲瀷锛岄€夋嫨鏇寸壒瀹氱殑鍊欓€? * 3. discriminateGenerics 鈥?闈炴硾鍨嬩紭浜庢硾鍨? * 4. discriminateByDefaults 鈥?灏戦粯璁ゅ€煎弬鏁扮殑鍊欓€変紭鍏? *
 * 瀵归綈 K2 ConeOverloadConflictResolver銆? */
class CfirOverloadConflictResolver(
    private val subtypeChecker: ConeSubtypeChecker,
) : CfirCallConflictResolver() {

    override fun chooseMaximallySpecificCandidates(
        candidates: Set<CfirCandidate>,
    ): Set<CfirCandidate> {
        if (candidates.size <= 1) return candidates

        val signatures = candidates.map { CfirFlatSignature.create(it) }

        // 绗?1 杞細鎸夊弬鏁扮壒鍖栧害閫夋嫨
        val mostSpecific = findMostSpecific(signatures)
        if (mostSpecific.size <= 1) return mostSpecific.map { it.origin }.toSet()

        // 绗?2 杞細闈炴硾鍨嬩紭浜庢硾鍨?
        val afterGenerics = discriminateGenerics(mostSpecific)
        if (afterGenerics.size <= 1) return afterGenerics.map { it.origin }.toSet()

        // 绗?3 杞細灏戦粯璁ゅ€煎弬鏁颁紭鍏?
        val afterDefaults = discriminateByDefaults(afterGenerics)
        return afterDefaults.map { it.origin }.toSet()
    }

    /**
     * 绗?1 杞細鎵惧埌鏈€鐗瑰畾鐨勫€欓€夐泦鍚堛€?     *
     * 閫愬弬鏁颁綅缃瘮杈冿紝濡傛灉 A 鐨勬瘡涓弬鏁扮被鍨嬮兘鏄?B 鐨勫搴斿弬鏁扮被鍨嬬殑瀛愮被鍨嬶紝
     * 鍒?A 姣?B 鏇寸壒瀹氥€傚幓闄ゆ墍鏈夎鏇寸壒瀹氬€欓€夎鐩栫殑鍊欓€夈€?     */
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
     * 鍒ゆ柇 specific 鏄惁姣?general 鏇寸壒瀹氥€?     *
     * 瑙勫垯锛歴pecific 鐨勬瘡涓弬鏁扮被鍨嬫槸 general 瀵瑰簲鍙傛暟绫诲瀷鐨勫瓙绫诲瀷锛?     * 涓旇嚦灏戞湁涓€涓弬鏁扮被鍨嬫槸涓ユ牸瀛愮被鍨嬨€?     */
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
     * 绗?2 杞細闈炴硾鍨嬩紭浜庢硾鍨嬨€?     *
     * 濡傛灉娣峰悎浜嗘硾鍨嬪拰闈炴硾鍨嬪€欓€夛紝浼樺厛閫夋嫨闈炴硾鍨嬨€?     */
    private fun discriminateGenerics(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val nonGeneric = signatures.filter { !it.isGeneric }
        return if (nonGeneric.isNotEmpty()) nonGeneric else signatures
    }

    /**
     * 绗?3 杞細灏戦粯璁ゅ€煎弬鏁扮殑鍊欓€変紭鍏堛€?     *
     * 鍦ㄥ弬鏁扮壒鍖栧害鍜屾硾鍨?闈炴硾鍨嬮兘鏃犳硶鍖哄垎鏃讹紝
     * 閫夋嫨浣跨敤鏇村皯榛樿鍊煎弬鏁扮殑鍊欓€夛紙鏇寸簿纭尮閰嶏級銆?     */
    private fun discriminateByDefaults(signatures: List<CfirFlatSignature>): List<CfirFlatSignature> {
        val minDefaults = signatures.minOf { it.numDefaults }
        val fewestDefaults = signatures.filter { it.numDefaults == minDefaults }
        return fewestDefaults.ifEmpty { signatures }
    }
}

