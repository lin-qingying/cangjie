package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.Name

/**
 * Body 瑙ｆ瀽涓婁笅鏂囷紝绠＄悊 scope 濉斿拰澹版槑瀹瑰櫒鏍堛€? *
 * 閫氳繃鏋勯€犲嚱鏁版敞鍏ユ牳蹇冧緷璧栵細
 * - [returnTypeCalculator]锛氬０鏄庤繑鍥炵被鍨嬭绠楀櫒
 * - [dataFlowAnalyzerContext]锛氭暟鎹祦鍒嗘瀽涓婁笅鏂? *
 * 杩愯鏃剁姸鎬侊細
 * - [towerDataContext]锛歴cope 濉旓紙copy-on-write 璇箟锛? * - [file]锛氬綋鍓嶅鐞嗙殑鏂囦欢
 * - [containers]锛氬０鏄庡鍣ㄦ爤锛堟枃浠?鈫?绫?鈫?鍑芥暟 鈫?鍧楋級
 *
 * 鍙傝€?K2 BodyResolveContext(returnTypeCalculator, dataFlowAnalyzerContext, isContextCollectorMode)銆? */
class CfirBodyResolveContext(
    var returnTypeCalculator: CfirReturnTypeCalculator,
    val dataFlowAnalyzerContext: CfirDataFlowAnalyzerContext,
    private val isContextCollectorMode: Boolean = false,
) {

    /** 褰撳墠鏂囦欢 */
    lateinit var file: CfirFile

    /** 褰撳墠 scope 濉斾笂涓嬫枃 */
    var towerDataContext: CfirTowerDataContext = CfirTowerDataContext()
        private set

    /**
     * 褰撳墠鏈€鍐呭眰灞€閮?scope 寮曠敤銆?     *
     * 鐢变簬 [CfirTowerDataContext] 浣跨敤 copy-on-write 璇箟锛堣繑鍥炴柊 List锛夛紝
     * 閫氳繃 towerDataContext.localScopes 鍙栧緱鐨勫璞″湪鍚庣画 towerDataContext 鏇存柊鍚?     * 涓嶅啀鏄悓涓€瀹炰緥銆傚洜姝ょ淮鎶や竴涓嫭绔嬪紩鐢紝纭繚 [storeVariable] 鑳芥纭啓鍏ュ彲鍙樼殑灞€閮?scope銆?     */
    private var currentLocalScope: CfirLocalScopeImpl? = null

    /** 澹版槑瀹瑰櫒鏍?*/
    val containers: ArrayDeque<CfirDeclaration> = ArrayDeque()

    /** 褰撳墠鏈€鍐呭眰瀹瑰櫒 */
    val containerIfAny: CfirDeclaration?
        get() = containers.lastOrNull()

    // ---- scope 绠＄悊锛堝鎵樺埌 towerDataContext锛?----

    /** 娣诲姞闈炲眬閮?scope锛堝寘銆佸鍏ャ€佺被鎴愬憳绛夛級 */
    fun addNonLocalScope(scope: CfirScope) {
        towerDataContext = towerDataContext.addNonLocalScope(scope)
    }

    /** 鎵归噺娣诲姞闈炲眬閮?scope */
    fun addNonLocalScopes(scopes: List<CfirScope>) {
        towerDataContext = towerDataContext.addNonLocalScopes(scopes)
    }

    /** 娣诲姞灞€閮?scope锛堝嚱鏁颁綋/鍧楀唴鍙橀噺 scope锛?*/
    fun addLocalScope(localScope: CfirLocalScopeImpl) {
        towerDataContext = towerDataContext.addLocalScope(localScope)
        currentLocalScope = localScope
    }

    /** 娣诲姞灞€閮ㄥ彉閲忓埌褰撳墠鏈€鍐呭眰灞€閮?scope */
    fun storeVariable(name: Name, symbol: CfirCallableSymbol<*>) {
        val localScope = currentLocalScope ?: return
        localScope.addVariable(name, symbol)
    }

    // ---- 涓婁笅鏂囧垏鎹紙scoped 鏂规硶锛?----

    /**
     * 鍦ㄦ柊鐨?scope 涓婁笅鏂囦腑鎵ц鎿嶄綔锛屾墽琛屽畬鍚庢仮澶嶃€?     *
     * 瀵归綈 K2 BodyResolveContext 鐨?withTowerDataContext 妯″紡銆?     */
    fun <T> withTowerDataContext(newContext: CfirTowerDataContext, action: () -> T): T {
        val old = towerDataContext
        val oldLocalScope = currentLocalScope
        towerDataContext = newContext
        currentLocalScope = null
        return try {
            action()
        } finally {
            towerDataContext = old
            currentLocalScope = oldLocalScope
        }
    }

    /** 灏嗗０鏄庡帇鍏ュ鍣ㄦ爤锛屾墽琛屾搷浣滃悗寮瑰嚭銆?*/
    fun <T> withContainer(declaration: CfirDeclaration, action: () -> T): T {
        containers.addLast(declaration)
        return try {
            action()
        } finally {
            containers.removeLast()
        }
    }

    /** 璁剧疆褰撳墠鏂囦欢骞舵墽琛屾搷浣溿€?*/
    fun <T> withFile(file: CfirFile, action: () -> T): T {
        val oldFile = if (::file.isInitialized) this.file else null
        this.file = file
        return try {
            withContainer(file, action)
        } finally {
            if (oldFile != null) this.file = oldFile
        }
    }
}

