package org.cangnova.cangjie.cfir.resolve.calls.tower

/**
 * Tower 层级分组，表示候选在 scope 塔中的来源层级。
 * 候选收集器会用它比较层级优先级：
 * - 层级越高，优先级越高
 * - 同一层级中，`depth` 越小越靠近当前词法位置
 *
 * 对齐 K2 `TowerGroupKind` 的核心顺序：
 * explicit receiver member > local > implicit/non-local。
 * 仓颉额外加入了 `EXTEND` 层级，位于 `LOCAL` 和普通非局部 scope 之间。
 */
data class CfirTowerGroup(
    /** 层级种类。 */
    val kind: Kind,
    /** 嵌套深度，值越大表示越靠内层。 */
    val depth: Int = 0,
) : Comparable<CfirTowerGroup> {

    /**
     * Scope 塔上的层级种类。
     *
     * `EXPLICIT_MEMBER` 只用于显式接收者（如 `a.foo`）的成员候选；
     * 隐式 `this` 成员必须低于局部变量/参数，否则同名构造器参数会被字段遮蔽。
     */
    enum class Kind {
        /** 显式接收者成员。 */
        EXPLICIT_MEMBER,
        /** 局部 scope，如函数体或块内部声明。 */
        LOCAL,
        /** `extend` 声明引入的成员，仓颉特有。 */
        EXTEND,
        /** 普通非局部 scope，例如类型参数、静态 scope 等。 */
        NON_LOCAL,
        /** 隐式接收者成员，例如当前类的 `this` 成员。 */
        IMPLICIT_MEMBER,
        /** import 引入的声明。 */
        IMPORTED,
        /** 包级声明。 */
        PACKAGE,
    }

    /** 按 tower 优先级比较两个候选分组。 */
    override fun compareTo(other: CfirTowerGroup): Int {
        // kind ordinal 越小，优先级越高
        val kindComparison = this.kind.ordinal.compareTo(other.kind.ordinal)
        if (kindComparison != 0) return kindComparison
        // Kotlin tower 中 innermost local depth 为 0，depth 越小越优先。
        return this.depth.compareTo(other.depth)
    }

    companion object {
        /** tower resolve 调度的起始分组，优先级最低并带最大深度。 */
        val Start = CfirTowerGroup(Kind.PACKAGE, Int.MAX_VALUE)
        /** 显式 receiver 成员候选的固定分组。 */
        val EXPLICIT_MEMBER = CfirTowerGroup(Kind.EXPLICIT_MEMBER)
        /** 隐式 receiver 成员候选的固定分组。 */
        val IMPLICIT_MEMBER = CfirTowerGroup(Kind.IMPLICIT_MEMBER)
        /** extend 成员候选的固定分组。 */
        val EXTEND = CfirTowerGroup(Kind.EXTEND)
        /** 普通非局部候选的固定分组。 */
        val NON_LOCAL = CfirTowerGroup(Kind.NON_LOCAL)
        /** 包级候选的固定分组。 */
        val PACKAGE = CfirTowerGroup(Kind.PACKAGE)

        /** 构造指定深度的局部候选分组。 */
        fun local(depth: Int) = CfirTowerGroup(Kind.LOCAL, depth)
        /** 构造指定深度的非局部候选分组。 */
        fun nonLocal(depth: Int) = CfirTowerGroup(Kind.NON_LOCAL, depth)
        /** 构造指定深度的 import 候选分组。 */
        fun imported(depth: Int) = CfirTowerGroup(Kind.IMPORTED, depth)
    }
}
