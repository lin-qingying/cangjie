


open class C {
    public static let a = 0
}

func Foo<U>(a: U) where U <: C {
    var b = U.a
    return b
}

class C1 <: C {}

main() {
    return Foo<C1>(C1())
}
