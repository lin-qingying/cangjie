// 变量：顶层与局部 let / var / const、类型标注、元组解构与模式变量。
// 官方依据：Cangjie 文档《变量》《常量》；cjc 1.0.5 / 1.1.3 零诊断。
let PI: Float64 = 3.14
var count: Int64 = 0
const LIMIT: Int64 = 100

var (a, b, c) = (1, 2, 3)

func locals(): Int64 {
    let immutable = 1
    var mutable = 2
    let typed: Int64 = 3
    let (x, y) = (4, 5)
    var total = immutable + mutable + typed + x + y
    total += count
    count = total
    return total
}

func shadowing(): Int64 {
    let v = 1
    let v2 = v + 1
    return v2
}

class Fields {
    private let id: Int64 = 0
    private var name: String = "field"
    public static const SIZE: Int64 = 8
}
