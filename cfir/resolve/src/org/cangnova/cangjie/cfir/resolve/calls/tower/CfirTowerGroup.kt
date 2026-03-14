package org.cangnova.cangjie.cfir.resolve.calls.tower

/**
 * Tower 层级分组，表示候选在 scope 塔中的来源层级。
 *
 * 用于候选收集器的层级优先级比较：
 * - 更高优先级（ordinal 更小）的层级优先
 * - 同一层级内通过 [depth] 区分嵌套深度（越深越优先）
 *
 * 对齐 K2 TowerGroup，使用枚举+depth 简化（K2 使用位编码）。
 * 仓颉特有：增加 EXTEND 层级（介于 LOCAL 和 IMPORTED 之间）。
 */
data class CfirTowerGroup(
    /** 层级种类 */
    val kind: Kind,
    /** 嵌套深度（用于区分同类 scope 的优先级，值越大越优先） */
    val depth: Int = 0,
) : Comparable<CfirTowerGroup> {

    /**
     * Scope 塔的层级种类。
     *
     * 按优先级从高到低排列：MEMBER > LOCAL > EXTEND > IMPORTED > PACKAGE。
     */
    enum class Kind {
        /** 类的直接成员（ClassDeclaredMemberScope） */
        MEMBER,
        /** 局部 scope（函数体/块内声明） */
        LOCAL,
        /** extend 声明引入的成员（ExtendMemberScope），仓颉特有 */
        EXTEND,
        /** import 引入的声明（ImportingScope） */
        IMPORTED,
        /** 包级声明（PackageMemberScope） */
        PACKAGE,
    }

    override fun compareTo(other: CfirTowerGroup): Int {
        // kind ordinal 越小越优先
        val kindComparison = this.kind.ordinal.compareTo(other.kind.ordinal)
        if (kindComparison != 0) return kindComparison
        // 同 kind 下 depth 越大越优先（越内层越好）
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
