// RUN_PIPELINE_TILL: FRONTEND
// 括号与作用域：括号表达式 / 多层括号 / 跨行续行、元组字面量与括号的区别、单元调用 f()、条件语句里的作用域块、块内变量遮蔽。
// 官方依据：Cangjie 文档《表达式》《元组》；cjc 1.0.5 / 1.1.3 零诊断（仓颉没有独立的花括号块语句，块只能挂在 if / for / while 等结构上）。
func noArgs(): Int64 {
    return 1
}

func blockAndParenForms(): Int64 {
    var a = 1
    let b = ((1 + 2))
    let c = (
        1 +
            2
    )
    let d = (
        1,
        2
    )
    let e = noArgs()
    if (true) {
        let inner = 1
        if (true) {
            let deeper = 2
            a += deeper + inner
        }
    }
    var x = 0
    if (true) {
        x = 1
    } else {
        x = 2
    }
    let (t0, t1) = d
    return a + b + c + t0 + t1 + e + x
}
