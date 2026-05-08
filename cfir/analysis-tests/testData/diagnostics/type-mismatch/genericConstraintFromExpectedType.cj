
func identity<T>(value: T): T {
    return value
}

main(): Unit {
    let _: Int64 = <!NEW_INFERENCE_ERROR!>identity<!>("hello")
}
