package org.cangjie.cfir.builder

import org.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.name.FqName

/**
 * Raw CFIR 构建上下文（对齐 Kotlin 的 Context<T>）。
 *
 * 泛型参数 T 为源码树节点类型（PsiElement 或 LighterASTNode），
 * 在源码 → Raw CFIR 转换过程中，维护编译状态：
 * - 当前包名
 * - 是否在局部上下文中
 * - 循环/函数/标签目标栈（后续补充）
 *
 * 注意：session 和 moduleData 在 AbstractRawCfirBuilder 中，与 Kotlin 一致。
 * 注意：仓颉没有嵌套类，不需要 className 栈。
 */
class Context<T> {
    /** 当前包的全限定名 */
    lateinit var packageFqName: FqName

    /** 是否处于局部上下文（局部函数内部） */
    var inLocalContext: Boolean = false

    val arraySetArgument: MutableMap<T, CfirExpression> = mutableMapOf()

}
