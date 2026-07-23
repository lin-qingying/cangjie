

func foo(): Unit {}

main(): Unit {
    let a: Int64 = UInt16({a: Int64 => foo<!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2)<!>}(1)) & 1
}
