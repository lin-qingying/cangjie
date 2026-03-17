package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 琛ㄨ揪寮忚В鏋愭ā寮忥紝鎺у埗绫诲瀷鍚堟垚鏂瑰悜銆? *
 * 鍙傝€冧粨棰?C++ 缂栬瘧鍣ㄧ殑 Synthesize/Check 鍙屽悜绫诲瀷妫€鏌ユā寮忥紝
 * 浠ュ強 K2 鐨?ResolutionMode銆? *
 * - [ContextIndependent]锛氳嚜搴曞悜涓婃帹鏂紙Synthesize锛夛紝鏃犳湡鏈涚被鍨? * - [WithExpectedType]锛氳嚜椤跺悜涓嬮獙璇侊紙Check锛夛紝鏈夋湡鏈涚被鍨? */
sealed class CfirResolutionMode {

    /** 鑷簳鍚戜笂鎺ㄦ柇 鈥?鏃犳湡鏈涚被鍨嬶紝绾补浠庤〃杈惧紡鏈韩鍚堟垚绫诲瀷 */
    object ContextIndependent : CfirResolutionMode()

    /** 鑷《鍚戜笅楠岃瘉 鈥?鏈夋湡鏈涚被鍨嬶紝鐢ㄤ簬绫诲瀷妫€鏌ュ拰闅愬紡杞崲 */
    class WithExpectedType(val expectedType: ConeCangjieType) : CfirResolutionMode()
}

