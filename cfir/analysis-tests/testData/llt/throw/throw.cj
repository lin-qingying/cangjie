


class MyException <: Exception {}

main(): Int64 {
    throw Exception() // ok
    throw MyException() // ok
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> 1
    return 0
}
