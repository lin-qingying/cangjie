package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.utils.SmartSet
import org.cangnova.cangjie.utils.popLast

/**
 * 判断当前类型树中是否存在满足 [predicate] 的类型节点。
 */
fun ConeCangJieType.contains(predicate: (ConeCangJieType) -> Boolean): Boolean {
    return contains(predicate, SmartSet.create())
}

/**
 * 判断当前完整类型树中是否含有错误类型。
 *
 * 外层名义类型即使解析成功，类型实参仍可能包含 [ConeErrorType]；官方 InvalidTy 语义会让
 * 这类复合类型阻断后续普通类型规则，因此调用方不能只检查根类型节点。
 */
fun ConeCangJieType.containsErrorType(): Boolean = contains { type ->
    type is ConeErrorType || type.isError
}

/**
 * 递归访问当前类型及其内部包含的所有 [ConeCangJieType]。
 *
 * 遍历顺序不作为 API 契约；调用方不能依赖具体访问顺序。
 */
inline fun ConeCangJieType.forEachType(
    prepareType: (ConeCangJieType) -> ConeCangJieType = { it },
    action: (ConeCangJieType) -> Unit,
) {
    val stack = mutableListOf(this)

    while (stack.isNotEmpty()) {
        val next = stack.popLast().let(prepareType)
        action(next)

        when (next) {
            is ConeFunctionType -> {
                stack.add(next.returnType)
                stack.addAll(next.parameterTypes)
            }
            is ConeTupleType -> stack.addAll(next.elementTypes)
            is ConeVArrayType -> stack.add(next.elementType)
            is ConePointerType -> stack.add(next.pointeeType)
            is ConeIntersectionType -> stack.addAll(next.intersectedTypes)
            is ConeUnionType -> stack.addAll(next.unionTypes)
            else -> next.typeArguments.forEach { projection -> stack.add(projection.type) }
        }
    }
}

/**
 * 带 visited 集合的递归 contains 实现，避免循环类型导致无限递归。
 */
private fun ConeCangJieType.contains(predicate: (ConeCangJieType) -> Boolean, visited: SmartSet<ConeCangJieType>): Boolean {
    if (this in visited) return false
    if (predicate(this)) return true
    visited += this

    return when (this) {
        is ConeFunctionType -> parameterTypes.any { it.contains(predicate, visited) } || returnType.contains(predicate, visited)
        is ConeTupleType -> elementTypes.any { it.contains(predicate, visited) }
        is ConeVArrayType -> elementType.contains(predicate, visited)
        is ConePointerType -> pointeeType.contains(predicate, visited)
        is ConeIntersectionType -> intersectedTypes.any { it.contains(predicate, visited) }
        is ConeUnionType -> unionTypes.any { it.contains(predicate, visited) }
        else -> typeArguments.any { projection -> projection.type.contains(predicate, visited) }
    }
}

/**
 * 使用调试渲染器渲染当前类型。
 */
fun ConeCangJieType.renderForDebugging(): String {
    val builder = StringBuilder()
    ConeTypeRendererForDebugging(builder).render(this)
    return builder.toString()
}

/**
 * 返回一个带 [upperBound] 近似上界的新交叉类型。
 */
fun ConeIntersectionType.withUpperBound(upperBound: ConeCangJieType): ConeIntersectionType {
    return ConeIntersectionType(
        intersectedTypes = intersectedTypes,
        upperBoundForApproximation = upperBound,
        attributes = attributes,
    )
}

/**
 * 返回刚性类型对应的类型构造器。
 */
fun ConeRigidType.getConstructor(): ConeTypeConstructorMarker {
    return when (this) {
        is ConeLookupTagBasedType -> this.lookupTag
        is ConeTypeVariableType -> this.typeConstructor
        is ConeStubType -> this.constructor
        is ConeTypeConstructorMarker -> this
    }
}
