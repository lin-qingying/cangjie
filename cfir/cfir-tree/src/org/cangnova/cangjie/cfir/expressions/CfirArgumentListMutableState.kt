package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.expressions.impl.CfirArgumentListImpl

/** 实参列表的元素和已解析映射会原地变化，恢复责任属于各自的树模型 owner。 */
interface CfirArgumentListMutableState {
    fun restore()

    companion object {
        /** 捕获真实列表与映射的状态；不重建表达式或形参身份。 */
        fun capture(arguments: CfirArgumentList): CfirArgumentListMutableState = when (arguments) {
            is CfirResolvedArgumentListImpl -> arguments.captureMutableState()
            is CfirResolvedArgumentListForErrorCall -> arguments.captureMutableState()
            is CfirArgumentListImpl -> {
                val values = arguments.arguments.toList()
                object : CfirArgumentListMutableState {
                    override fun restore() {
                        arguments.arguments.clear()
                        arguments.arguments.addAll(values)
                    }
                }
            }
            CfirEmptyArgumentList -> object : CfirArgumentListMutableState {
                override fun restore() {}
            }
            else -> error("Unknown mutable argument list ${arguments::class.simpleName}")
        }
    }
}
