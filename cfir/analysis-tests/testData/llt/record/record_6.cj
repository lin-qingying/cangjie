

struct Base12 {
    public var du: Int64 = 1
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public prop a: Int64 {
    }<!>
    public func f(): Int64 {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = 2
        return 0
    }
}

main() {
    var x = Base12()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>x.a<!> = 4
    if (x.a == 4) {
        print("${x.a}")
        return 0
    }
    return 1
}
