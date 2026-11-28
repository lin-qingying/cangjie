// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测：`return -"abc"`（一元 `-` 作用于 String，结果类型 Int64）→
// error: invalid unary operator '-' on type 'Struct-String' with return type 'Int64'
//   （kind sema_invalid_unary_expr_with_target，锚整个一元表达式 `-"abc"`）。
// 本项目：官方 kind 对应 INVALID_UNARY_EXPR_WITH_TARGET；CFIR 对该形态报通用 INVALID_UNARY_EXPR（锚点相同），属实现缺口。
func negateString(): Int64 {
    return <!INVALID_UNARY_EXPR_WITH_TARGET!>-"abc"<!>
}
