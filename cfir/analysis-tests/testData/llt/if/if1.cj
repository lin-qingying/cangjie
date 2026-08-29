


func foo(): Int32 {
    if (false) {
        return <!CANNOT_CONVERT_LITERAL!>2.6<!>
    } else {
        var a: Int32 = 1
        return a
    }
    return 0
}
