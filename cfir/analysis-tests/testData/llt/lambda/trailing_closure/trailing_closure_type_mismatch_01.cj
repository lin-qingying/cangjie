


func test<T>(a: T) {}

main(): Int64 {
    test<Any> <!TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION!>{=> 1}<!>
    return 0
}
