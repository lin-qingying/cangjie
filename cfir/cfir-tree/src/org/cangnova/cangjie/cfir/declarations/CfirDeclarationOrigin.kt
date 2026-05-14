package org.cangnova.cangjie.cfir.declarations

sealed class CfirDeclarationOrigin(
    private val displayName: String? = null,
    val fromSupertypes: Boolean = false,
    val generated: Boolean = false,
    val fromSource: Boolean = false,
) {
    object Source : CfirDeclarationOrigin(fromSource = true)
    object Library : CfirDeclarationOrigin()
    object IntersectionOverride : CfirDeclarationOrigin(fromSupertypes = true)

    sealed class Synthetic : CfirDeclarationOrigin(generated = true) {
        data object Default : Synthetic()
        data object FakeFunction : Synthetic()
        object TypeAliasConstructor : Synthetic()

        object Error : Synthetic()

    }

    object ImplicitDefault : CfirDeclarationOrigin(generated = true)
    object GenericInstantiation : CfirDeclarationOrigin(generated = true)
    object Extension : CfirDeclarationOrigin(generated = true)
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
        data object DeclarationSite : SubstitutionOverride("SubstitutionOverride.DeclarationSite")
        data object CallSite : SubstitutionOverride("SubstitutionOverride.CallSite")
    }

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
