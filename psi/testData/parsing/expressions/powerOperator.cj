// 幂运算符：** 的优先级、结合性与重载声明。
// 官方依据：Cangjie 文档《运算符》；cjc 1.0.5 / 1.1.3 零诊断。
func powerForms(a: Int64, b: UInt64): Int64 {
    let p = a ** b
    let q = a ** b + a
    let r = (a + 1) ** b
    return p + q + r
}

class P {
    public init() {}

    public operator func **(o: P): P {
        return o
    }
}
