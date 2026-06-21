


main(): Int64 {
    return 0
}

abstract class Boo {
    private mut prop <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>l<!>: Int32
    <!MISSING_FUNC_BODY!>p<!>ublic mut static prop m: Int32
}
