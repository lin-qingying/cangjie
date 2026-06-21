


func foo(): Int32 {
    if (false) {
        return <!RETURN_TYPE_MISMATCH!>2.6<!>
    } else {
        var a: Int32 = 1
        return a
    }
    return 0
}
