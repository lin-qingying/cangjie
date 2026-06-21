

interface Data2 {
    static func foo(): Int64 {
        return 2
    }
}

interface Data3 <: Data2 {}

open class Test1 <: Data2 {
    private static func foo(a: Int64): Int64 {
        return 3
    }
    public func f(): Int64 {
        <!AMBIGUOUS_USE!>foo<!>
        return 0
    }
}

class Test <: Test1 & Data2 & Data3 {}

main(): Int64 {
    return 1
}
