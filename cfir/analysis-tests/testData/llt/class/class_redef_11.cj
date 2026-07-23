


main(): Int64 {
    if (Coo.foo<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Int32<!>>(1, 2) != 2 || Boo<Int32>.foo<Int32>(3, 4) != 4) {
        return 1
    }
    return 0
}

open class Boo<T> {
    public static func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>>(x: T, y: V) where V <: Int32 {
        return y
    }
}

class Coo <: Boo<Int32> {
    public static redef func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>>(x: Int32, y: V) where <!GENERIC_CONSTRAINT_NOT_LOOSER!>V <: Int64<!> {
        return y
    }
}
