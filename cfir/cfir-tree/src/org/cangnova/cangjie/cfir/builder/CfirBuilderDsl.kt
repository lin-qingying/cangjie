package org.cangnova.cangjie.cfir.builder

/**
 * CFIR builder DSL 的作用域标记。
 *
 * 该标记限制嵌套 builder 中的隐式接收者解析，避免构建 CFIR 树时把字段写到错误层级。
 */
@DslMarker
annotation class CfirBuilderDsl
