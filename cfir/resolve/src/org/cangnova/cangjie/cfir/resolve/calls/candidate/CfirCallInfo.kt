package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

/**
 * 璋冪敤淇℃伅锛屽皝瑁呬竴娆″嚱鏁拌皟鐢?鏋勯€犲櫒璋冪敤/鍙橀噺璁块棶鐨勬墍鏈変笂涓嬫枃淇℃伅銆? *
 * 鍦?Tower 閬嶅巻鍓嶆瀯寤猴紝浼犻€掔粰鍊欓€夋敹闆嗗拰楠岃瘉绠＄嚎銆? *
 * 瀵归綈 K2 CallInfo锛屽幓鎺?invoke/SAM/callable-reference 绛?Kotlin 鐗规湁瀛楁銆? */
class CfirCallInfo(
    /** 璋冪敤绔欑偣 AST 鑺傜偣 */
    val callSite: CfirElement,
    /** 璋冪敤绉嶇被锛堝嚱鏁?鍙橀噺璁块棶/鏋勯€犲櫒锛?*/
    val callKind: CfirCallKind,
    /** 琚皟鐢ㄥ悕绉?*/
    val name: Name,
    /** 鏄惧紡鎺ユ敹鑰呰〃杈惧紡锛堝彲閫夛級 */
    val explicitReceiver: CfirExpression?,
    /** 瀹炲弬鍒楄〃 */
    val arguments: List<CfirExpression>,
    /** 鏄惧紡绫诲瀷瀹炲弬鍒楄〃 */
    val typeArguments: List<CfirTypeRef>,
    /** 缂栬瘧鍣?session */
    val session: CfirSession,
)

