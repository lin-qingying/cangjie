package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.importTracker
import org.cangnova.cangjie.cfir.reportImportDirectives
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirImportBindingResolver
import org.cangnova.cangjie.cfir.resolve.CfirImportConflictReporter
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull

/**
 * Import 瑙ｆ瀽闃舵鐨勫鐞嗗櫒锛圥rocessor锛夈€? *
 * 鑱岃矗锛氫綔涓?[CfirTransformerBasedResolveProcessor] 鐨勫叿浣撳疄鐜帮紝
 * 灏?Import 瑙ｆ瀽闃舵锛圼CfirResolvePhase.IMPORTS]锛変笌瀵瑰簲鐨?Transformer
 * [CfirImportResolveTransformer] 缁戝畾鍦ㄤ竴璧凤紝椹卞姩鏁翠釜 Import 瑙ｆ瀽娴佺▼銆? *
 * 鍦ㄧ紪璇戝櫒 resolve 娴佹按绾夸腑锛屾瘡涓樁娈甸兘鏈変竴涓搴旂殑 Processor锛? * 鏈被璐熻矗鐨勬槸 IMPORTS 闃舵銆? *
 * @param diagnosticReporter 鐢ㄤ簬鏀堕泦骞朵笂鎶ヨ瘖鏂俊鎭紙閿欒銆佽鍛婄瓑锛? * @param session 褰撳墠缂栬瘧浼氳瘽锛屾寔鏈夊叏灞€绗﹀彿琛ㄣ€侀厤缃瓑涓婁笅鏂? * @param scopeSession 浣滅敤鍩熶細璇濓紝绠＄悊绗﹀彿鏌ユ壘鐨勪綔鐢ㄥ煙缂撳瓨
 */
internal class CfirImportResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPORTS, // 澹版槑鏈?Processor 璐熻矗 IMPORTS 闃舵
) {
    // 灏嗗疄闄呯殑鏍戦亶鍘嗗拰 import 瑙ｆ瀽閫昏緫濮旀墭缁 CfirImportResolveTransformer
    override val transformer: CfirImportResolveTransformer =
        CfirImportResolveTransformer(session, diagnosticReporter)
}

/**
 * [CfirImportResolveProcessor] 鐨勭被鍨嬪埆鍚嶏紝渚涘閮ㄦā鍧椾互鏇磋涔夊寲鐨勫悕绉板紩鐢ㄣ€? * 涓よ€呭畬鍏ㄧ瓑浠枫€? */
internal typealias CfirImportsResolveProcessor = CfirImportResolveProcessor

/**
 * Import 瑙ｆ瀽闃舵鐨勬爲鍙樻崲鍣紙Transformer锛夈€? *
 * 鑱岃矗锛氶亶鍘?CFIR 鏍戯紝瀵规瘡涓?[CfirFile] 鑺傜偣鎵ц import 缁戝畾瑙ｆ瀽锛? * 鍏蜂綋鍖呮嫭锛? *  1. 灏嗘瘡鏉?import 鎸囦护瑙ｆ瀽涓哄搴旂殑绗﹀彿缁戝畾锛坮esolveImportBinding锛? *  2. 妫€娴嬪苟涓婃姤鏃犳硶瑙ｆ瀽鐨?import 鐩爣锛坲nresolved targets锛? *  3. 妫€娴嬪苟涓婃姤鍚屽悕 import 鍐茬獊锛坈onflicts锛? *  4. 灏嗚В鏋愮粨鏋滆褰曞埌 importBindingStore 渚涘悗缁樁娈典娇鐢? *  5. 鍚?importTracker 涓婃姤 import 璺緞锛岀敤浜庡閲忕紪璇戜緷璧栬拷韪? *
 * 缁ф壙鑷?[CfirAbstractTreeTransformer]锛屾硾鍨嬪弬鏁?`Nothing?` 琛ㄧず
 * 閬嶅巻鏃朵笉闇€瑕侀澶栫殑涓婁笅鏂囨暟鎹紶閫掋€? *
 * @param session 褰撳墠缂栬瘧浼氳瘽
 * @param diagnosticReporter 鐢ㄤ簬鏀堕泦璇婃柇淇℃伅
 */
internal class CfirImportResolveTransformer(
    override val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.IMPORTS) {
    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirDeclaration) {
            @Suppress("UNCHECKED_CAST")
            return transformDeclaration(element, data) as E
        }
        return super.transformElement(element, data)
    }

    /**
     * 瀵瑰崟涓０鏄庤妭鐐规墽琛?import 瑙ｆ瀽鍙樻崲銆?     *
     * 杩欐槸鏍戦亶鍘嗙殑鏍稿績鍥炶皟锛屾瘡涓?[CfirDeclaration] 鑺傜偣閮戒細缁忚繃姝ゆ柟娉曘€?     *
     * @param declaration 褰撳墠姝ｅ湪澶勭悊鐨勫０鏄庤妭鐐?     * @param data 閫忎紶鏁版嵁锛屾湰闃舵涓嶄娇鐢紙鍥哄畾涓?null锛?     * @return 澶勭悊鍚庣殑澹版槑鑺傜偣锛堟湰闃舵涓嶆浛鎹㈣妭鐐癸紝鍘熸牱杩斿洖锛?     */
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {

        // 鈹€鈹€ 闃舵瀹堝崼锛圥hase Guard锛夆攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
        // CFIR 鐨?resolve 鏄闃舵娴佹按绾匡紝姣忎釜鑺傜偣閮芥湁涓€涓?
        // resolvePhase 鏍囪
        // 褰撳墠闃舵鏍囪琛ㄧず璇ヨ妭鐐瑰凡瀹屾垚鐨勬渶楂樿В鏋愰樁娈点€?        //
        // 璺宠繃鏉′欢锛?        //   - resolvePhase < RAW_CFIR锛氬墠缃樁娈碉紙Raw CFIR 鏋勫缓锛夊皻鏈畬鎴愶紝
        //     import 瑙ｆ瀽渚濊禆 Raw CFIR 鐨勭粨鏋勶紝涓嶈兘鎻愬墠澶勭悊
        //   - resolvePhase >= IMPORTS锛氬凡缁忓畬鎴愪簡 import 瑙ｆ瀽锛岄伩鍏嶉噸澶嶅鐞?        //     锛堝湪澧為噺缂栬瘧鎴栧娆￠亶鍘嗘椂灏や负閲嶈锛?
        if (declaration.resolvePhase < CfirResolvePhase.RAW_CFIR || declaration.resolvePhase >= CfirResolvePhase.IMPORTS) {
            return declaration
        }

        declaration.transformChildren(this, data)

        // 鈹€鈹€ 浠呭鐞嗘枃浠剁骇鍒妭鐐?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
        // import 鎸囦护鍙瓨鍦ㄤ簬鏂囦欢鐨勯《灞傦紝鍥犳鍙 CfirFile 鑺傜偣鍋氬疄闄呭鐞嗐€?        // 鍏朵粬绫诲瀷鐨勫０鏄庯紙绫汇€佸嚱鏁扮瓑锛夌洿鎺ヨ烦杩囷紝浠呭湪鏈€鍚庢洿鏂?resolvePhase銆?
        if (declaration is CfirFile) {

            // importBindingStore 鏄綋鍓?session 涓瓨鍌?import 瑙ｆ瀽缁撴灉鐨勪粨搴撱€?            // 鑻ヤ负 null锛岃鏄庡綋鍓嶇紪璇戦厤缃笉闇€瑕?import 缁戝畾锛堜緥濡傛煇浜涜交閲忔ā寮忥級锛?            // 鐩存帴璺宠繃瑙ｆ瀽閫昏緫銆?
            val store = session.importBindingStoreOrNull
            if (store != null) {

                // 鈹€鈹€ Step 1锛氬垱寤烘湰鏂囦欢鐨?Import 缁戝畾瑙ｆ瀽鍣?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
                // CfirImportBindingResolver 璐熻矗灏嗕竴鏉?import 璇彞锛圛mportDirective锛?                // 瑙ｆ瀽涓哄叿浣撶殑绗﹀彿缁戝畾锛屼緥濡傚皢 "import foo.Bar" 鍏宠仈鍒?Bar 绫荤殑绗﹀彿銆?
                val bindingResolver = CfirImportBindingResolver(session)

                // CfirImportConflictReporter 璐熻矗妫€娴?
                // import 鍐茬獊骞堕€氳繃
                // diagnosticReporter 涓婃姤璇婃柇閿欒銆?
                val conflictReporter = CfirImportConflictReporter(diagnosticReporter)

                // 鈹€鈹€ Step 2锛氭壒閲忚В鏋愭湰鏂囦欢鎵€鏈?
                // import 鎸囦护 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
                // declaration.imports 鏄綋鍓嶆枃浠剁殑鎵€鏈?import 璇彞鍒楄〃锛?                // 閫愪竴璋冪敤 resolveImportBinding 灏嗗叾杞崲涓哄甫缁戝畾淇℃伅鐨?ResolvedImport銆?
                val resolvedImports = declaration.imports.map { bindingResolver.resolveImportBinding(it) }

                // 鈹€鈹€ Step 3锛氫笂鎶ユ棤娉曡В鏋愮殑 import 鐩爣 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
                // 渚嬪 "import foo.NonExistent"锛岀洰鏍囩鍙峰湪 classpath 涓笉瀛樺湪锛?                // 浼氫骇鐢?"unresolved reference" 绫昏瘖鏂敊璇€?
                conflictReporter.reportUnresolvedTargets(resolvedImports)

                // 鈹€鈹€ Step 4锛氫笂鎶?
                // import 鍐茬獊 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
                // 渚嬪鍚屼竴涓畝鍗曞悕绉拌涓や釜涓嶅悓鐨?import 寮曞叆锛堟槦鍙峰鍏ヤ笌鍏峰悕瀵煎叆鍐茬獊绛夛級锛?                // 浼氫骇鐢?"conflicting import" 绫昏瘖鏂敊璇€?
                conflictReporter.reportConflicts(resolvedImports)

                // 鈹€鈹€ Step 5锛氬皢瑙ｆ瀽缁撴灉璁板綍鍒?
                // importBindingStore 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
                // 鍚庣画鐨勭被鍨嬭В鏋愩€佺鍙锋煡鎵剧瓑闃舵鍙€氳繃 store 鐩存帴鑾峰彇
                // 褰撳墠鏂囦欢鐨?import 缁戝畾锛岃€屾棤闇€閲嶆柊瑙ｆ瀽銆?
                store.record(declaration, resolvedImports)

                // 鈹€鈹€ Step 6锛氬悜 importTracker 涓婃姤 import 璺緞锛堝閲忕紪璇戞敮鎸侊級鈹€鈹€
                // importTracker 鐢ㄤ簬杩借釜"鏂囦欢 鈫?鍏朵緷璧栫殑 import 璺緞"鐨勬槧灏勫叧绯伙紝
                // 鏄閲忕紪璇戜緷璧栧浘鐨勪竴閮ㄥ垎銆傚綋鏌愪釜琚?import 鐨勭鍙峰彂鐢熷彉鍖栨椂锛?                // 缂栬瘧鍣ㄥ彲閫氳繃 tracker 蹇€熸壘鍒版墍鏈夊彈褰卞搷鐨勬枃浠跺苟瑙﹀彂閲嶆柊缂栬瘧銆?
                val filePath = declaration.sourceFile?.path
                if (filePath != null) {
                    // importTracker 鏈韩鍙兘涓?null锛堥潪澧為噺缂栬瘧妯″紡涓嬩笉鍚敤锛?
                    session.importTracker?.let { tracker ->
                        for (resolvedImport in resolvedImports) {
                            // 灏嗘瘡鏉?import 鐨勫叏闄愬畾鍚嶏紙FqName锛変笂鎶ョ粰 tracker锛?                            // importedFqName 涓?null 鏃讹紙渚嬪瑙ｆ瀽澶辫触鐨?import锛変笉涓婃姤銆?
                            tracker.reportImportDirectives(
                                filePath,
                                resolvedImport.importDirective.importedFqName?.asString(),
                            )
                        }
                    }
                }
            }
        }

        // 鈹€鈹€ 鏇存柊闃舵鏍囪 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
        // 鏃犺鏄?
        // CfirFile 杩樻槸鍏朵粬澹版槑绫诲瀷锛屽鐞嗗畬姣曞悗閮藉皢 resolvePhase
        // 鎺ㄨ繘鍒?IMPORTS锛岃〃绀鸿鑺傜偣宸插畬鎴?import 瑙ｆ瀽闃舵锛?        // 闃叉鍚庣画娴佹按绾块噸澶嶅鐞嗐€?
        declaration.replaceResolvePhase(CfirResolvePhase.IMPORTS)
        return declaration
    }
}

