// 词法组合边界：标识符与关键字前缀（lets / varsity / structX 不是关键字）、
// 点号与区间混排（s.size、1..2、f..g）、模板插值嵌套。
// 官方依据：Cangjie 文档《标识符》《字面量》；cjc 1.0.5 / 1.1.3 零诊断。
class Holder2 {
    var c: Int64 = 1

    public init() {}
}

func boundaries(): Unit {
    let lets = 1
    let varsity = 2
    let structX = 3
    let classy = 4
    let functional = 5
    let _ = lets + varsity + structX + classy + functional
    let b = Holder2()
    let a = b.c
    let f = 1
    let g = 2
    let d = 1..2
    let e = f..g
    let h = "x${a}y"
    let i = 0
    let j = 1
    let k = 2
    let _ = a
    let _ = d
    let _ = e
    let _ = h
    let _ = i
    let _ = j
    let _ = k
}
