package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 闅愬紡杩斿洖绫诲瀷鎺ㄦ柇鐨勮绠楃姸鎬併€? *
 * 涓夌姸鎬佹満锛歂otComputed 鈫?Computing 鈫?Computed锛? * 鐢ㄤ簬閫掑綊渚濊禆妫€娴嬪拰缂撳瓨宸茶绠楃殑缁撴灉銆? *
 * 鍙傝€?K2 ImplicitBodyResolveComputationStatus銆? */
sealed class CfirImplicitBodyResolveComputationStatus {
    /** 灏氭湭寮€濮嬭绠?*/
    object NotComputed : CfirImplicitBodyResolveComputationStatus()

    /** 姝ｅ湪璁＄畻涓紙鐢ㄤ簬閫掑綊妫€娴嬶級 */
    object Computing : CfirImplicitBodyResolveComputationStatus()

    /** 宸插畬鎴愯绠楋紝缂撳瓨瑙ｆ瀽鍚庣殑绫诲瀷鍜屽彉鎹㈠悗鐨勫０鏄?*/
    class Computed(
        val resolvedType: ConeCangjieType,
        val transformedDeclaration: CfirCallableDeclaration,
    ) : CfirImplicitBodyResolveComputationStatus()
}

/**
 * 闅愬紡绫诲瀷鎺ㄦ柇璁＄畻浼氳瘽銆? *
 * 绠＄悊鎵€鏈夊彲璋冪敤澹版槑鐨勮绠楃姸鎬侊紝鎻愪緵閫掑綊淇濇姢锛? * - 棣栨璁＄畻锛歂otComputed 鈫?Computing 鈫?Computed
 * - 閫掑綊璁块棶锛氭娴嬪埌 Computing 鐘舵€?鈫?杩斿洖閿欒绫诲瀷
 * - 閲嶅璁块棶锛欳omputed 鐘舵€?鈫?鐩存帴杩斿洖缂撳瓨缁撴灉
 *
 * 鍙傝€?K2 ImplicitBodyResolveComputationSession銆? */
class CfirImplicitBodyResolveComputationSession {

    private val statusMap = HashMap<CfirCallableSymbol<*>, CfirImplicitBodyResolveComputationStatus>()

    /** 褰撳墠姝ｅ湪璁＄畻鐨勭鍙锋爤锛岀敤浜庤皟璇曞拰閿欒鎶ュ憡 */
    private val computingSymbolsStack = mutableListOf<CfirCallableSymbol<*>>()

    /** 鏌ヨ绗﹀彿鐨勫綋鍓嶈绠楃姸鎬?*/
    fun getStatus(symbol: CfirCallableSymbol<*>): CfirImplicitBodyResolveComputationStatus {
        return statusMap[symbol] ?: CfirImplicitBodyResolveComputationStatus.NotComputed
    }

    /**
     * 鎵ц璁＄畻骞剁紦瀛樼粨鏋溿€?     *
     * 1. 鏍囪涓?Computing 骞跺帇鏍?     * 2. 鎵ц [transformation] 鑾峰緱鍙樻崲鍚庣殑澹版槑
     * 3. 鎻愬彇杩斿洖绫诲瀷骞剁紦瀛樹负 Computed
     * 4. 寮规爤
     *
     * @param symbol 琚绠楃殑绗﹀彿
     * @param transformation 瀹為檯鎵ц body resolve 鐨勫彉鎹㈤棴鍖?     * @return 鍙樻崲鍚庣殑澹版槑
     */
    fun <D : CfirCallableDeclaration> compute(
        symbol: CfirCallableSymbol<*>,
        transformation: () -> D,
    ): D {
        statusMap[symbol] = CfirImplicitBodyResolveComputationStatus.Computing
        computingSymbolsStack.add(symbol)
        return try {
            val result = transformation()
            val resolvedType = extractResolvedType(result)
            statusMap[symbol] = CfirImplicitBodyResolveComputationStatus.Computed(resolvedType, result)
            result
        } finally {
            computingSymbolsStack.removeLastOrNull()
        }
    }

    /** 浠庡彉鎹㈠悗鐨勫０鏄庝腑鎻愬彇宸茶В鏋愮殑杩斿洖绫诲瀷 */
    private fun extractResolvedType(declaration: CfirCallableDeclaration): ConeCangjieType {
        val typeRef = when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirVariable -> declaration.returnTypeRef
            else -> return org.cangnova.cangjie.cfir.types.ConeErrorType("unsupported declaration for implicit type")
        }
        return if (typeRef is org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef) {
            typeRef.coneType
        } else {
            org.cangnova.cangjie.cfir.types.ConeErrorType("type not resolved after transformation")
        }
    }
}

