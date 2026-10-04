package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 作用域查询协议。
 *
 * 设计要点/职责:
 * - 围绕文件、包、声明容器、类、扩展、类型等不同维度,提供统一的 [CaScope] 视图入口。
 * - 不同入口对应不同作用域语义:
 *   - 文件级别看到顶层声明与显式 import。
 *   - 包级别遍历包内所有顶层声明。
 *   - 类/扩展/容器级别区分 declared/combined/member 三类视图。
 *   - 类型上的 scope 表示通过该类型可见的成员集合。
 * - 协议只暴露稳定结果,不暴露 scope 合并/缓存等实现细节。
 *
 * 对齐 Kotlin Analysis API 的 `KaScopeProvider`。
 */
@CaExperimentalApi
interface CaScopeProvider : CaLifetimeOwner {
    /**
     * 获取该文件可见的作用域,包含顶层声明与 import。
     */
    fun CjFile.getFileScope(): CaScope

    /**
     * 按完全限定包名获取对应包的作用域;若包不存在则返回 `null`。
     */
    fun getPackageScope(packageFqName: FqName): CaScope?

    /**
     * 该 package symbol 对应的作用域。
     */
    val CaPackageSymbol.packageScope: CaScope
    /**
     * 该声明容器中显式 declared 的所有成员构成的作用域。
     *
     * 与 [declaredMemberScope] / 静态成员视图不同, 这里同时包含静态与非静态成员,
     * 用来表达"自身声明出来的成员全集"。
     */
    val CaDeclarationContainerSymbol.combinedDeclaredMemberScope: CaScope

    /**
     * 该类型声明显式 declared 的成员作用域,不包含继承自父类型的成员。
     */
    val CaClassLikeSymbol.declaredMemberScope: CaScope

    /**
     * 该扩展声明 declared 的成员作用域。
     */
    val CaExtendSymbol.declaredMemberScope: CaScope

    /**
     * 该类型声明对外暴露的完整成员作用域,包含继承自父类型/接口的成员。
     */
    val CaClassLikeSymbol.memberScope: CaScope
    /**
     * 该声明容器对外暴露的完整成员作用域,语义与 [CaClassLikeSymbol.memberScope] 对齐。
     */
    val CaDeclarationContainerSymbol.memberScope: CaScope

    /**
     * 通过该类型可访问到的成员作用域;不可计算时(例如错误类型)返回 `null`。
     *
     * 该入口把具体类型还原为 class-like 声明后取未替换的成员作用域，
     * 因此不保证成员签名保留接收者的实际类型实参。需要 use-site 签名的消费方
     * 必须走 [org.cangnova.cangjie.analysis.api.components.CaTypeScopeProvider]。
     */
    val CaType.scope: CaScope?

    /**
     * 通过该类型的**实例**可访问到的成员作用域（含继承链与适用 extend）；非 class-like 或错误类型返回 `null`。
     *
     * 与 [CaType.scope] 的区别：本入口只包含需要接收者才能访问的成员，
     * 适合 `receiver.member` 位置的补全。
     */
    @CaExperimentalApi
    val CaType.instanceMemberScope: CaScope?

    /**
     * 通过该类型的**类型名**可访问到的静态成员作用域；非 class-like 或没有静态成员时返回 `null`。
     *
     * 「没有静态成员」返回 `null` 而非空作用域：调用方据此判断是否需要提示
     * 「类型名访问了实例成员」，而不是拿到一个必然为空的候选集合。
     */
    @CaExperimentalApi
    val CaType.staticMemberScope: CaScope?

    /**
     * 按使用点形态给出该类型的成员作用域查询列表。
     *
     * 返回**按优先级排列**的多个视图而不是单一作用域：
     * `Type.member` 需要同时考虑静态通道与（用于诊断的）实例通道，
     * 而 `receiver.member` 只考虑实例通道。把两种形态压成一个 `CaScope`
     * 会让补全无法区分「补出来能用」与「补出来只能报错」。
     */
    @CaExperimentalApi
    fun CaType.memberScopeQueries(accessKind: CaTypeAccessKind): List<CaMemberScopeQuery>

    /**
     * 取得 [position] 处的词法作用域上下文。
     *
     * 返回的作用域来自 CFIR 解析塔投影，保留真实查找顺序、结构性来源和可用隐式接收者；
     * 禁止由调用方用 PSI 祖先遍历自行推导词法作用域。
     * [position] 可以是使用点元素本身或其后代；文件顶层位置得到导入上下文。
     */
    fun CjFile.scopeContext(position: CjElement): CaScopeContext

    /**
     * 取得该文件的导入/包/顶层声明上下文。
     *
     * 只包含本文件顶层声明、同包成员、显式导入和语言默认导入，不含局部声明；
     * 该视图独立于 [getFileScope]，不会扩大文件声明作用域的既有结果集合。
     */
    val CjFile.importingScopeContext: CaScopeContext

    /**
     * 把多个作用域组合为单一名称查询视图，保持入参顺序与全部符号身份。
     *
     * 组合不会按显示名去重：同名重载、导入别名和成员各自保留。
     * [filter] 用于只取特定来源的子集，默认保留全部。
     */
    fun List<CaScope>.asCompositeScope(filter: (CaScope) -> Boolean = { true }): CaScope

    /**
     * 按 [CaScopeKind] 过滤合成单一作用域视图，保持 [CaScopeContext.scopes] 的优先级顺序。
     */
    fun CaScopeContext.compositeScope(filter: (CaScopeKind) -> Boolean = { true }): CaScope
}

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
fun CjFile.getFileScope(): CaScope {
    return with(session) {
        getFileScope()
    }
}

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
fun getPackageScope(packageFqName: FqName): CaScope? {
    return with(session) {
        getPackageScope(packageFqName)
    }
}

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
val CaPackageSymbol.packageScope: CaScope
    get() = with(session) { packageScope }

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
val CaDeclarationContainerSymbol.combinedDeclaredMemberScope: CaScope
    get() = with(session) { combinedDeclaredMemberScope }

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
val CaClassLikeSymbol.declaredMemberScope: CaScope
    get() = with(session) { declaredMemberScope }

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
val CaExtendSymbol.declaredMemberScope: CaScope
    get() = with(session) { declaredMemberScope }

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
val CaClassLikeSymbol.memberScope: CaScope
    get() = with(session) { memberScope }

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
val CaDeclarationContainerSymbol.memberScope: CaScope
    get() = with(session) { memberScope }

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaScopeProvider` 的 context bridge。
 */
context(session: CaSession)
val CaType.scope: CaScope?
    get() = with(session) { scope }
