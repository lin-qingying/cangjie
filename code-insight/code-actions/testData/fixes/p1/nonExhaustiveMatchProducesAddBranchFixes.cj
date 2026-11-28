// NON_EXHAUSTIVE_MATCH：枚举 match 缺少分支。
package quickfix.p1

enum Color {
    Red | Green | Blue
}

func pick(c: Color): Int64 {
    match(c) {
        case Red => 1
        case Green => 2
    }
}
