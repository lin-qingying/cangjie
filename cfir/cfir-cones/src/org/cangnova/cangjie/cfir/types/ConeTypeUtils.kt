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
 * 返回当前类型的直接组成类型。
 *
 * 对应官方 `Ty::typeArgs`：函数类型由返回值和参数组成，元组由元素组成，
 * 数组、指针、交叉与联合类型各自暴露其成员，其余名义类型使用类型实参。
 * 函数类型与元组的 [typeArguments] 为空，因此任何按“类型包含关系”遍历的地方
 * 都必须使用该投影，否则会静默漏掉写在函数签名或元组内部的类型。
 */
fun ConeCangJieType.directComponentTypes(): List<ConeCangJieType> = when (this) {
    is ConeFunctionType -> buildList {
        add(returnType)
        addAll(parameterTypes)
    }
    is ConeTupleType -> elementTypes
    is ConeVArrayType -> listOf(elementType)
    is ConePointerType -> listOf(pointeeType)
    is ConeIntersectionType -> intersectedTypes.toList()
    is ConeUnionType -> unionTypes.toList()
    else -> typeArguments.map { it.type }
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
        next.directComponentTypes().forEach { stack.add(it) }
    }
}

/**
 * 带 visited 集合的递归 contains 实现，避免循环类型导致无限递归。
 */
private fun ConeCangJieType.contains(predicate: (ConeCangJieType) -> Boolean, visited: SmartSet<ConeCangJieType>): Boolean {
    if (this in visited) return false
    if (predicate(this)) return true
    visited += this

    return directComponentTypes().any { it.contains(predicate, visited) }
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
