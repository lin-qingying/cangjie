package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 作用域的结构性来源。
 *
 * 该枚举只说明候选如何到达使用点（局部声明、成员、包、导入），不携带可见性结论：
 * 可见性、适用性和导入需求必须由对应组件在使用点单独判断。
 *
 * [indexInTower] 记录该类作用域在解析塔中的相对优先级，值越小优先级越高；
 * 它与 Kotlin 的 `KaScopeKind` 一样只用于排序与解释，不作为查找捷径。
 */
enum class CaScopeKind(
    /**
     * 该类作用域在解析塔中的优先级序号，值越小越先被查找。
     */
    val indexInTower: Int,
) {
    /** 函数体内的局部声明（局部变量、局部函数）。 */
    LOCAL_SCOPE(0),

    /** 泛型声明的类型参数作用域。 */
    TYPE_PARAMETER_SCOPE(1),

    /** 当前类型的成员作用域（含继承链与适用 extend）。 */
    TYPE_SCOPE(2),

    /** 类的静态成员作用域，可通过类型名限定访问。 */
    STATIC_MEMBER_SCOPE(3),

    /** 本文件顶层声明作用域。 */
    FILE_SCOPE(10),

    /** 当前文件所属包的成员作用域。 */
    PACKAGE_MEMBER_SCOPE(11),

    /** 源码显式 import（含别名）作用域。 */
    EXPLICIT_SIMPLE_IMPORTING_SCOPE(12),

    /** 源码显式星号 import 作用域。 */
    EXPLICIT_STAR_IMPORTING_SCOPE(13),

    /** 语言默认 import 的简单导入作用域。 */
    DEFAULT_SIMPLE_IMPORTING_SCOPE(14),

    /** 语言默认 import 的星号导入作用域。 */
    DEFAULT_STAR_IMPORTING_SCOPE(15),

    /** 内置类型等编译器内建作用域。 */
    BUILTIN_SCOPE(20),

    /** 当前尚未分类的其它作用域。 */
    OTHER(Int.MAX_VALUE),
    ;

    /**
     * 判断该来源是否为 import 来源。
     */
    val isImporting: Boolean
        get() = this == EXPLICIT_SIMPLE_IMPORTING_SCOPE || this == EXPLICIT_STAR_IMPORTING_SCOPE ||
            this == DEFAULT_SIMPLE_IMPORTING_SCOPE || this == DEFAULT_STAR_IMPORTING_SCOPE

    /**
     * 判断该来源是否为显式 import 来源。
     */
    val isExplicitImporting: Boolean
        get() = this == EXPLICIT_SIMPLE_IMPORTING_SCOPE || this == EXPLICIT_STAR_IMPORTING_SCOPE
}

/**
 * 带来源信息的作用域条目。
 *
 * 组合视图按 [CaScopeKind.indexInTower] 与实际解析塔次序排列同名声明；
 * 同一层内的合法重载必须全部保留，调用方按 [CaScope] 的名称查询协议自行决定取舍。
 */
@CaExperimentalApi
interface CaScopeWithKind : CaLifetimeOwner {
    /** 本条目对应的作用域视图。 */
    val scope: CaScope

    /** 本作用域的结构性来源。 */
    val kind: CaScopeKind

    /**
     * 提供本作用域的声明者符号；文件、包和内建等无声明者作用域返回 `null`。
     */
    val ownerSymbol: CaSymbol?
}

/**
 * 隐式接收者视图。
 *
 * 隐式接收者本身不保证其成员一定可见：静态/实例区分、权限、extend 适用性都必须由消费方
 * 按使用点再判断。该视图只表达“此处存在这个接收者，且它的成员位于塔中的哪个层级”。
 */
@CaExperimentalApi
interface CaImplicitReceiver : CaLifetimeOwner {
    /** 接收者的具体类型；必须保留实际类型实参。 */
    val type: CaType

    /** 声明该接收者的符号（类、extend 或函数字面量）。 */
    val ownerSymbol: CaSymbol

    /** 本接收者成员在 [CaScopeContext.scopes] 中的位置；用于排序，不作为身份。 */
    val scopeIndexInTower: Int
}

/**
 * 按文件与位置确定的作用域上下文。
 *
 * 对齐 Kotlin `KaScopeContext`：`scopes` 按真实查找顺序从高优先级到低优先级排列，
 * `implicitReceivers` 给出当前位置存在的隐式接收者。
 *
 * 该上下文是词法语义的投影，禁止由调用方用 PSI 祖先遍历重写名称解析；
 * 上下文本身也不表达遮蔽结论——遮蔽需要结合 [scopes] 的顺序与具体名称查询判断。
 */
@CaExperimentalApi
interface CaScopeContext : CaLifetimeOwner {
    /** 当前位置可见的作用域，按查找优先级从高到低排列。 */
    val scopes: List<CaScopeWithKind>

    /** 当前位置存在的隐式接收者，按塔层级从内到外排列。 */
    val implicitReceivers: List<CaImplicitReceiver>
}