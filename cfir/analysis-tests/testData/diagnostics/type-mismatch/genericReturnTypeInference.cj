
// 泛型返回类型约束推断测试
func identity<T>(x: T): T {
    return x
}

main(): Unit {
    identity(42)
}
