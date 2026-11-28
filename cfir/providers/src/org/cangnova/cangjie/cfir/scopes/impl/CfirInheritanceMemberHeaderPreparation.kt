package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.ownTypeParameters
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*

/**
 * prepare owner 的原声明空间 header 生产。所有 source/alias/可达参数依赖读取均由本次
 * normalize/registerParameterOwner 完成；返回产品不持有回调，也不应用实例化 owner 映射。
 */
internal fun captureInheritanceMemberHeader(
    member: CfirDeclaration,
    physicalOwner: CfirDeclaration,
    logicalOwner: CfirDeclaration,
    normalize: (ConeCangJieType) -> ConeCangJieType,
    registerParameterOwner: (CfirTypeParameterSymbol) -> Unit,
): CfirInheritanceMemberHeader? {
    val callable = member as? CfirCallableDeclaration ?: return null
    val kind = when (member) {
        is CfirFunction -> CfirInheritanceMemberKind.FUNCTION
        is CfirProperty -> CfirInheritanceMemberKind.PROPERTY
        is CfirFieldVariable -> CfirInheritanceMemberKind.VARIABLE
        else -> return null
    }
    val result = callable.returnTypeRef.coneTypeOrNull?.let(normalize)?.takeUnless { it.containsErrorType() } ?: return null
    val parameters = (member as? CfirFunction)?.valueParameters.orEmpty()
    val parameterTypes = parameters.map { parameter ->
        parameter.returnTypeRef.coneTypeOrNull?.let(normalize)?.takeUnless { it.containsErrorType() } ?: return null
    }
    fun closedBounds(root: CfirTypeParameterSymbol): List<ConeCangJieType> {
        val pending = ArrayDeque<CfirTypeParameterSymbol>()
        val visited = hashSetOf<CfirTypeParameterSymbol>()
        val result = linkedSetOf<ConeCangJieType>()
        pending += root
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!visited.add(current)) continue
            registerParameterOwner(current)
            for (bound in current.resolvedBounds) {
                val value = normalize(bound.coneType)
                val parameter = (value as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol
                if (parameter === root) continue
                result += value
                parameter?.let(pending::addLast)
            }
        }
        return result.toList()
    }
    val ownParameters = callable.typeParameters.ownTypeParameters
    val status = callable.status
    return CfirInheritanceMemberHeader(callable.symbol, callable.symbol.name, kind, physicalOwner, logicalOwner,
        CfirInheritanceMemberFlags(status.isStatic, status.isOverride, status.isRedef, status.isConst, status.isMut,
            status.isDefault, status.isAbstract, status.visibility, (member as? CfirFieldVariable)?.isVar ?: status.isMut,
            (member as? CfirProperty)?.getter != null, (member as? CfirProperty)?.setter != null,
            status.isOpen, logicalOwner is CfirInterface, member is CfirConstructor, status.isOperator),
        result, parameterTypes, parameters.map { it.name }, parameters.map { it.isNamed }, ownParameters.map { it.symbol },
        ownParameters.associate { parameter ->
            val bounds = closedBounds(parameter.symbol)
            parameter.symbol to if (bounds.size > 1) listOf(ConeIntersectionType(java.util.Collections.unmodifiableList(ArrayList(bounds)))) else bounds
        }, member is CfirFunction && member.hasGenericNode,
        (result as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol?.let(::closedBounds).orEmpty())
}
