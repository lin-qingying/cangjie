


open class C {
    private static let a = 0
}

func Foo<U>(a: U) where U <: C {
    var b = <!INVALID_ACCESS_CONTROL!>U<!>.a
    return b
}

class C1 <: C {}

main() {
    return Foo<C1>(C1())
}
