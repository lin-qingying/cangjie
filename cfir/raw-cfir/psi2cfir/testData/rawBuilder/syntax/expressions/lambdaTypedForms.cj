// RUN_PIPELINE_TILL: FRONTEND
// lambda 与函数类型的标注：无参 / 单参 / 多参 / 具名形参（类型别名里写 `x:`）、无返回值的 lambda（返回 Unit）、函数类型变量初始化。
// 官方依据：Cangjie 文档《函数表达式》；官方 ParseExpr.cpp ParseLambdaExpr；cjc 1.0.5 / 1.1.3 零诊断。
func lambdaTypedForms(): Unit {
    let a: (Int64) -> Int64 = { x => x + 1 }
    let b: () -> Int64 = { => 1 }
    let c: (Int64, Int64) -> Int64 = { x, y => x + y }
    let d: (x: Int64, y: Int64) -> Int64 = { x, y => x + y }
    let g: (Int64) -> Unit = { _ => }
    let _ = (a, b, c, d, g)
}
