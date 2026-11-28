


main(): Int64 {
    return 0
}

abstract class Boo {
    private mut prop <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>l<!>: Int32
    public mut static <!MISSING_FUNC_BODY!>prop m: Int32<!>
}
