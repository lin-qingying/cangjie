// RUN_PIPELINE_TILL: FRONTEND
// 控制流表达式形态：if 表达式直接作为实参 / 嵌套实参、if-let-else / if-let-else-if 链、while 条件里的 let 模式（配合 Option 变量推进）、
// for 的元组解构模式与单变量遍历区间。
// 官方依据：Cangjie 文档《控制流》；官方 ParseExpr.cpp ParseIfExpr / ParseLetPattern / ParseForInExpr；cjc 1.0.5 / 1.1.3 零诊断。
func g1(x: Int64): Int64 {
    return x
}

func g3(x: Int64, y: Int64, z: Int64): Int64 {
    return x + y + z
}

func controlFlowExprForms(a: Int64, b: Int64, c: Int64, o: ?Int64, pairs: Array<(Int64, String)>): Int64 {
    let one = g1(if (a > 0) { 1 } else { 2 })
    let three = g3(if (a > 0) { 1 } else { 2 }, b, if (c > 0) { 3 } else { 4 })
    let chain = if (let Some(v) <- o) {
        v
    } else if (b > 0) {
        b
    } else {
        0
    }
    var t = 0
    var n = o
    while (let Some(v) <- n) {
        t += v
        n = None
    }
    for ((k, v) in pairs) {
        t += k + v.size
    }
    for (i in 1..3) {
        t += i
    }
    return one + three + chain + t
}
