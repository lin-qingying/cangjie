


func test(): Int64 {
    let myLambda = {
        i_in: Int64 =>
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>i_in<!> = <!UNRESOLVED_REFERENCE!>i_out<!>
        return i_in
    }
    return 1
}

main(): Int64 {
    var i_out: Int64 = 0
    return test()
}
