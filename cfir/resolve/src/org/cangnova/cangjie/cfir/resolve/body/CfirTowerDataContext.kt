package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl

/**
 * Scope 濉斾腑鐨勫崟涓暟鎹厓绱犮€? *
 * 姣忎釜鍏冪礌灏佽涓€涓?scope 鍙婂叾鍏冩暟鎹紙鏄惁涓哄眬閮?scope锛夈€? * Phase 2 浠呮敮鎸?scope 绫诲瀷鐨勫厓绱狅紝鍚庣画灏嗘墿灞曚负鏀寔闅愬紡鎺ユ敹鑰呫€? *
 * 鍙傝€?K2 FirTowerDataElement銆? */
class CfirTowerDataElement(
    /** 璇ュ眰绾у搴旂殑 scope锛堥潪绌猴級 */
    val scope: CfirScope,
    /** 鏄惁涓哄眬閮?scope锛堝潡/鍑芥暟浣撳唴鐨勫眬閮ㄥ彉閲?scope锛?*/
    val isLocal: Boolean,
)

/**
 * Scope 濉斾笂涓嬫枃锛屾寔鏈夊綋鍓嶈В鏋愮偣鐨勫畬鏁?scope 鏍堛€? *
 * 浣跨敤 copy-on-write 璇箟锛堟瘡娆″彉鏇磋繑鍥炴柊 List锛夛紝
 * 杩涘叆/閫€鍑哄０鏄庝笂涓嬫枃鏃堕€氳繃鏂板缓 context 瀹炰緥绠＄悊 scope 鍙樺寲銆? *
 * 鍙傝€?K2 FirTowerDataContext銆? */
data class CfirTowerDataContext private constructor(
    /** 鎵€鏈?scope 鍏冪礌锛屼粠澶栧埌鍐呮帓鍒?*/
    val towerDataElements: List<CfirTowerDataElement>,
    /** 灞€閮?scope 鍒楄〃锛堜粠澶栧埌鍐咃紝鏄?towerDataElements 涓?isLocal=true 鐨勫瓙闆嗭級 */
    val localScopes: List<CfirLocalScopeImpl>,
    /** 闈炲眬閮?scope 鍏冪礌鍒楄〃 */
    val nonLocalTowerDataElements: List<CfirTowerDataElement>,
) {

    constructor() : this(
        towerDataElements = emptyList(),
        localScopes = emptyList(),
        nonLocalTowerDataElements = emptyList(),
    )

    /** 娣诲姞灞€閮?scope锛堝嚱鏁颁綋/鍧楀唴鐨勫彉閲?scope锛?*/
    fun addLocalScope(localScope: CfirLocalScopeImpl): CfirTowerDataContext {
        val element = CfirTowerDataElement(localScope, isLocal = true)
        return copy(
            towerDataElements = towerDataElements + element,
            localScopes = localScopes + localScope,
        )
    }

    /** 娣诲姞闈炲眬閮?scope锛堝寘銆佸鍏ャ€佺被鎴愬憳銆佺被鍨嬪弬鏁扮瓑锛?*/
    fun addNonLocalScope(scope: CfirScope): CfirTowerDataContext {
        val element = CfirTowerDataElement(scope, isLocal = false)
        return copy(
            towerDataElements = towerDataElements + element,
            nonLocalTowerDataElements = nonLocalTowerDataElements + element,
        )
    }

    /** 鎵归噺娣诲姞闈炲眬閮?scope */
    fun addNonLocalScopes(scopes: List<CfirScope>): CfirTowerDataContext {
        if (scopes.isEmpty()) return this
        var ctx = this
        for (scope in scopes) {
            ctx = ctx.addNonLocalScope(scope)
        }
        return ctx
    }

    /** 鑾峰彇鎵€鏈?scope锛堜粠鍐呭埌澶栵級锛岀敤浜庡悕绉版煡鎵?*/
    fun allScopesReversed(): List<CfirScope> {
        return towerDataElements.asReversed().map { it.scope }
    }
}

