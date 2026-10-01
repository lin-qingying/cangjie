// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind sema_wrong_number_of_arguments）：
//   `g(1)`（g 有两个参数）→ error: missing argument for parameter list '(Int64, Int64)' in call
//     （MainHint expected 2 arguments, found 1；锚实参列表 `(1)`）
//   `f(1, 2)`（f 有一个参数）→ error: extra argument given for parameter list '(Int64)' in call
//     （MainHint expected 1 argument, found 2；锚实参列表 `(1, 2)`）
// 本项目：两条都用同一个面 CFIR 的 WRONG_NUMBER_OF_ARGUMENTS 表达。
// 官方另有 sema_no_value_for_parameter / sema_too_many_arguments 形式的 kind（v1.0.0 的 .def 里没有同义名），
// 语义对应本项目的 NO_VALUE_FOR_PARAMETER / TOO_MANY_ARGUMENTS，当前没有可达输入。
func g(a: Int64, b: Int64): Int64 {
    return a + b
}

func f(a: Int64): Int64 {
    return a
}

main(): Unit {
    let _ = g<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    let _ = f<!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2)<!>
}
