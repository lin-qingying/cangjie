
// Lambda 参数类型推断测试
func applyFunc(f: (Int64) -> Int64, x: Int64): Int64 {
    return f(x)
}

main() {
    let result = applyFunc({ a => a + 1 }, 10)
}
