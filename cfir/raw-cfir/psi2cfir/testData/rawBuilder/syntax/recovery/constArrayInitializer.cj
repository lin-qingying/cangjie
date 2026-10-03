// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：const 声明用数组字面量（官方 sema_expect_const "expected 'const' expression guaranteed to be evaluated at compile time"，锚数组字面量）。
// 官方 cjc 1.0.5 / 1.1.3：sema_expect_const。
const A: Array<Int64> = [1, 2]
