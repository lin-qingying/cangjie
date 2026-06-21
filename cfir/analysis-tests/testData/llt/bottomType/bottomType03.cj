


let one: Int32 = 1
let two: Int32 = 2

func foo() {
    return 1
    let a = one
    let b = two
    <!INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE!>r<!>eturn a + b
}

main(): Int64 {
    foo()
    return 0
}
