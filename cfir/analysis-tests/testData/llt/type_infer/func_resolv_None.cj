

func foo(a: Int64): Unit {}

func foo(a: Bool): Unit {}

main(): Int64 {
    foo(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!> is Int)
    println(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!> is Int)
    return 0
}
