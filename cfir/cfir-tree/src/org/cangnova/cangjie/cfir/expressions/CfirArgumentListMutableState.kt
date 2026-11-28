package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.expressions.impl.CfirArgumentListImpl

/**
 * 实参列表真正拥有的可变状态。
 *
 * 原始列表的元素会原地替换，已解析列表还拥有实参到形参的映射；
 * 保存列表节点本身不足以隔离一次候选或资源类型试算。
 */
interface CfirArgumentListMutableState {
    /** 恢复原列表元素、形参映射及原有映射别名关系。 */
    fun restore()

    companion object {
        /** 各列表实现负责保存自己的真实存储，避免向派生的 arguments 视图写入。 */
        fun capture(argumentList: CfirArgumentList): CfirArgumentListMutableState = when (argumentList) {
            is CfirResolvedArgumentList -> argumentList.captureMutableState()
            is CfirArgumentListImpl -> {
                val arguments = argumentList.arguments
                val originalArguments = arguments.toList()
                object : CfirArgumentListMutableState {
                    override fun restore() {
                        arguments.clear()
                        arguments.addAll(originalArguments)
                    }
                }
            }
            is CfirAbstractArgumentList -> ImmutableArgumentListState
            else -> error("Unsupported mutable argument list: ${argumentList::class.qualifiedName}")
        }
    }
}

/** 不参与参数节点变换的列表没有可变子节点状态。 */
private object ImmutableArgumentListState : CfirArgumentListMutableState {
    override fun restore() {}
}
