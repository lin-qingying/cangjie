/*
 * Copyright 2010-2026 cangjie contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.asRigidType
import org.cangnova.cangjie.type.model.extractArgumentsForFunctionType
import org.cangnova.cangjie.type.model.extractElementsForTupleType
import org.cangnova.cangjie.type.model.getArgumentOrNull
import org.cangnova.cangjie.type.model.getType
import org.cangnova.cangjie.type.model.isFunctionType
import org.cangnova.cangjie.type.model.isTupleType
import org.cangnova.cangjie.type.model.parametersCount
import org.cangnova.cangjie.type.model.supertypes
import org.cangnova.cangjie.type.model.isTypeAccessible
import org.cangnova.cangjie.type.model.typeConstructor
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.SimpleTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeSystemCommonSuperTypesContext

/**
 * 公共父类型计算器（Common Super Type Calculator）
 *
 * 负责在类型推断阶段为一组类型求出"最小公共父类型"（Least Upper Bound / LUB）。
 * 例如：given [Dog, Cat]，若它们共同继承自 Animal，则返回 Animal。
 *
 * 典型应用场景：
 *   - if/else 两个分支返回不同类型，需推断整体表达式类型
 *   - when 表达式各分支类型不同时
 *   - 集合字面量元素类型不同时，如 [1, "hello"]
 *
 * 递归安全性：
 *   通过 depth 参数控制递归深度，防止以下两类无限递归：
 *   1. 泛型接口参数与原始输入相同导致的循环（如 BigEndianOrder<Bool> + BigEndianOrder<Int64>）
 *   2. 自引用泛型类型（如 interface Some<T : Some<T>>）
 *
 *   与 Kotlin 编译器的处理策略对比：
 *     Kotlin：递归时将类型参数替换为星号投影（*），保留构造器
 *     仓颉：没有星号投影，递归时直接丢弃该构造器（返回 null），结果更干净
 *
 *   示例：求 CST([Int64, Bool])
 *     Int64 和 Bool 都实现了 BigEndianOrder<Int64> / BigEndianOrder<Bool>
 *     类型参数 [Int64, Bool] 与原始输入相同，检测到递归
 *     Kotlin 结果：... & BigEndianOrder<*> & ...（保留构造器，参数退化为 *）
 *     仓颉结果：ToTokens & Hashable & ToString（丢弃所有泛型接口，只保留无参数接口）
 */
object CommonSuperTypeCalculator {

    /**
     * 公共入口：计算多个类型的公共父类型。
     *
     * 以类型列表中最大类型深度的负值作为初始 depth，使得递归深度预算
     * 恰好等于输入类型的最大嵌套层数，超出后安全丢弃该构造器。
     *
     * @param types 待求公共父类型的类型列表，不可为空
     * @return 所有输入类型的公共父类型
     */
    context(c: TypeSystemCommonSuperTypesContext)
    fun commonSuperType(types: List<CangJieTypeMarker>): CangJieTypeMarker {
        require(types.isNotEmpty()) { "Empty collection for common super type" }

        // 快速路径：只有一个类型，直接返回自身
        types.singleOrNull()?.let { return it }

        // 以最大类型深度的负值作为初始 depth：
        //   depth 从 -maxDepth 开始，每递归一层 +1
        //   当 depth > 0 时说明递归层数超过了输入类型的嵌套深度，丢弃该构造器
        val maxDepth = types.maxOfOrNull { it.typeDepth() } ?: 0
        return commonSuperTypeInternal(types, -maxDepth)
    }

    /**
     * 内部实现：携带 depth 参数的公共父类型计算。
     *
     * 算法流程（对齐官方 C++ BatchJoin）：
     *   1. 将所有类型转为 RigidType，若有失败则返回错误类型
     *   2. 去重（uniquify）：移除类型相等的重复元素
     *   3. 快速路径（findSmallestSupertype）：在输入中直接找最小公共父类型
     *   4. 过滤严格父类型（filterStrictSupertypes）：移除冗余的父类型
     *   5. 整数字面量特殊处理
     *   6. 函数类型特化（joinFunctionTypes）：参数逆变 + 返回值协变
     *   7. 元组类型特化（joinTupleTypes）：各分量协变 Join
     *   8. 通用路径：计算所有类型共同的父类型构造器集合
     *   9. 为每个公共构造器推断出带类型参数的具体父类型（递归检测到环则丢弃）
     *   10. 合并候选结果，尝试简化交叉类型（simplifyIntersection）
     *
     * @param types 待求公共父类型的类型列表
     * @param depth 当前递归深度预算，超过 0 时触发构造器丢弃
     * @return 所有输入类型的公共父类型
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun commonSuperTypeInternal(
        types: List<CangJieTypeMarker>,
        depth: Int,
    ): CangJieTypeMarker {
        val rigidTypes = types.mapNotNull { it.asRigidType() }
        if (rigidTypes.size != types.size) {
            return c.createErrorType("CST(${types.joinToString()})", delegatedType = null)
        }

        val typeCheckerState = c.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = true,
        )

        // 步骤一：去重
        val uniqueTypes = uniquify(rigidTypes, typeCheckerState)
        if (uniqueTypes.size == 1) return uniqueTypes.single()

        // 步骤二：快速路径 — 在输入中直接找最小公共父类型
        // 对齐官方 C++ FindSmallestTy(realTys, isSupertype)
        findSmallestSupertype(uniqueTypes, typeCheckerState)?.let { return it }

        // 步骤三：过滤掉严格父类型
        // 例如：[List<Int>, Collection<Int>]，List <: Collection，过滤后只剩 [List<Int>]
        val filteredTypes = filterStrictSupertypes(uniqueTypes, typeCheckerState)
        if (filteredTypes.size == 1) return filteredTypes.single()

        // 步骤四：整数字面量类型的特殊处理
        c.findCommonIntegerLiteralTypesSuperType(filteredTypes)?.let { return it }

        // 步骤五：函数类型特化 — 参数逆变 + 返回值协变
        // 对齐官方 C++ JoinOrMeetFuncTy
        joinFunctionTypes(filteredTypes, depth)?.let { return it }

        // 步骤六：元组类型特化 — 各分量协变 Join
        // 对齐官方 C++ JoinOrMeetTupleTy
        joinTupleTypes(filteredTypes, depth)?.let { return it }

        // 步骤七：通用路径 — 祖先交集 + 按构造器推断类型参数
        val commonConstructors = allCommonSuperTypeConstructors(filteredTypes, typeCheckerState)
        if (commonConstructors.isEmpty()) return c.anyType()

        // 步骤 7.5：可访问性过滤 — 对齐 C++ BatchJoin 的 IsTyAccessible
        val accessibleConstructors = commonConstructors.filter { it.isTypeAccessible() }
        if (accessibleConstructors.isEmpty()) return c.anyType()

        // 步骤八：为每个公共构造器推断带类型参数的具体超类型
        // 使用 mapNotNull：检测到递归的构造器返回 null，直接从候选中过滤掉
        val candidateTypes = accessibleConstructors.mapNotNull { constructor ->
            superTypeWithGivenConstructor(filteredTypes, constructor, typeCheckerState, depth)
        }

        // 步骤九：合并候选结果，尝试简化交叉类型
        // 对齐官方 C++ ToUserVisibleTy / FindSmallestTy
        return when (candidateTypes.size) {
            0 -> c.anyType()
            1 -> candidateTypes.single()
            else -> simplifyIntersection(candidateTypes, typeCheckerState)
        }
    }

    /**
     * 去重：从列表中移除类型相等的重复项。
     *
     * 使用 AbstractTypeChecker.equalTypes 进行语义相等判断（而非引用相等），
     * 因此 List<Int> 和另一个 List<Int> 实例会被视为相同并去重。
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun uniquify(
        types: List<RigidTypeMarker>,
        state: TypeCheckerState,
    ): List<RigidTypeMarker> {
        val uniqueTypes = ArrayList<RigidTypeMarker>(types.size)
        for (type in types) {
            if (uniqueTypes.none { existing -> AbstractTypeChecker.equalTypes(state, existing, type) }) {
                uniqueTypes += type
            }
        }
        return uniqueTypes
    }

    /**
     * 过滤严格父类型：从列表中移除那些"被其他类型包含（是其子类型）"的类型。
     *
     * 直觉：如果 A <: B，那么 B 在求 CST 时是冗余的，保留 A 即可表达更精确的公共类型。
     * 例如：输入 [ArrayList<Int>, List<Int>, Collection<Int>]，结果为 [ArrayList<Int>]
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun filterStrictSupertypes(
        types: List<RigidTypeMarker>,
        state: TypeCheckerState,
    ): List<RigidTypeMarker> {
        if (types.size <= 1) return types

        return types.filterNot { potentialSubtype ->
            types.any { candidateSupertype ->
                candidateSupertype !== potentialSubtype &&
                        AbstractTypeChecker.isSubtypeOf(state, potentialSubtype, candidateSupertype)
            }
        }
    }

    /**
     * 快速路径：在输入类型中直接找到一个是所有其他类型父类的类型。
     *
     * 对齐官方 C++ FindSmallestTy(realTys, isSupertype)：
     * 遍历输入类型，若某类型是所有其他类型的超类型，直接返回。
     * 这避免了不必要的祖先交集计算。
     *
     * 示例：[Dog, Animal] 中 Animal 是 Dog 的父类 → 直接返回 Animal
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun findSmallestSupertype(
        types: List<RigidTypeMarker>,
        state: TypeCheckerState,
    ): RigidTypeMarker? {
        for (candidate in types) {
            if (types.all { it === candidate || AbstractTypeChecker.isSubtypeOf(state, it, candidate) }) {
                return candidate
            }
        }
        return null
    }

    /**
     * 函数类型 Join 特化。
     *
     * 对齐官方 C++ JoinOrMeetFuncTy：
     * - 所有输入都是同参数数量的函数类型时，参数位逆变取交叉类型，返回值协变递归 CST
     * - 非全部为函数类型或参数数量不同时返回 null，继续后续通用路径
     *
     * @return 函数类型 Join 结果，或 null 表示不适用
     */
    // TODO: 实现完整 Meet（GLB），对齐官方 DualMode 设计，替换参数位的 intersectTypes 近似
    context(c: TypeSystemCommonSuperTypesContext)
    private fun joinFunctionTypes(
        types: List<RigidTypeMarker>,
        depth: Int,
    ): CangJieTypeMarker? {
        if (types.any { !(it as CangJieTypeMarker).isFunctionType() }) return null

        // 提取所有函数类型的参数列表（含返回值类型，最后一个为返回值）
        val allArgs = types.map { (it as CangJieTypeMarker).extractArgumentsForFunctionType() }
        val paramCount = allArgs.first().size - 1 // 最后一个是返回值类型

        // 参数数量不同时，无法特化，回退通用路径
        if (allArgs.any { it.size - 1 != paramCount }) return null

        // 参数位：逆变 → 取交叉类型（Meet 的近似）
        val joinedParams = (0 until paramCount).map { i ->
            val paramTypes = allArgs.map { it[i] }
            c.intersectTypes(paramTypes)
        }

        // 返回值位：协变 → 递归 CST
        val returnTypes = allArgs.map { it.last() }
        val joinedReturn = commonSuperTypeInternal(returnTypes, depth + 1)

        return c.createFunctionType(joinedParams, joinedReturn)
    }

    /**
     * 元组类型 Join 特化。
     *
     * 对齐官方 C++ JoinOrMeetTupleTy：
     * - 所有输入都是同元素数量的元组类型时，各分量协变递归 CST
     * - 非全部为元组类型或元素数量不同时返回 null，继续后续通用路径
     *
     * @return 元组类型 Join 结果，或 null 表示不适用
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun joinTupleTypes(
        types: List<RigidTypeMarker>,
        depth: Int,
    ): CangJieTypeMarker? {
        if (types.any { !(it as CangJieTypeMarker).isTupleType() }) return null

        // 提取所有元组类型的元素列表
        val allElements = types.map { (it as CangJieTypeMarker).extractElementsForTupleType() }
        val elementCount = allElements.first().size

        // 元素数量不同时，无法特化，回退通用路径
        if (allElements.any { it.size != elementCount }) return null

        // 各分量协变 → 递归 CST
        val joinedElements = (0 until elementCount).map { i ->
            val elementTypes = allElements.map { it[i] }
            commonSuperTypeInternal(elementTypes, depth + 1)
        }

        return c.createTupleType(joinedElements)
    }

    /**
     * 简化交叉类型：尝试从候选中找到最小子类型。
     *
     * 对齐官方 C++ ToUserVisibleTy / FindSmallestTy：
     * 若多个候选类型中存在子类型关系，直接返回最小的那个，
     * 避免生成不必要的交叉类型。
     *
     * 示例：候选为 [Animal, Serializable]，若 Animal <: Serializable，返回 Animal
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun simplifyIntersection(
        candidates: List<SimpleTypeMarker>,
        state: TypeCheckerState,
    ): CangJieTypeMarker {
        for (candidate in candidates) {
            if (candidates.all { it === candidate || AbstractTypeChecker.isSubtypeOf(state, candidate, it) }) {
                return candidate
            }
        }
        return c.intersectTypes(candidates)
    }

    /**
     * 计算所有输入类型共同拥有的最具体父类型构造器集合。
     *
     * 算法：
     *   1. 收集第一个类型的全部祖先构造器作为初始集合
     *   2. 对剩余每个类型，与其祖先构造器集合取交集
     *   3. 移除被其他公共构造器继承的冗余构造器，只保留最具体的叶子构造器
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun allCommonSuperTypeConstructors(
        types: List<RigidTypeMarker>,
        state: TypeCheckerState,
    ): List<TypeConstructorMarker> {
        val commonConstructors = LinkedHashSet(collectAllSupertypes(types.first(), state))
        for (type in types.drop(1)) {
            commonConstructors.retainAll(collectAllSupertypes(type, state))
        }

        return commonConstructors.filterNot { target ->
            commonConstructors.any { other ->
                other != target && other.supertypes().any { supertype ->
                    supertype.typeConstructor() == target
                }
            }
        }
    }

    /**
     * 收集某个类型的所有祖先类型构造器（包含自身）。
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun collectAllSupertypes(
        type: RigidTypeMarker,
        state: TypeCheckerState,
    ): Set<TypeConstructorMarker> {
        val result = LinkedHashSet<TypeConstructorMarker>()
        state.anySupertype(
            type,
            predicate = {
                result += it.typeConstructor()
                false
            },
            supertypesPolicy = { TypeCheckerState.SupertypesPolicy.Direct },
        )
        return result
    }

    /**
     * 给定一个类型构造器，推断出以该构造器为头部的具体超类型（含类型参数）。
     * 若检测到递归则返回 null，由调用方将该构造器从候选中丢弃。
     *
     * 递归安全保障（两道防线）：
     *
     *   防线一：depth > 0
     *     递归深度超过初始预算（输入类型的最大嵌套层数），丢弃该构造器。
     *     典型场景：复杂多层泛型嵌套，如 Map<List<Set<Dog>>, ...>
     *
     *   防线二：checkRecursion
     *     提取出的类型参数集合与本次 CST 的原始输入集合实质相同，
     *     说明再次递归必然得到相同输入，丢弃该构造器。
     *     典型场景：
     *       - 基本类型实现泛型接口：BigEndianOrder<Bool> + BigEndianOrder<Int64>
     *         提取参数得到 [Bool, Int64]，与原始输入 [Bool, Int64] 相同 → 丢弃
     *       - 自引用泛型约束：interface Some<T : Some<T>>
     *         展开后参数仍是同一批类型 → 丢弃
     *
     * @param types 输入的类型列表
     * @param constructor 目标类型构造器
     * @param state 类型检查器状态
     * @param depth 当前递归深度预算
     * @return 以 constructor 为头部的具体超类型，检测到递归时返回 null
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun superTypeWithGivenConstructor(
        types: List<RigidTypeMarker>,
        constructor: TypeConstructorMarker,
        state: TypeCheckerState,
        depth: Int,
    ): SimpleTypeMarker? {
        if (constructor.parametersCount() == 0) {
            return c.createSimpleType(constructor, emptyList())
        }

        val correspondingSupertypes = types.flatMap { type ->
            AbstractTypeChecker.findCorrespondingSupertypes(state, type, constructor)
        }
        if (correspondingSupertypes.isEmpty()) {
            return c.createSimpleType(constructor, emptyList())
        }

        val arguments = buildList(constructor.parametersCount()) {
            for (index in 0 until constructor.parametersCount()) {
                val candidateArguments = correspondingSupertypes.mapNotNull { it.getArgumentOrNull(index) }
                val candidateTypes = candidateArguments.mapNotNull { it.getType() }

                val argumentType = when {
                    candidateTypes.isEmpty() -> c.anyType()

                    candidateTypes.size == 1 -> candidateTypes.single()

                    // 防线一：深度超限，丢弃整个构造器
                    depth > 0 -> return null

                    // 防线二：检测到自引用递归，丢弃整个构造器
                    checkRecursion(types, candidateTypes) -> return null

                    // 安全递归：depth + 1 消耗深度预算
                    else -> commonSuperTypeInternal(candidateTypes, depth + 1)
                }

                add(c.createTypeArgument(argumentType))
            }
        }

        return c.createSimpleType(constructor, arguments)
    }

    /**
     * 检测类型参数是否与原始输入构成自引用循环。
     *
     * 判断条件：candidateTypes 的构造器集合与 originalTypes 的构造器集合完全相同。
     *
     * 原理：如果提取出的类型参数在构造器层面与原始输入完全一致，
     * 再次递归必然得到相同输入，会陷入死循环。
     *
     * 示例一（基本类型实现泛型接口）：
     *   originalTypes  = [Bool, Int64]
     *   candidateTypes = [Bool, Int64]  ← BigEndianOrder<Bool/Int64> 提取出的参数
     *   构造器集合相同 → 返回 true，触发丢弃
     *
     * 示例二（自引用泛型约束）：
     *   interface Some<T : Some<T>>
     *   originalTypes  = [SomeImpl1, SomeImpl2]
     *   candidateTypes ≈ [SomeImpl1, SomeImpl2]
     *   构造器集合相同 → 返回 true，触发丢弃
     */
    context(c: TypeSystemCommonSuperTypesContext)
    private fun checkRecursion(
        originalTypes: List<RigidTypeMarker>,
        candidateTypes: List<CangJieTypeMarker>,
    ): Boolean {
        if (originalTypes.size != candidateTypes.size) return false

        val originalConstructors = originalTypes.mapTo(mutableSetOf()) { it.typeConstructor() }
        val candidateConstructors = candidateTypes.mapNotNull { it.asRigidType()?.typeConstructor() }.toSet()

        return originalConstructors == candidateConstructors
    }
}