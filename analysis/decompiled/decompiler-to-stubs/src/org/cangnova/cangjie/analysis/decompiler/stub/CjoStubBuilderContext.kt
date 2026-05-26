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
    val packageFqName: FqName,
    val owningClassFqName: FqName? = null,
    val owningClassSimpleName: String? = null,
    val isExtendBody: Boolean = false,
    val packageFacadeOrigin: CangJieStubOrigin.Facade? = null,
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
