


enum ET {
    A
    | B(String)
    | C(String, String)

    public func foo1(): Int64 {
        print("function foo1 defined in enum\n")
        return 1
    }

    public func foo1(a: Int64): Int64 {
        print("function foo1(Int64) defined in enum\n")
        return 1
    }

    private func foo2(): Int64 {
        print("function foo2 defined in enum\n")
        return 1
    }

    public static func foo3(): Int64 {
        print("function foo3 defined in enum\n")
        return 1
    }
}

main() {
    let e1: ET = ET.A
    let e2: ET = ET.B("one")
    e1.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>f<!>oo2()
    <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>e2<!>.foo3()
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>ET<!>.foo1()
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>ET<!>.foo1(1)

    return 0
}
