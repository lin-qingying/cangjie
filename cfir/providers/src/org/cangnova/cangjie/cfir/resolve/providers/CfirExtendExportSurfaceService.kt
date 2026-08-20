package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.isStaticMemberForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/** extend 成员在跨包导出面中的结构分类。 */
sealed interface CfirExtendMemberExportSurface {
    /** 目标类型与 extend 同包，不需要接口 requirement gate。 */
    data object DirectlyAvailable : CfirExtendMemberExportSurface

    /** 成员不是任何 inherited interface requirement 的实现，不进入跨包成员面。 */
    data object NotExported : CfirExtendMemberExportSurface

    /** 成员由一个或多个具体接口 requirement 导出。 */
    data class InterfaceRequirements(
        val requirements: List<CfirExtendInterfaceRequirement>,
    ) : CfirExtendMemberExportSurface {
        init {
            require(requirements.isNotEmpty())
        }
    }
}

/** 单个 extend member 对应的实例化接口 requirement。 */
data class CfirExtendInterfaceRequirement(
    /** requirement 所属接口的 nominal id。 */
    val interfaceClassId: ClassId,
    /** requirement 所属的实例化接口类型。 */
    val interfaceType: ConeCangJieType,
    /** 接口中被实现的真实 callable。 */
    val requirementSymbol: CfirCallableSymbol<*>,
)

/**
 * extend 导出成员面的唯一结构 owner。
 *
 * 服务递归遍历实例化接口闭包，并复用 override 签名比较成员与 requirement。它不读取
 * use-site 文件，也不判断访问权限；可访问性由 [CfirAccessibilityChecker] 基于返回的
 * requirement 身份统一决定。
 */
class CfirExtendExportSurfaceService(
    private val session: CfirSession,
) : CfirSessionComponent {
    fun classifyMember(
        extend: CfirExtend,
        callable: CfirCallableSymbol<*>,
        provenance: CfirCallableLookupProvenance,
        receiverType: ConeCangJieType?,
    ): CfirExtendMemberExportSurface {
        val extendPackage = extend.getDeclarationPackage() ?: return CfirExtendMemberExportSurface.NotExported
        val targetPackage = extend.extendedTypeRef.coneTypeOrNull
            ?.classIdOrPrimitiveClassId
            ?.packageFqName
        val targetSharesExtendPackage = targetPackage == extendPackage ||
            targetPackage == null && extendPackage.asString() == STDLIB_CORE_PACKAGE
        if (targetSharesExtendPackage) return CfirExtendMemberExportSurface.DirectlyAvailable

        val interfaceTypes = extend.instantiatedInterfaceClosure(receiverType)
        if (interfaceTypes.isEmpty()) return CfirExtendMemberExportSurface.NotExported

        val originalCallable = callable.unwrapCallableForDeclarationMetadataLookup()
        val declarationOwnerId = originalCallable.getContainingClass()?.classId
        val inheritedRequirementType = provenance.requirementInterfaceType
            ?.takeIf { it.classIdOrPrimitiveClassId == declarationOwnerId }

        val requirements = if (
            provenance.sourceExtend === extend &&
            declarationOwnerId != null &&
            originalCallable.getContainingExtend() == null
        ) {
            val interfaceType = inheritedRequirementType
                ?: interfaceTypes.firstOrNull { it.classIdOrPrimitiveClassId == declarationOwnerId }
            interfaceType?.let {
                listOf(CfirExtendInterfaceRequirement(declarationOwnerId, it, originalCallable))
            }.orEmpty()
        } else {
            interfaceTypes.flatMap { interfaceType ->
                interfaceType.matchingRequirements(callable)
            }
        }

        return if (requirements.isEmpty()) {
            CfirExtendMemberExportSurface.NotExported
        } else {
            CfirExtendMemberExportSurface.InterfaceRequirements(requirements.distinctBy {
                Triple(it.interfaceClassId, it.interfaceType, it.requirementSymbol)
            })
        }
    }

    /** 按 extend receiver 实例化直接接口，并递归构造声明接口闭包。 */
    private fun CfirExtend.instantiatedInterfaceClosure(receiverType: ConeCangJieType?): List<ConeCangJieType> {
        val extendSubstitutor = receiverType?.let { concreteReceiver ->
            val targetPattern = extendedTypeRef.coneTypeOrNull ?: return@let null
            createExtendDeclarationSubstitution(
                session = session,
                extend = this,
                targetPattern = targetPattern,
                concreteReceiverType = concreteReceiver,
            )?.substitutor
        } ?: ConeSubstitutor.Empty

        val queue = ArrayDeque<ConeCangJieType>()
        for (superTypeRef in superTypeRefs) {
            val superType = superTypeRef.coneTypeOrNull ?: continue
            queue += extendSubstitutor.substituteOrSelf(superType)
        }

        val result = mutableListOf<ConeCangJieType>()
        val visited = linkedSetOf<ConeCangJieType>()
        while (queue.isNotEmpty()) {
            val interfaceType = queue.removeFirst()
            if (!visited.add(interfaceType)) continue
            val interfaceId = interfaceType.classIdOrPrimitiveClassId ?: continue
            val interfaceSymbol = session.symbolProvider.getClassLikeSymbolByClassId(interfaceId)
                as? CfirClassLikeSymbol<*> ?: continue
            val interfaceDeclaration = interfaceSymbol.cfir as? CfirInterface ?: continue
            result += interfaceType

            val ownerSubstitutor = interfaceDeclaration.ownerSubstitutor(interfaceType)
            for (parentRef in interfaceDeclaration.superTypeRefs) {
                val parentType = parentRef.coneTypeOrNull ?: continue
                queue += ownerSubstitutor.substituteOrSelf(parentType)
            }
        }
        return result
    }

    /** 返回当前实例化接口中与 [member] override 签名一致的 requirement。 */
    private fun ConeCangJieType.matchingRequirements(
        member: CfirCallableSymbol<*>,
    ): List<CfirExtendInterfaceRequirement> {
        val interfaceId = classIdOrPrimitiveClassId ?: return emptyList()
        val interfaceSymbol = session.symbolProvider.getClassLikeSymbolByClassId(interfaceId)
            as? CfirClassLikeSymbol<*> ?: return emptyList()
        val interfaceDeclaration = interfaceSymbol.cfir as? CfirInterface ?: return emptyList()
        val ownerSubstitutor = interfaceDeclaration.ownerSubstitutor(this)
        val memberSignature = member.overrideSignatureKey()
        val memberStatic = member.isStaticMemberForOverride()
        val result = mutableListOf<CfirExtendInterfaceRequirement>()
        CfirClassDeclaredMemberScope(interfaceSymbol).processCallablesByName(member.name) { requirement ->
            if (requirement.isStaticMemberForOverride() != memberStatic) return@processCallablesByName
            if (requirement.overrideSignatureKey(ownerSubstitutor) != memberSignature) return@processCallablesByName
            result += CfirExtendInterfaceRequirement(interfaceId, this, requirement)
        }
        return result
    }

    /** 将接口声明类型参数替换为当前接口实例的实参。 */
    private fun CfirClassLikeDeclaration.ownerSubstitutor(type: ConeCangJieType): ConeSubstitutor {
        val lookupType = type as? ConeLookupTagBasedType ?: return ConeSubstitutor.Empty
        if (typeParameters.isEmpty()) return ConeSubstitutor.Empty
        check(typeParameters.size == lookupType.typeArguments.size) {
            "Interface owner type argument count differs from declaration: $type"
        }
        val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
            typeParameters.zip(lookupType.typeArguments).associate { (parameter, argument) ->
                parameter.symbol.toLookupTag() to argument.type
            }
        return replacements.takeIf { it.isNotEmpty() }
            ?.let(::CfirTypeSubstitutorByMap)
            ?: ConeSubstitutor.Empty
    }

    private companion object {
        const val STDLIB_CORE_PACKAGE: String = "std.core"
    }
}
