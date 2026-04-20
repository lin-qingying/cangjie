package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers

/**
 * 对齐 Kotlin `CommonIdeOnlyDeclarationCheckers`。
 *
 * 仓颉当前主干还没有仅 IDE 运行、且必须与 CLI 隔离的声明 checker，
 * 因此该容器先只承担结构角色，供 low-level diagnostics 工厂按 Kotlin 同构路径组装。
 */
object CommonIdeOnlyDeclarationCheckers : DeclarationCheckers()
