package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 璋冪敤瑙ｆ瀽璇婃柇鍩虹被銆? *
 * 姣忎釜璇婃柇鎼哄甫涓€涓€傜敤鎬х瓑绾э紝鐢ㄤ簬鏇存柊鍊欓€夌殑 lowestApplicability銆? * 鍏蜂綋瀛愮被鎻忚堪鍚勭瑙ｆ瀽澶辫触鍘熷洜銆? *
 * 瀵归綈 K2 ResolutionDiagnostic锛堟暎钀藉澶勶級锛岀粺涓€鏀舵暃鍒版鏂囦欢銆? */
abstract class CfirResolutionDiagnostic(
    val applicability: CfirCandidateApplicability,
)

/** 鍊欓€夎闅愯棌锛堜笉鍙鐨勫唴閮?API 绛夛級 */
class HiddenCandidate(
    val symbol: CfirSymbol<*>,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.HIDDEN)

/** 瀹炲弬鏁伴噺涓庡舰鍙傛暟閲忎笉鍖归厤 */
class WrongArgumentCount(
    val expectedCount: Int,
    val actualCount: Int,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

/** 瀹炲弬绫诲瀷涓庡舰鍙傜被鍨嬩笉鍏煎 */
class ArgumentTypeMismatch(
    val expectedType: ConeCangjieType,
    val actualType: ConeCangjieType,
    val parameterIndex: Int,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.INAPPLICABLE)

/** 鍙鎬ц繚瑙勶紙private/protected 绛夛級 */
class VisibilityError(
    val symbol: CfirSymbol<*>,
) : CfirResolutionDiagnostic(CfirCandidateApplicability.RESOLVED_WITH_ERROR)

