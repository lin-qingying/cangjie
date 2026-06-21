

// Should reporting type mismatch, not just invalid subscript expr
func test(a: Array<Int8>): Unit {}

main() {
    var arr: Array<Array<UInt64>> = [[255u64, 0u64, 0u64, 0u64]]
    var a = test(<!TYPE_MISMATCH!>arr[0]<!>)
}
