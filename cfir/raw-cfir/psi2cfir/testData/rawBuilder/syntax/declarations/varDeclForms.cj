// RUN_PIPELINE_TILL: FRONTEND
// 变量声明：let / var、带类型标注 / 不带、省略初始化、多值（解构）声明、带模式的声明、通配符。
// 官方依据：Cangjie 文档《变量声明》；cjc 1.0.5 / 1.1.3 零诊断。
func varDeclForms(): Unit {
    let a = 1
    let b: Int64 = 2
    let c: Int64
    var d = 3
    var e: Int64 = 4
    var f: String
    let (p, q) = (1, 2)
    var (r, s) = (3, 4)
    let (t, _) = (5, 6)
    var _: Int64 = 7
    c = 8
    f = "x"
    let _ = (a, b, c, d, e, f, p, q, r, s, t)
}
