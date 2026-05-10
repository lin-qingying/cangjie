package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * `.cjo` stub 构建上下文。
 *
 * 当前上下文只承载 decompiled stub 构建期间稳定传播的 owner 信息和 type builder。
 */
internal data class CjoStubBuilderContext(
    val packageFqName: FqName,
    val owningClassFqName: FqName? = null,
    val owningClassSimpleName: String? = null,
    val isExtendBody: Boolean = false,
    val typeStubBuilder: TypeCjoStubBuilder = TypeCjoStubBuilder(),
) {
    fun child(name: Name): CjoStubBuilderContext {
        val qualifiedName = composeQualifiedName(packageFqName, owningClassFqName, name)
        return copy(
            owningClassFqName = qualifiedName,
            owningClassSimpleName = name.asString(),
            isExtendBody = false,
        )
    }

    fun withContainingClassSimpleName(simpleName: String): CjoStubBuilderContext {
        return copy(owningClassSimpleName = simpleName)
    }

    fun forExtendBody(simpleName: String): CjoStubBuilderContext {
        return copy(
            owningClassSimpleName = simpleName,
            isExtendBody = true,
        )
    }
}
