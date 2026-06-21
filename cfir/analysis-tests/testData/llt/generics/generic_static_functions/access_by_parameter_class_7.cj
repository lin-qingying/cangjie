


open class C {
    public let a = 0
}

func Foo<U>(a: U) where U <: C {
    var b = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>U<!>.a
    return b
}

class C1 <: C {}

main() {
    return Foo<C1>(C1())
}
