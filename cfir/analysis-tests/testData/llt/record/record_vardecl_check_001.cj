

struct R {
    public var a: C = C()
}

struct C {
    public var m: Int64 = 1
}

main(): Int64 {
    let r = R()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>r.a.m<!> = 2
    return 0
}
