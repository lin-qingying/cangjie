


open class C {
    public static func foo() {
        return 0
    }
}

func Foo<U>(a: U) where U <: C {
    var b = U.foo()
    return b
}

class C1 <: C {}

main() {
    return Foo<C1>(C1())
}
