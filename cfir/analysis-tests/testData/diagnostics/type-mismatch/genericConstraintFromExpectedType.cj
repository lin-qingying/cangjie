
func identity<T>(value: T): T {
    return value
}

main() {
    let number: Int64 <!PATTERN_INITIALIZER_TYPE_MISMATCH!>=<!> identity("hello")
}
