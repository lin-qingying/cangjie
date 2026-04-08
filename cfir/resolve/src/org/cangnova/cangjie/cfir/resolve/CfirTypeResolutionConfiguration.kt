package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.scopes.CfirScope

/**
 * 对齐 Kotlin `TypeResolutionConfiguration` 的类型解析配置。
 */
data class TypeResolutionConfiguration(
    /**
     * 类型解析作用域按“高优先级在前、低优先级在后”排列。
     *
     * `CfirTypeResolver` 会按当前迭代顺序直接命中第一个可见分类器，
     * 因此这里绝不能再把文件级/包级/导入级 scope 反转后传入，
     * 否则默认导入会抢在当前文件声明之前被解析。
     */
    val scopes: Iterable<CfirScope> = emptyList(),
    val containingClassDeclarations: List<CfirClass> = emptyList(),
    val useSiteFile: CfirFile?,
    val topContainer: CfirDeclaration? = null,
    val scopeTypeParameters: Map<String, CfirTypeParameter> = emptyMap(),
) {
    fun withUseSiteFile(file: CfirFile): TypeResolutionConfiguration {
        if (useSiteFile === file) return this
        return copy(useSiteFile = file)
    }

    fun withTopContainer(container: CfirDeclaration?): TypeResolutionConfiguration {
        if (topContainer === container) return this
        return copy(topContainer = container)
    }

    fun withAdditionalTypeParameters(parameters: List<CfirTypeParameter>): TypeResolutionConfiguration {
        if (parameters.isEmpty()) return this
        val updated = LinkedHashMap(scopeTypeParameters)
        for (parameter in parameters) {
            updated[parameter.name.asString()] = parameter
        }
        return copy(scopeTypeParameters = updated)
    }

    fun withContainingClassDeclarations(classes: List<CfirClass>): TypeResolutionConfiguration {
        if (containingClassDeclarations === classes) return this
        return copy(containingClassDeclarations = classes)
    }

    fun withScopes(scopes: Iterable<CfirScope>): TypeResolutionConfiguration {
        if (this.scopes === scopes) return this
        return copy(scopes = scopes)
    }

    companion object {
        val EMPTY: TypeResolutionConfiguration = TypeResolutionConfiguration(useSiteFile = null)
    }
}

typealias CfirTypeResolutionConfiguration = TypeResolutionConfiguration
