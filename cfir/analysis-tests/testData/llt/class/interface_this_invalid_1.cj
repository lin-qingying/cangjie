


interface I0 {
    static func foo(): Int64 {
        return 1
    }

    func foo1(): Int64 {
        var res = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>this<!>.foo()
        return res
    }
}

class C0 <: I0 {}

main(): Int64 {
    return 0
}
