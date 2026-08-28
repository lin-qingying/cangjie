package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeRigidType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * for-in 可迭代性语义（单一实现）。
 *
 * for-in 的迭代对象类型判别由 resolver（元素类型推断，[iterableElementTypeOrNull]）
 * 与 checker（`EXPR_IN_FORIN_MUST_HAS_ITERATOR` 诊断，[isIterableForForIn]）共享，
 * 避免两处判定漂移。对齐官方 `LoopExprs.cpp` 的 `GetIterableTy`
 * （`sema_expr_in_forin_must_has_iterator`）。
 *
 * [isIterableForForIn] 的语义 = [iterableElementTypeOrNull] 返回非空元素类型；
 * null / 错误类型不可迭代（对齐官方 `CanSkipDiag` 门控：错误类型跳过诊断）。
 * Array、VArray、Range、String 与实现 `Iterable<E>` 的类（含泛型上界，如
 * `T <: Collection<E>`）均可迭代。
 */
fun ConeCangJieType?.isIterableForForIn(session: CfirSession): Boolean =
    iterableElementTypeOrNull(session) != null

/**
 * 从 for-in 迭代对象类型提取元素类型；不可识别时返回 null。
 *
 * 数组、VArray、Range 按已知结构提取；其余类型要求存在 `Iterable<E>` 对应超类型
 * （String、HashMap、泛型上界 Collection<T> 等，由 `findCorrespondingClassLikeSupertype`
 * 统一走 corresponding-supertype 算法）。
 */
fun ConeCangJieType?.iterableElementTypeOrNull(session: CfirSession): ConeCangJieType? {
    val iterableType = this ?: return null
    if (iterableType is ConeErrorType) return null
    val expandedIterableType = iterableType.fullyExpandedType(session)
    expandedIterableType.arrayElementType?.let { return it }
    val classifierType = expandedIterableType as? ConeClassifierType
    if (classifierType?.lookupTag?.classId == StdlibClassIds.Range) {
        return expandedIterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
    }
    return expandedIterableType
        .findCorrespondingClassLikeSupertype(session, StdlibClassIds.Iterable)
        ?.typeArguments
        ?.singleOrNull()
        ?.type
}

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

/**
 * 计算数组字面量在“`Array<E>` 超类型”目标下应采用的 `Array<E>` 定形；不适用时返回 null。
 *
 * 官方 `ChkArrayLit` 的目标类型既可以是 `Array<E>` / `VArray<E, N>` 本身，也可以是
 * `Array<E>` 的超类型——例如 `ArrayList<T>` 构造器形参 `Collection<T>`。后一种情况不能把
 * 字面量当成该超类型自身的实例定形，必须按目标的元素视角 `E` 合成真正的 `Array<E>`，
 * 否则字面量只能保留无目标推断出的默认元素类型（`Array<Int64>`），并在 `T` 恰好不是
 * `Int64` 时产生假的实参类型不匹配。
 *
 * 元素视角复用 [iterableElementTypeOrNull]，使 `Collection<E>`、`Iterable<E>` 等
 * `Array<E>` 的真实超类型都经过同一条 corresponding-supertype 查询。`Range<E>`、`String`
 * 这类同样可迭代、但不能由数组字面量构造的目标，由末尾的 `Array<E> <: 目标` 校验排除。
 *
 * 直接的 `Array<E>` / `VArray<E, N>` 目标不属于本函数职责（`VArray` 还需保留长度语义），
 * 由调用方按原目标类型定形，这里返回 null。
 */
fun ConeCangJieType.arrayLiteralTypeForSupertypeTarget(session: CfirSession): ConeCangJieType? {
    val expandedTargetType = fullyExpandedType(session)
    if (expandedTargetType.arrayLiteralElementType != null) return null
    val elementType = expandedTargetType.iterableElementTypeOrNull(session) ?: return null
    val arrayType = session.symbolProvider
        .getClassLikeSymbolByClassId(StdlibClassIds.Array)
        ?.constructType(listOf(elementType))
        ?: return null
    if (!AbstractTypeChecker.isSubtypeOf(session.typeContext, arrayType, expandedTargetType)) return null
    return arrayType
}
