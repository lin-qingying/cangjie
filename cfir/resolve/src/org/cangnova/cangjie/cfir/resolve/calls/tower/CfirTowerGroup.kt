package org.cangnova.cangjie.cfir.resolve.calls.tower

/**
 * Tower 灞傜骇鍒嗙粍锛岃〃绀哄€欓€夊湪 scope 濉斾腑鐨勬潵婧愬眰绾с€? *
 * 鐢ㄤ簬鍊欓€夋敹闆嗗櫒鐨勫眰绾т紭鍏堢骇姣旇緝锛? * - 鏇撮珮浼樺厛绾э紙ordinal 鏇村皬锛夌殑灞傜骇浼樺厛
 * - 鍚屼竴灞傜骇鍐呴€氳繃 [depth] 鍖哄垎宓屽娣卞害锛堣秺娣辫秺浼樺厛锛? *
 * 瀵归綈 K2 TowerGroup锛屼娇鐢ㄦ灇涓?depth 绠€鍖栵紙K2 浣跨敤浣嶇紪鐮侊級銆? * 浠撻鐗规湁锛氬鍔?EXTEND 灞傜骇锛堜粙浜?LOCAL 鍜?IMPORTED 涔嬮棿锛夈€? */
data class CfirTowerGroup(
    /** 灞傜骇绉嶇被 */
    val kind: Kind,
    /** 宓屽娣卞害锛堢敤浜庡尯鍒嗗悓绫?scope 鐨勪紭鍏堢骇锛屽€艰秺澶ц秺浼樺厛锛?*/
    val depth: Int = 0,
) : Comparable<CfirTowerGroup> {

    /**
     * Scope 濉旂殑灞傜骇绉嶇被銆?     *
     * 鎸変紭鍏堢骇浠庨珮鍒颁綆鎺掑垪锛歁EMBER > LOCAL > EXTEND > IMPORTED > PACKAGE銆?     */
    enum class Kind {
        /** 绫荤殑鐩存帴鎴愬憳锛圕lassDeclaredMemberScope锛?*/
        MEMBER,
        /** 灞€閮?scope锛堝嚱鏁颁綋/鍧楀唴澹版槑锛?*/
        LOCAL,
        /** extend 澹版槑寮曞叆鐨勬垚鍛橈紙ExtendMemberScope锛夛紝浠撻鐗规湁 */
        EXTEND,
        /** import 寮曞叆鐨勫０鏄庯紙ImportingScope锛?*/
        IMPORTED,
        /** 鍖呯骇澹版槑锛圥ackageMemberScope锛?*/
        PACKAGE,
    }

    override fun compareTo(other: CfirTowerGroup): Int {
        // kind ordinal 瓒婂皬瓒婁紭鍏?
        val kindComparison = this.kind.ordinal.compareTo(other.kind.ordinal)
        if (kindComparison != 0) return kindComparison
        // 鍚?kind 涓?
        // depth 瓒婂ぇ瓒婁紭鍏堬紙瓒婂唴灞傝秺濂斤級
        return other.depth.compareTo(this.depth)
    }

    companion object {
        val MEMBER = CfirTowerGroup(Kind.MEMBER)
        val EXTEND = CfirTowerGroup(Kind.EXTEND)
        val PACKAGE = CfirTowerGroup(Kind.PACKAGE)

        fun local(depth: Int) = CfirTowerGroup(Kind.LOCAL, depth)
        fun imported(depth: Int) = CfirTowerGroup(Kind.IMPORTED, depth)
    }
}

