

func test(): Int64 {
    var i: Int32 = 0
    var res = match (i++) {
        case <!CANNOT_CONVERT_LITERAL!>0<!> => <!UNRESOLVED_REFERENCE!>res<!> = 1
        case <!CANNOT_CONVERT_LITERAL!>1<!> => <!UNRESOLVED_REFERENCE!>res<!> = 0
        case _ => <!UNRESOLVED_REFERENCE!>res<!> = -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
