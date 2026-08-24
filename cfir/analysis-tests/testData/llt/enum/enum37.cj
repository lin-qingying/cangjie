

func test1(v: Any) {}

func test2(v: ToString) {}

main() {
    let x = <!UNABLE_TO_INFER_GENERIC_FUNC!>Some<!>(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!>)
    test1(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!>)
    test2(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!>)
    0
}
