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
