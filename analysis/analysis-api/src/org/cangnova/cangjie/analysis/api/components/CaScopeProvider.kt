package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.FqName
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
     */
    val CaType.scope: CaScope?
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
