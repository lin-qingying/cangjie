package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.AbstractTypePreparator
import org.cangnova.cangjie.type.AbstractTypeRefiner
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.TypeCheckerProviderContext

/**
 * 类型检查工具类。
 * 提供子类型判断和类型渲染能力，供各类 checker 复用。
 *
 * 使用 [AbstractTypeChecker] 进行子类型判断，配合基础的
 * [TypeCheckerProviderContext]，避免形成 `checkers -> resolve` 的循环依赖。
 */
object CfirTypeCheckUtils {

    /** 判断 [subType] 是否为 [superType] 的子类型。 */
    fun isSubtypeOf(subType: ConeCangJieType, superType: ConeCangJieType): Boolean =
        AbstractTypeChecker.isSubtypeOf(BasicTypeCheckerProviderContext, subType, superType)

    /** 将类型渲染为可读字符串。 */
    fun renderType(type: ConeCangJieType): String = type.toString()

    /**
     * 基础类型检查器提供上下文。
     * 仅支持内建规则，不提供超类型图遍历；类继承关系由后续增强补齐。
     */
    private object BasicTypeCheckerProviderContext : TypeCheckerProviderContext {
        override fun newTypeCheckerState(
            errorTypesEqualToAnything: Boolean,
            stubTypesEqualToAnything: Boolean,
        ): TypeCheckerState = TypeCheckerState(
            isErrorTypeEqualsToAnything = errorTypesEqualToAnything,
            isStubTypeEqualsToAnything = stubTypesEqualToAnything,
            allowedTypeVariable = false,
            typeSystemContext = BasicTypeSystemContext,
            cangjieTypePreparator = AbstractTypePreparator.Default,
            cangjieTypeRefiner = AbstractTypeRefiner.Default,
        )
    }

    /**
     * 最小化 TypeSystemContext 实现。
     * 仅满足 [TypeCheckerState] 的构造需求，不提供超类型遍历。
     */
    private object BasicTypeSystemContext : org.cangnova.cangjie.type.model.TypeSystemContext
}