package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
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
    if (this is ConeTupleType && superType is ConeTupleType) {
        return elementTypes.size == superType.elementTypes.size &&
                elementTypes.zip(superType.elementTypes).all { (leafElement, rootElement) ->
                    leafElement.isMatchSubtypeOf(rootElement, session)
                }
    }
    if (this is ConeFunctionType && superType is ConeFunctionType) {
        return parameterTypes.size == superType.parameterTypes.size &&
                isCFunc == superType.isCFunc &&
                hasVariableLenArg == superType.hasVariableLenArg &&
                parameterTypes.zip(superType.parameterTypes).all { (leafParameter, rootParameter) ->
                    rootParameter.isMatchSubtypeOf(leafParameter, session)
                } &&
                returnType.isMatchSubtypeOf(superType.returnType, session)
    }
    return hasTypeAwareSupertype(superType, session)
            || hasVisibleExtendSupertype(superType, session)
}

/**
 * 判断 type pattern 是否可在 usefulness 矩阵中直接退化为 wildcard。
 *
 * 官方 `PatternUsefulness::FromTypePattern` 只在 `goalTy <: patternTy` 时把类型模式
 * 当作通配符；tuple/function 中由元素装箱、函数逆变/协变带来的可运行期命中属于
 * `IsSubtypeBoxed` 的诊断语义，不能提前吞成 wildcard，否则会把告警错误地转移到后续 `_`。
 */
fun ConeCangJieType.isTypePatternWildcardSubtypeOf(
    superType: ConeCangJieType,
    session: CfirSession,
): Boolean = isTypePatternOrdinarySubtypeOf(superType, session)

/**
 * type pattern usefulness 使用的普通 subtype 关系。
 *
 * 官方 `TypeManager::IsSubtype` 对函数和 tuple 的结构分量不会走 `IsSubtypeBoxed`
 * 的递归装箱路径；只有 `ChkTypePattern` 的 boxed 告警语义才使用那条关系。
 */
fun ConeCangJieType.isTypePatternOrdinarySubtypeOf(
    superType: ConeCangJieType,
    session: CfirSession,
): Boolean = isTypePatternOrdinarySubtypeOf(superType, session, allowValueBoxing = true)

private fun ConeCangJieType.isTypePatternOrdinarySubtypeOf(
    superType: ConeCangJieType,
    session: CfirSession,
    allowValueBoxing: Boolean,
): Boolean {
    if (this is ConeTupleType || superType is ConeTupleType) {
        if (this !is ConeTupleType || superType !is ConeTupleType) return false
        return elementTypes.size == superType.elementTypes.size &&
                elementTypes.zip(superType.elementTypes).all { (leafElement, rootElement) ->
                    leafElement.isTypePatternOrdinarySubtypeOf(rootElement, session, allowValueBoxing = false)
                }
    }

    if (this is ConeFunctionType || superType is ConeFunctionType) {
        if (this !is ConeFunctionType || superType !is ConeFunctionType) return false
        return parameterTypes.size == superType.parameterTypes.size &&
                isCFunc == superType.isCFunc &&
                hasVariableLenArg == superType.hasVariableLenArg &&
                parameterTypes.zip(superType.parameterTypes).all { (leafParameter, rootParameter) ->
                    rootParameter.isTypePatternOrdinarySubtypeOf(leafParameter, session, allowValueBoxing = false)
                } &&
                returnType.isTypePatternOrdinarySubtypeOf(superType.returnType, session, allowValueBoxing = false)
    }

    if (!allowValueBoxing && requiresBoxingToClassLikeSupertype(superType)) return false
    return AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(session.typeContext, this, superType)
}

private fun ConeCangJieType.requiresBoxingToClassLikeSupertype(superType: ConeCangJieType): Boolean {
    if (superType !== ConeAnyType && superType !is ConeClassLikeType) return false
    return when (this) {
        is ConePrimitiveType,
        is ConeStructType,
        is ConeEnumType,
        is ConeTupleType,
        is ConeFunctionType,
        is ConeVArrayType,
        -> true

        is ConeTypeAliasType -> expandedType?.requiresBoxingToClassLikeSupertype(superType) == true
        else -> false
    }
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
