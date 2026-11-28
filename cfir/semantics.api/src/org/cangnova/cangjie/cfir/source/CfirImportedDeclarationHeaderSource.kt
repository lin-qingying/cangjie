package org.cangnova.cangjie.cfir.source

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * 声明首次构造时绑定的库 header 内容拥有者。读取 snapshot 仍是被动操作；
 * 显式完成仅在原 package context 执行，保留声明/符号/参数身份，不授权正文或变量初值。
 */
interface CfirImportedDeclarationHeaderSource : CfirContentSource.Published {
    /** 完成原声明的正常 annotation/default header family；非本 context 的声明和材料化失败必须传播。 */
    fun completeHeader(declaration: CfirDeclaration)

    /** 冻结输入消费者的只读区域；本 owner 的任何内容事务均不得在 action 中启动。 */
    fun <T> withReadOnlyContent(action: () -> T): T
}
