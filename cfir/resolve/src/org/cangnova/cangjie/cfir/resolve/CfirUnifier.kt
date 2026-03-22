package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.types.*

/**
 * 结构统一化算法，对标官方 LocalTypeArgumentSynthesis::UnifyOne。
 *
 * 给定 argTy <: paramTy，递归拆解类型结构并生成约束（添加到类型变量的 upper/lower bounds）。
 */
class CfirUnifier(
    private val typeRelations: CfirTypeRelations,
    private val store: CfirConstraintStore,
) {
    /** 防环：记录已处理的类型对 */
    private val memo = mutableSetOf<Pair<ConeCangJieType, ConeCangJieType>>()

    /**
     * 统一 argTy <: paramTy，递归拆解并收集约束。
     * @return true 表示统一成功（或添加了约束），false 表示检测到不兼容
     */
    fun unify(argTy: ConeCangJieType, paramTy: ConeCangJieType, position: CfirConstraintPosition): Boolean {
        // 防环
        val pair = argTy to paramTy
        if (!memo.add(pair)) return true

        try {
            return unifyCore(argTy, paramTy, position)
        } finally {
            // 不移除 memo 条目——同一对不需要重复处理
        }
    }

    /** 重置防环记录（新一轮统一化时调用） */
    fun resetMemo() {
        memo.clear()
    }

    private fun unifyCore(argTy: ConeCangJieType, paramTy: ConeCangJieType, position: CfirConstraintPosition): Boolean {
        // 1. 同类型直接返回
        if (argTy == paramTy) return true

        // 2. Quest 类型与任何类型兼容
        if (argTy is ConeQuestType || paramTy is ConeQuestType) return true

        // 3. TypeAlias 展开
        if (argTy is ConeTypeAliasType) {
            val expanded = argTy.expandedType ?: return false
            return unify(expanded, paramTy, position)
        }
        if (paramTy is ConeTypeAliasType) {
            val expanded = paramTy.expandedType ?: return false
            return unify(argTy, expanded, position)
        }

        // 4. 交集类型处理
        if (paramTy is ConeIntersectionType) {
            // A <: B & C → A <: B AND A <: C
            return paramTy.intersectedTypes.all { unify(argTy, it, position) }
        }
        if (argTy is ConeIntersectionType) {
            // A & B <: C → A <: C OR B <: C
            return argTy.intersectedTypes.any { unify(it, paramTy, position) }
        }

        // 5. 联合类型处理
        if (paramTy is ConeUnionType) {
            // A <: B | C → A <: B OR A <: C
            return paramTy.unionTypes.any { unify(argTy, it, position) }
        }
        if (argTy is ConeUnionType) {
            // A | B <: C → A <: C AND B <: C
            return argTy.unionTypes.all { unify(it, paramTy, position) }
        }

        // 6. 类型变量（占位符）处理
        //    同时检查 isPlaceholder 和是否为约束系统中已注册的类型变量
        if (paramTy is ConeTypeParameterType && (paramTy.isPlaceholder || isRegisteredVariable(paramTy))) {
            return collectUpperBound(paramTy, argTy, position)
        }
        if (argTy is ConeTypeParameterType && (argTy.isPlaceholder || isRegisteredVariable(argTy))) {
            return collectLowerBound(argTy, paramTy, position)
        }

        // 7. Nominal 类型（class/struct/enum/interface）
        if (argTy is ConeClassLikeType && paramTy is ConeClassLikeType) {
            return unifyNominal(argTy, paramTy, position)
        }

        // 8. 函数类型（参数逆变、返回协变）
        if (argTy is ConeFuncType && paramTy is ConeFuncType) {
            return unifyFuncType(argTy, paramTy, position)
        }

        // 9. 元组类型（逐元素协变）
        if (argTy is ConeTupleType && paramTy is ConeTupleType) {
            return unifyTupleType(argTy, paramTy, position)
        }

        // 10. 数组类型（不变）
        if (argTy is ConeArrayType && paramTy is ConeArrayType) {
            return unifyArrayType(argTy, paramTy, position)
        }

        // 11. 指针类型（不变）
        if (argTy is ConePointerType && paramTy is ConePointerType) {
            return unifyPointerType(argTy, paramTy, position)
        }

        // 12. Primitive 类型
        if (argTy is ConePrimitiveType && paramTy is ConePrimitiveType) {
            return unifyPrimitiveType(argTy, paramTy)
        }

        // 兼容性判断回退
        return typeRelations.isCompatible(argTy, paramTy)
    }

    /**
     * 收集下界约束：argTy 是类型变量，paramTy 是其上界。
     * 对标 UnifyTyVarCollectConstraints。
     */
    private fun collectUpperBound(
        variableType: ConeTypeParameterType,
        bound: ConeCangJieType,
        position: CfirConstraintPosition,
    ): Boolean {
        val variable = store.findTypeVariable(variableType.lookupTag.name) ?: return false

        // 避免重复
        if (bound in variable.lowerBounds) return true

        // 新下界必须满足已有上界
        for (upper in variable.upperBounds) {
            if (!typeRelations.isCompatible(bound, upper)) {
                store.reportIssue(
                    CfirConstraintIssue.ConflictingBounds(
                        variable = variable,
                        position = position,
                        message = "New lower bound $bound conflicts with existing upper bound $upper for ${variable.lookupTag.name}",
                    ),
                )
                return false
            }
        }

        variable.lowerBounds += bound
        return true
    }

    /**
     * 收集上界约束：argTy 是类型变量的下界。
     * 对标 UnifyTyVarCollectConstraints。
     */
    private fun collectLowerBound(
        variableType: ConeTypeParameterType,
        bound: ConeCangJieType,
        position: CfirConstraintPosition,
    ): Boolean {
        val variable = store.findTypeVariable(variableType.lookupTag.name) ?: return false

        // 避免重复
        if (bound in variable.upperBounds) return true

        // 新上界必须兼容已有下界
        for (lower in variable.lowerBounds) {
            if (!typeRelations.isCompatible(lower, bound)) {
                store.reportIssue(
                    CfirConstraintIssue.ConflictingBounds(
                        variable = variable,
                        position = position,
                        message = "New upper bound $bound conflicts with existing lower bound $lower for ${variable.lookupTag.name}",
                    ),
                )
                return false
            }
        }

        variable.upperBounds += bound
        return true
    }

    /** Nominal 类型统一化：沿继承链提升后比较类型参数（不变性） */
    private fun unifyNominal(
        argTy: ConeClassLikeType,
        paramTy: ConeClassLikeType,
        position: CfirConstraintPosition,
    ): Boolean {
        if (argTy.classId == paramTy.classId) {
            // 相同类构造器，逐个类型参数统一（不变性 → 双向）
            if (argTy.typeArguments.size != paramTy.typeArguments.size) return false
            return argTy.typeArguments.zip(paramTy.typeArguments).all { (a, p) ->
                unify(a, p, position) && unify(p, a, position)
            }
        }
        // 不同类构造器，尝试沿继承链查找
        return typeRelations.isCompatible(argTy, paramTy)
    }

    /** 函数类型统一化：参数逆变、返回协变 */
    private fun unifyFuncType(
        argTy: ConeFuncType,
        paramTy: ConeFuncType,
        position: CfirConstraintPosition,
    ): Boolean {
        if (argTy.parameterTypes.size != paramTy.parameterTypes.size) return false

        // 参数逆变：paramTy.param <: argTy.param
        val paramsOk = argTy.parameterTypes.zip(paramTy.parameterTypes).all { (a, p) ->
            unify(p, a, position)
        }
        // 返回协变：argTy.return <: paramTy.return
        val returnOk = unify(argTy.returnType, paramTy.returnType, position)

        return paramsOk && returnOk
    }

    /** 元组类型统一化：逐元素协变 */
    private fun unifyTupleType(
        argTy: ConeTupleType,
        paramTy: ConeTupleType,
        position: CfirConstraintPosition,
    ): Boolean {
        if (argTy.elementTypes.size != paramTy.elementTypes.size) return false
        return argTy.elementTypes.zip(paramTy.elementTypes).all { (a, p) ->
            unify(a, p, position)
        }
    }

    /** 数组类型统一化：元素类型不变 */
    private fun unifyArrayType(
        argTy: ConeArrayType,
        paramTy: ConeArrayType,
        position: CfirConstraintPosition,
    ): Boolean {
        if (argTy.dims != paramTy.dims) return false
        return unify(argTy.elementType, paramTy.elementType, position) &&
            unify(paramTy.elementType, argTy.elementType, position)
    }

    /** 指针类型统一化：指向类型不变 */
    private fun unifyPointerType(
        argTy: ConePointerType,
        paramTy: ConePointerType,
        position: CfirConstraintPosition,
    ): Boolean {
        return unify(argTy.pointeeType, paramTy.pointeeType, position) &&
            unify(paramTy.pointeeType, argTy.pointeeType, position)
    }

    /** Primitive 类型统一化：ideal 类型特殊处理 */
    private fun unifyPrimitiveType(argTy: ConePrimitiveType, paramTy: ConePrimitiveType): Boolean {
        if (argTy.kind == paramTy.kind) return true
        // ideal 类型可以兼容具体数值类型
        return typeRelations.isCompatible(argTy, paramTy)
    }

    /** 检查类型参数是否为约束系统中已注册的类型变量 */
    private fun isRegisteredVariable(type: ConeTypeParameterType): Boolean {
        return store.findTypeVariable(type.lookupTag.name) != null
    }
}
