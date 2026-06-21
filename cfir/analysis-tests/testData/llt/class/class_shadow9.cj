


open class Base {
    public let val: Int32 = 0
}

class Data <: Base {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic let val: Int32 = 1
    public let f: Int32 = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.val
}

main(): Int64 {
    var data = Data()
    var d = data.val
    var f = data.f
    if (d != 1 || f != 0) {
        return 1
    }
    return 0
}
