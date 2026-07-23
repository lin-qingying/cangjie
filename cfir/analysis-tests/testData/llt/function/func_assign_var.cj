

open class Base {}

class Sub <: Base {
    public let f = {x: Sub => 3}
    public func ff(x: Sub, y!: Int32 = 0): Int32 {
        4
    }
    public func ff(x: Sub): Int32 {
        5
    }
}

func a(x: Base) {
    return Base()
}

func a(x: Sub): Int32 {
    return 2
}

func a(x: Int32, y: Int32): Int32 {
    return x + y
}

func codegenUnitTest(): Int64 {
    var x: (Int64) -> Int32 = <!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>a<!>
    var b = Sub().<!AMBIGUOUS_USE!>ff<!>
    var z = <!AMBIGUOUS_USE!>a<!>
    return 0
}

main(): Int64 {
    return codegenUnitTest()
}
