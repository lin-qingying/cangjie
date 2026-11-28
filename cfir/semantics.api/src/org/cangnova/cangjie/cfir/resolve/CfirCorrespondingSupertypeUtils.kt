package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 查找当前类型到目标 class/interface 的唯一已实例化父类型视图。
 *
 * 该查询统一交给类型系统的 corresponding-supertype 算法，使继承链上的泛型替换、
 * extend 超类型和多路径继承都使用与 subtype 检查相同的语义。
 * （自 cfir:resolve 的 `ResolveUtils` 移入 providers，供 checkers 与 resolve 共用。）
 */
fun ConeCangJieType.findCorrespondingClassLikeSupertype(
    session: CfirSession,
    targetClassId: ClassId,
): ConeClassLikeType? {
    val sourceType = fullyExpandedType(session) as? ConeRigidType ?: return null
    val targetType = session.symbolProvider.getClassLikeSymbolByClassId(targetClassId)
        ?.constructType() as? ConeRigidType
        ?: return null
    val targetConstructor = with(session.typeContext) { targetType.typeConstructor() }
    val typeCheckerState = session.typeContext.newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = false,
    )
    val correspondingTypes = AbstractTypeChecker.findCorrespondingSupertypes(
        typeCheckerState,
        sourceType,
        targetConstructor,
    ).mapNotNull { type -> type as? ConeClassLikeType }
    val first = correspondingTypes.firstOrNull() ?: return null
    return first.takeIf { candidate ->
        correspondingTypes.all { current ->
            AbstractTypeChecker.equalTypes(session.typeContext, candidate, current)
        }
    }
}
