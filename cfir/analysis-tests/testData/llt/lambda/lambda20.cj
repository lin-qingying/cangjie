


func test(): Int64 {
    let assign = {a: Int64 => <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 0}
    return 0
}

main() {
    return test()
}
