
// 泛型返回类型约束推断测试
func identity<T>(x: T): T {
    return x
}

main() {
    let a: Int64 = identity(42)

}
