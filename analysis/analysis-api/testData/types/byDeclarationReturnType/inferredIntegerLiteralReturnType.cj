// FILE: inferredIntegerLiteralReturnType.cj
package sample.types.returntype

// 省略返回类型：整数字面量在无更强上下文时按仓颉默认规则推导为 Int64。
func infer<caret>redIntegerLiteral() {
    return 1
}
