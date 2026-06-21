

open class Base {}

class Child <: Base {}

class A {
    public static func a<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, K>(x: T, b: K): Bool where T <: K {
        return true
    }
}

main(): Int64 {
    var obj = A()
    if (A.a<Child, Base>(Child(), Base()) != true) {
        return 1
    }
    return 0
}
