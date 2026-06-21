

func foo(): Unit {}

main(): Unit {
    let a: Int64 = UInt16({a: Int64 => <!TOO_MANY_ARGUMENTS!>foo(1, 2)<!>}(1)) & 1
}
