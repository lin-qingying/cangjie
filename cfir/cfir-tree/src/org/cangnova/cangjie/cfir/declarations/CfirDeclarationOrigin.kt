package org.cangnova.cangjie.cfir.declarations

/**
 * CFIR 声明来源。
 *
 * Origin 描述声明是从源码、库、继承图还是编译器合成逻辑进入 CFIR 树的。
 * 下游 resolver、scope provider、checker 和 renderer 会使用这些标志区分真实源码声明、
 * 继承/替换产生的声明副本，以及无法直接映射到用户源码的 synthetic 声明。
 *
 * @property displayName 调试输出使用的稳定显示名；为 `null` 时使用对象或类名。
 * @property fromSupertypes 当前声明是否来自父类型成员合成。
 * @property generated 当前声明是否由编译器生成，而不是源码直接声明。
 * @property fromSource 当前声明是否直接来自用户源码。
 */
sealed class CfirDeclarationOrigin(
    /**
     * 调试输出使用的稳定显示名；为 `null` 时使用对象或类名。
     */
    private val displayName: String? = null,
    /**
     * 当前声明是否来自父类型成员合成。
     */
    val fromSupertypes: Boolean = false,
    /**
     * 当前声明是否由编译器生成，而不是源码直接声明。
     */
    val generated: Boolean = false,
    /**
     * 当前声明是否直接来自用户源码。
     */
    val fromSource: Boolean = false,
) {
    /**
     * 用户源码中直接出现的声明。
     */
    object Source : CfirDeclarationOrigin(fromSource = true)

    /**
     * 已编译库或外部依赖中反序列化出的声明。
     */
    object Library : CfirDeclarationOrigin()

    /**
     * 多父类型继承时合成的 intersection override 声明。
     */
    object IntersectionOverride : CfirDeclarationOrigin(fromSupertypes = true)

    /**
     * 编译器合成声明的来源族。
     *
     * Synthetic 声明不对应用户源码中的完整声明节点，但会作为正常 CFIR 声明参与解析、
     * 查询、渲染或诊断定位。
     */
    sealed class Synthetic : CfirDeclarationOrigin(generated = true) {
        /**
         * 默认 synthetic 来源，用于没有更细分来源的普通合成声明。
         */
        data object Default : Synthetic()

        /**
         * 调用解析或控制流需要的伪函数声明。
         */
        data object FakeFunction : Synthetic()

        /**
         * 仓颉内建 `Array<T>(...)` 构造表达式对应官方 `ArrayExpr`。
         *
         * 它复用 call-resolution 的候选与约束系统，但语义上不是 Kotlin
         * `when` / `try` / `!!` 一类控制结构 fake function，不能进入
         * synthetic fake function 的 expected-type equality 处理。
         */
        data object BuiltinArrayConstructor : Synthetic()

        /**
         * 仓颉内建 `CPointer<T>(...)` 构造表达式对应官方 `PointerExpr`。
         *
         * 它与 Array 一样只借用统一 call-resolution 管线承载类型实参、
         * 参数映射与约束求解，不是用户源码中的函数声明。
         */
        data object BuiltinPointerConstructor : Synthetic()

        /**
         * 仓颉内建 `CString(CPointer<UInt8>)` 构造表达式对应官方 CString built-in call。
         */
        data object BuiltinCStringConstructor : Synthetic()

        /**
         * typealias 构造入口合成声明。
         *
         * 该来源用于把类型别名构造语法接入统一 callable resolution，而不把它误认为源码函数。
         */
        object TypeAliasConstructor : Synthetic()

        /**
         * 错误恢复过程中生成的占位声明。
         */
        object Error : Synthetic()

    }

    /**
     * 源码省略但语言规则要求存在的默认声明。
     */
    object ImplicitDefault : CfirDeclarationOrigin(generated = true)

    /**
     * 泛型实例化后生成的声明副本。
     */
    object GenericInstantiation : CfirDeclarationOrigin(generated = true)

    /**
     * 仓颉扩展声明挂接到目标类型时产生的成员来源。
     */
    object Extension : CfirDeclarationOrigin(generated = true)

    /**
     * SAM 构造器合成声明。
     */
    object SamConstructor : CfirDeclarationOrigin(generated = true)

    /**
     * 对齐 Kotlin FIR 的 substitution override 概念。
     *
     * providers 层会在 use-site scope 中为 inherited/extended 成员复制出一份
     * “已经替换 owner 类型实参”的声明，这些声明不属于源码声明，也不能再回退为
     * 解析阶段的临时补丁。
     */
    sealed class SubstitutionOverride(displayName: String) : CfirDeclarationOrigin(
        displayName = displayName,
        fromSupertypes = true,
        generated = true,
    ) {
        /**
         * 在声明处 scope 中为继承成员产生的替换副本。
         */
        data object DeclarationSite : SubstitutionOverride("SubstitutionOverride.DeclarationSite")

        /**
         * 在调用处 scope 中按实际接收者类型产生的替换副本。
         */
        data object CallSite : SubstitutionOverride("SubstitutionOverride.CallSite")
    }

    /**
     * 返回稳定的调试显示名。
     */
    override fun toString(): String = displayName ?: this::class.simpleName!!
}

/**
 * 对齐 Kotlin FIR `FirDeclarationOrigin.isLazyResolvable`：
 * 只有可能以“未完全解析”状态存在于 lazy resolve 主流程中的声明 origin 才返回 `true`。
 *
 * 仓颉没有 Kotlin 的 Java/ImportedFromObjectOrStatic 等分支，
 * 因此这里只保留本地主干真实存在、并参与 lazy CFIR 的 origin 集合。
 */
val CfirDeclarationOrigin.isLazyResolvable: Boolean
    get() = when (this) {
        is CfirDeclarationOrigin.Source,
        is CfirDeclarationOrigin.Synthetic,
        is CfirDeclarationOrigin.SubstitutionOverride,
        is CfirDeclarationOrigin.SamConstructor,
        is CfirDeclarationOrigin.IntersectionOverride,
        is CfirDeclarationOrigin.Extension,
        is CfirDeclarationOrigin.ImplicitDefault,
        is CfirDeclarationOrigin.GenericInstantiation,
            -> true

        else -> false
    }
