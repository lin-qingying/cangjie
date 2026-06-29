package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin

/**
 * `.cjo` stub 构建上下文。
 *
 * 负责在反编译 stub 构建期间稳定传播 owner、package facade 来源和 type builder。
 */
internal data class CjoStubBuilderContext(
    /** 当前 `.cjo` package 的包全限定名。 */
    val packageFqName: FqName,

    /** 当前正在构建成员时所属类型的全限定名，顶层声明为 `null`。 */
    val owningClassFqName: FqName? = null,

    /** 当前所属类型的短名，用于构造 extend body 或成员 stub 的展示 owner。 */
    val owningClassSimpleName: String? = null,

    /** 当前上下文是否正在构建 extend body 内的声明。 */
    val isExtendBody: Boolean = false,

    /** 当前文件作为 package facade 时的来源信息。 */
    val packageFacadeOrigin: CangJieStubOrigin.Facade? = null,

    /** 共享的类型引用 stub 构建器，保证同一文件内类型 stub 生成规则一致。 */
    val typeStubBuilder: TypeCjoStubBuilder = TypeCjoStubBuilder(),
) {
    /**
     * 创建进入子类型声明后的上下文。
     *
     * 子上下文会更新 owning class 全限定名和短名，并离开 extend body 状态。
     */
    fun child(name: Name): CjoStubBuilderContext {
        val qualifiedName = composeQualifiedName(packageFqName, owningClassFqName, name)
        return copy(
            owningClassFqName = qualifiedName,
            owningClassSimpleName = name.asString(),
            isExtendBody = false,
        )
    }

    /**
     * 返回只更新所属类型短名的上下文。
     */
    fun withContainingClassSimpleName(simpleName: String): CjoStubBuilderContext {
        return copy(owningClassSimpleName = simpleName)
    }

    /**
     * 创建用于 extend body 成员构建的上下文。
     */
    fun forExtendBody(simpleName: String): CjoStubBuilderContext {
        return copy(
            owningClassSimpleName = simpleName,
            isExtendBody = true,
        )
    }
}
