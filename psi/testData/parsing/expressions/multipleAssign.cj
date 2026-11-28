// 多元赋值：元组解构赋值与元组变量整体赋值。
// 官方依据：Cangjie 文档《元组类型》；cjc 1.0.5 / 1.1.3 零诊断。
func multipleAssign(): Unit {
    var a = 1
    var b = 2
    (a, b) = (b, a)
    var t: (Int64, Int64) = (1, 2)
    t = (3, 4)
    var x = 0
    var y = 0
    (x, y) = (1, 2)
    let _ = (a, b, t, x, y)
}
