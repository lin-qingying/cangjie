


func test(a: (Int64) -> Int32) {}

main() {
    test({<!PARAM_COUNT_MISMATCH!>i, j<!> => <!TYPE_MISMATCH!>()<!>})
    test(<!TYPE_MISMATCH!>{i: Int64, j: Rune =>}<!>)
}
