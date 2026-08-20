
func identity<T>(value: T): T {
    return value
}

main(): Unit {
    let _: Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>identity<!>("hello")
}
