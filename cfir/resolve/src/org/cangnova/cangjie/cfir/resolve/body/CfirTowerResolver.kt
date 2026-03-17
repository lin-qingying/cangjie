package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirSessionHolder
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.name.Name

/**
 * Scope 濉旇В鏋愬櫒锛岃礋璐ｅ湪 scope 濉斾腑鎸夊眰绾ф悳绱㈢鍙枫€? *
 * 鏀寔涓ょ妯″紡锛? * 1. 鏃х増 findFunctions/findVariables 鈥?绠€鍗曟寜鍚嶇О鏌ユ壘锛堝悜鍚庡吋瀹癸級
 * 2. runResolver 鈥?瀹屾暣鐨?Tower 閬嶅巻 + 鍊欓€夋敹闆嗭紙Phase 3 鏂板锛? *
 * 鍙傝€?K2 FirTowerResolver(components, resolutionStageRunner, collector)銆? */
class CfirTowerResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
    internal val collector: CfirCandidateCollector =
        CfirCandidateCollector(components, resolutionStageRunner),
) : CfirSessionHolder {

    override val session: CfirSession get() = components.session

    // ---- Phase 3: 瀹屾暣 Tower 閬嶅巻 ----

    /**
     * 鎵ц瀹屾暣鐨?Tower 閬嶅巻瑙ｆ瀽銆?     *
     * 閬嶅巻 towerDataContext.allScopesReversed() 鐨勬瘡涓€灞傦細
     * - 涓烘瘡灞傚垎閰?CfirTowerGroup
     * - 瀵规瘡涓尮閰嶅悕绉扮殑绗﹀彿鍒涘缓 CfirCandidate
     * - 閫氳繃 collector 鏀堕泦鍜屾帓搴忓€欓€?     * - 鏍规嵁 shouldStopAtTheGroup 鍐冲畾鏄惁鎻愬墠缁堟
     *
     * @param callInfo 璋冪敤淇℃伅
     * @param context 瑙ｆ瀽涓婁笅鏂?     */
    fun runResolver(callInfo: CfirCallInfo, context: CfirResolutionContext) {
        collector.newDataSet()

        val towerDataElements = components.towerDataContext.towerDataElements
        var localDepth = 0
        var importedDepth = 0

        // 浠庡唴鍒板閬嶅巻 scope 濉?
        for (element in towerDataElements.asReversed()) {
            val scope = element.scope
            val group = classifyScope(scope, element.isLocal, localDepth, importedDepth)

            // 妫€鏌ユ槸鍚﹀簲鍦ㄦ灞傜骇鍋滄
            if (collector.shouldStopAtTheGroup(group)) break

            // 鍦ㄦ scope 涓煡鎵惧尮閰嶅悕绉扮殑鍑芥暟绗﹀彿
            val symbols = mutableListOf<CfirCallableSymbol<*>>()
            scope.processFunctionsByName(callInfo.name) { symbols.add(it) }

            // 涓烘瘡涓尮閰嶇鍙峰垱寤哄€欓€夊苟鎻愪氦鏀堕泦
            for (symbol in symbols) {
                val candidate = CfirCandidate(
                    symbol = symbol,
                    callInfo = callInfo,
                    originScope = scope,
                )
                collector.consumeCandidate(group, candidate, context)
            }

            // 鏇存柊娣卞害璁℃暟
            if (element.isLocal) localDepth++
            if (scope is CfirImportScope) importedDepth++
        }
    }

    /**
     * 鏍规嵁 scope 绫诲瀷鍜屽睘鎬у垎閰?TowerGroup銆?     */
    private fun classifyScope(scope: CfirScope, isLocal: Boolean, localDepth: Int, importedDepth: Int): CfirTowerGroup {
        return when {
            scope is CfirClassDeclaredMemberScope -> CfirTowerGroup.MEMBER
            scope is CfirClassScope -> CfirTowerGroup.MEMBER
            isLocal || scope is CfirLocalScopeImpl || scope is CfirLocalScope -> CfirTowerGroup.local(localDepth)
            scope is CfirExtendMemberScope || scope is CfirExtendScope -> CfirTowerGroup.EXTEND
            scope is CfirImportScope -> CfirTowerGroup.imported(importedDepth)
            scope is CfirPackageMemberScope || scope is CfirPackageScope -> CfirTowerGroup.PACKAGE
            else -> CfirTowerGroup.PACKAGE // 淇濆畧绛栫暐锛氭湭鐭?scope 瑙嗕负鏈€浣庝紭鍏堢骇
        }
    }

    // ---- 鏃х増 API锛堝悜鍚庡吋瀹癸級 ----

    /**
     * 鎸夊悕绉版煡鎵惧彉閲?灞炴€х鍙枫€?     *
     * 浼樺厛鏌ユ壘灞€閮ㄥ彉閲忥紙CfirLocalScope 鐨?processVariablesByName锛夛紝
     * 鍐嶆煡鎵惧睘鎬э紙processPropertiesByName锛夈€?     * 杩斿洖绗竴涓尮閰嶅眰鐨勬墍鏈夌鍙枫€?     */
    fun findVariables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()

            // 灞€閮?scope 浼樺厛鏌ユ壘灞€閮ㄥ彉閲?
            if (scope is CfirLocalScopeImpl) {
                scope.processVariablesByName(name) { result.add(it) }
            }

            // 涔熸煡鎵惧睘鎬э紙绫绘垚鍛樺睘鎬х瓑锛?
            scope.processPropertiesByName(name) { result.add(it) }

            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 鎸夊悕绉版煡鎵惧嚱鏁扮鍙凤紝杩斿洖绗竴涓尮閰嶅眰鐨勬墍鏈夌鍙?*/
    fun findFunctions(name: Name): List<CfirFunctionSymbol> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol>()
            scope.processFunctionsByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 鎸夊悕绉版煡鎵剧被绗﹀彿锛岃繑鍥炵涓€涓尮閰嶅眰鐨勬墍鏈夌鍙?*/
    fun findClassifiers(name: Name): List<CfirClassSymbol> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirClassSymbol>()
            scope.processClassifiersByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 鍦ㄦ寚瀹氱殑 scope 鍒楄〃涓煡鎵惧彉閲?灞炴€х鍙枫€?     *
     * 鐢ㄤ簬甯︽帴鏀惰€呯殑灞炴€ц闂紙鍦ㄦ帴鏀惰€呯被鍨嬬殑鎴愬憳 scope 涓煡鎵撅級銆?     */
    fun findVariablesInScopes(name: Name, scopes: List<CfirScope>): List<CfirCallableSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            scope.processPropertiesByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 鍦ㄦ寚瀹氱殑 scope 鍒楄〃涓煡鎵惧嚱鏁扮鍙枫€?     *
     * 鐢ㄤ簬甯︽帴鏀惰€呯殑鏂规硶璋冪敤銆?     */
    fun findFunctionsInScopes(name: Name, scopes: List<CfirScope>): List<CfirFunctionSymbol> {
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol>()
            scope.processFunctionsByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 閲嶇疆鏀堕泦鍣ㄧ姸鎬?*/
    fun reset() {
        collector.newDataSet()
    }
}

