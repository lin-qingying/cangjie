package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.Name

/**
 * 宏原始位置的词法构建事实，供 PSI 与 LightTree 重解析恢复同一外层环境。
 * 列表保持只读，外层符号与 return target 是非 owning 引用，新声明始终创建新符号。
 * parser 模块与语法临时节点属于新的解析树，不进入该词法快照。
 */
data class MacroRawScopeSnapshot(
    /** 声明是否位于当前局部语境，区别于局部类内部的成员语境。 */
    val inLocalContext: Boolean,
    /** 原始位置的完整容器符号栈。 */
    val containerSymbols: List<CfirBasedSymbol<*>>,
    /** 原始位置的泛型 class-like self type 栈。 */
    val dispatchReceiverTypes: List<ConeSimpleCangJieType>,
    /** 当前可绑定的完整函数 target 栈。 */
    val functionTargets: List<CfirFunctionTarget>,
    /** 原始位置的 label 栈。 */
    val labels: List<Name>,
    /** 特殊 raw 构建入口实际采用的最外层容器覆盖。 */
    val forcedContainerSymbol: CfirBasedSymbol<*>?,
) {
    companion object {
        /** 无外层声明的包级构建环境。 */
        val ROOT: MacroRawScopeSnapshot = MacroRawScopeSnapshot(
            false, emptyList(), emptyList(), emptyList(), emptyList(), null,
        )
    }
}
