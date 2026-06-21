

interface Data2 {
    func foo(a: Int64): Int64 {
        return 2
    }
}

interface Data3 <: Data2 {}

open class Test1 <: Data2 {
    public func foo(): Int64 {
        return 3
    }
}

class Test <: Test1 & Data2 & Data3 {}

main(): Int64 {
    var a = Test().<!AMBIGUOUS_USE!>foo<!>
    return 0
}
