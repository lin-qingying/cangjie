package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import java.util.ArrayDeque

/**
 * match pattern 专用 subtype 关系。
 *
 * 官方 pattern checking/usefulness 使用 `IsSubtypeBoxed`，其中包含普通 subtype
 * 与 extend 引入的 boxed interface 关系。CFIR 的 type-aware supertype provider
 * 是 declared supertype 与 extend supertype 的统一入口；match 语义在普通 subtype
 * 未命中时显式遍历该入口，避免直接读取未实例化的 extend 规则索引。
 */
fun ConeCangJieType.isMatchSubtypeOf(
    superType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    if (AbstractTypeChecker.isSubtypeOf(session.typeContext, this, superType) == true) return true
    return hasTypeAwareSupertype(superType, session)
            || hasVisibleExtendSupertype(superType, session)
}

/**
 * 通过 type-aware 父类型入口补读 extend 注入的已实例化接口。
 */
private fun ConeCangJieType.hasTypeAwareSupertype(
    superType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    val supertypeProvider = session.typeAwareSupertypeProviderOrNull ?: return false
    val queue = ArrayDeque<ConeCangJieType>()
    val visited = linkedSetOf<ConeCangJieType>()
    queue += this

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current)) continue

        for (directSupertype in supertypeProvider.getDirectSupertypes(current)) {
            if (AbstractTypeChecker.equalTypes(session.typeContext, directSupertype, superType)) return true
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, directSupertype, superType) == true) return true
            queue += directSupertype
        }
    }

    return false
}

/**
 * 直接实例化当前类型可见的 extend 声明，补足通用类型系统尚未命中的 boxed subtype。
 */
private fun ConeCangJieType.hasVisibleExtendSupertype(
    superType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    val targetKey = expandedExtendTargetKey ?: return false
    val extendProvider = session.extendProviderOrNull ?: return false

    for (extend in extendProvider.getExtendsForTarget(targetKey)) {
        if (!extendProvider.isExtendAccessible(extend)) continue
        val targetPattern = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
        val substitution = createExtendDeclarationSubstitution(
            session = session,
            extend = extend,
            targetPattern = targetPattern,
            concreteReceiverType = this,
        ) ?: continue

        for (superTypeRef in extend.superTypeRefs) {
            val extendSupertype = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            val substitutedSupertype = substitution.substitutor.substituteOrSelf(extendSupertype)
            if (AbstractTypeChecker.equalTypes(session.typeContext, substitutedSupertype, superType)) return true
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, substitutedSupertype, superType) == true) return true
        }
    }

    return false
}
