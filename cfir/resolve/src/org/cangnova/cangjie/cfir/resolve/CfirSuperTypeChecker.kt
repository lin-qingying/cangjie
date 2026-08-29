package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.Name

/** class super type 检查的结构化结果。 */
internal data class CfirSuperTypeCheckResult(
    /** 已解析出的 class-like 超类型声明。 */
    val classLikeSupers: List<CfirClassLikeDeclaration>,
    /** 参与继承图构建的超类型边。 */
    val graphEdges: List<CfirSuperTypeGraphEdge>,
    /** 重复声明的超类型名称。 */
    val duplicateTypes: List<Name>,
    /** 直接自引用的超类型名称。 */
    val selfReferencedTypes: List<Name>,
)

/** class 直接超类型检查器。 */
internal class CfirSuperTypeChecker(
    /** 用于把 super type ref 解析为 class-like 声明的类型解析器。 */
    private val resolver: CfirTypeResolver,
) {
    /** 收集 class 超类型并检查重复继承、自引用以及继承图边。 */
    fun collectAndCheck(target: CfirClass): CfirSuperTypeCheckResult {
        val seen = linkedMapOf<String, CjSourceElement?>()
        val seenResolvedInterfaceSymbols = linkedSetOf<CfirClassSymbol>()
        val firstSourceByInterfaceSymbol = linkedMapOf<CfirClassSymbol, CjSourceElement?>()
        val classLikeSupers = mutableListOf<CfirClassLikeDeclaration>()
        val graphEdges = mutableListOf<CfirSuperTypeGraphEdge>()
        val duplicateTypes = linkedSetOf<Name>()
        val selfReferencedTypes = linkedSetOf<Name>()

        for (superTypeRef in target.superTypeRefs) {
            val key = superTypeRef.renderStableKey()
            val resolvedClass = resolver.resolveClass(superTypeRef)
            graphEdges += CfirSuperTypeGraphEdge(
                typeRef = superTypeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
                    ?: continue,
                renderedType = key,
                resolvedClassSymbol = resolvedClass?.symbol as? CfirClassSymbol,
            )

            val firstSourceByKey = seen.putIfAbsent(key, superTypeRef.source)
            val duplicateByKey = firstSourceByKey != null
            if (duplicateByKey) {
                duplicateTypes += resolvedClass?.classLikeNameOrNull() ?: key.toApproxName()
            }

            val interfaceSymbol = (resolvedClass as? CfirInterface)?.symbol as? CfirClassSymbol
            if (interfaceSymbol != null) {
                firstSourceByInterfaceSymbol.putIfAbsent(interfaceSymbol, superTypeRef.source)
                val duplicateBySymbol = !seenResolvedInterfaceSymbols.add(interfaceSymbol)
                if (duplicateBySymbol && !duplicateByKey) {
                    duplicateTypes += resolvedClass.classLikeNameOrNull() ?: key.toApproxName()
                }
            }

            val approximateTypeName = key.toApproxName()
            if (approximateTypeName == target.name || resolvedClass?.classLikeNameOrNull() == target.name) {
                selfReferencedTypes += target.name
            }
            resolvedClass?.let { classLikeSupers += it }
        }

        return CfirSuperTypeCheckResult(
            classLikeSupers = classLikeSupers,
            graphEdges = graphEdges,
            duplicateTypes = duplicateTypes.toList(),
            selfReferencedTypes = selfReferencedTypes.toList(),
        )
    }
}

/** 从稳定 key 中提取近似名称，用于错误恢复场景的诊断展示。 */
private fun String.toApproxName(): Name {
    val raw = substringAfterLast('.').substringBefore('<')
    return Name.identifierIfValid(raw) ?: Name.ERROR_NAME
}

/** 提取 class-like 声明的名称；不含名称的声明返回 null。 */
private fun CfirClassLikeDeclaration.classLikeNameOrNull(): Name? = when (this) {
    is CfirClass -> name
    is CfirPrimitiveTypeDeclaration -> name
    is CfirBuiltInDeclaration -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirTypeAlias -> name
}
