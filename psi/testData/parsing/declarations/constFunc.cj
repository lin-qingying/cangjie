// const 函数：编译期求值函数的声明与调用。
// 官方依据：Cangjie 文档《const 函数》；cjc 1.0.5 / 1.1.3 零诊断。
const func double(x: Int64): Int64 {
    return x * 2
}

const func maxConst(a: Int64, b: Int64): Int64 {
    if (a > b) {
        return a
    }
    return b
}

func useConst(): Int64 {
    return double(2) + maxConst(1, 2)
}
