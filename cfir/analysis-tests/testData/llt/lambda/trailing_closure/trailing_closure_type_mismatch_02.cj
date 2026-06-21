


func test(a: Any) {}

main(): Int64 {
    test <!TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION!>{=> 1}<!>
    return 0
}
