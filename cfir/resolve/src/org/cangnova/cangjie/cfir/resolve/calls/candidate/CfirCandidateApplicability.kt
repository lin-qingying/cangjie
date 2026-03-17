package org.cangnova.cangjie.cfir.resolve.calls.candidate

/**
 * 鍊欓€夐€傜敤鎬х瓑绾э紙7 绾э級銆? *
 * 鍊间粠浣庡埌楂樻帓鍒楋細HIDDEN 鏈€宸紝RESOLVED 鏈€浼樸€? * 鐢ㄤ簬鍊欓€夋敹闆嗗櫒姣旇緝鍊欓€変紭鍔ｏ紝浠ュ強 Tower 閬嶅巻鐨勫仠姝㈡潯浠跺垽瀹氥€? *
 * 瀵归綈 K2 CandidateApplicability锛屽幓鎺?SAM/suspend/smart-cast/K1-K2 鍏煎灞傜瓑 Kotlin 鐗规湁灞傜骇銆? */
enum class CfirCandidateApplicability {

    /** 鍊欓€夎闅愯棌锛圫inceKotlin / Deprecation 绛夛紝浠撻涓富瑕佺敤浜庡唴閮?API 闅愯棌锛?*/
    HIDDEN,

    /** 鎺ユ敹鑰呯被鍨嬩笉鍖归厤 */
    INAPPLICABLE_WRONG_RECEIVER,

    /** 鍙傛暟鏄犲皠閿欒锛堝弬鏁版暟閲忎笉鍖归厤锛?*/
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,

    /** 鍙傛暟绫诲瀷涓嶅吋瀹圭瓑涓€鑸笉閫傜敤 */
    INAPPLICABLE,

    /** 閫傜敤浣嗕紭鍏堢骇杈冧綆锛堜綆浼樺厛绾ч噸杞芥爣璁帮級 */
    RESOLVED_LOW_PRIORITY,

    /** 閫傜敤浣嗘湁閿欒锛堜緥濡傚彲瑙佹€ц繚瑙勶紝浠嶅彲浣滀负鍞竴鍊欓€夋姤閿欙級 */
    RESOLVED_WITH_ERROR,

    /** 瀹屽叏閫傜敤 */
    RESOLVED;

    /** 鏄惁涓烘垚鍔熼€傜敤锛堝彲浣滀负鏈€缁堝€欓€夛級 */
    val isSuccess: Boolean
        get() = this >= RESOLVED_LOW_PRIORITY

    /** 鏄惁搴斿仠姝?Tower 鎼滅储锛堝凡鎵惧埌瓒冲濂界殑鍊欓€夛級 */
    val shouldStopResolve: Boolean
        get() = this >= RESOLVED_WITH_ERROR
}

