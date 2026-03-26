package org.cangnova.cangjie.cfir.resolve.calls.tower

/**
 * Tower 层级分组，表示候选在 scope 塔中的来源层级。
 * 候选收集器会用它比较层级优先级：
 * - 层级越高，优先级越高
 * - 同一层级中，`depth` 越深优先级越高
 * 对齐 K2 `TowerGroup`，但这里直接用 `depth` 表示嵌套深度。
 * 仓颉额外加入了 `EXTEND` 层级，位于 `LOCAL` 和 `IMPORTED` 之间。
 */
data class CfirTowerGroup(
    /** 层级种类。 */
    val kind: Kind,
    /** 嵌套深度，值越大表示越靠内层。 */
    val depth: Int = 0,
) : Comparable<CfirTowerGroup> {

    /**
     * Scope 塔上的层级种类。
     * 优先级从高到低依次为：`MEMBER > LOCAL > EXTEND > IMPORTED > PACKAGE`。
     */
    enum class Kind {
        /** 类的直接成员。 */
        MEMBER,
        /** 局部 scope，如函数体或块内部声明。 */
        LOCAL,
        /** `extend` 声明引入的成员，仓颉特有。 */
        EXTEND,
        /** import 引入的声明。 */
        IMPORTED,
        /** 包级声明。 */
        PACKAGE,
    }

    override fun compareTo(other: CfirTowerGroup): Int {
        // kind ordinal 越小，优先级越高
        val kindComparison = this.kind.ordinal.compareTo(other.kind.ordinal)
        if (kindComparison != 0) return kindComparison
        // 同 kind 中，depth 越大优先级越高
        return other.depth.compareTo(this.depth)
    }

    companion object {
        val Start = CfirTowerGroup(Kind.PACKAGE, Int.MAX_VALUE)
        val MEMBER = CfirTowerGroup(Kind.MEMBER)
        val EXTEND = CfirTowerGroup(Kind.EXTEND)
        val PACKAGE = CfirTowerGroup(Kind.PACKAGE)

        fun local(depth: Int) = CfirTowerGroup(Kind.LOCAL, depth)
        fun imported(depth: Int) = CfirTowerGroup(Kind.IMPORTED, depth)
    }
}

