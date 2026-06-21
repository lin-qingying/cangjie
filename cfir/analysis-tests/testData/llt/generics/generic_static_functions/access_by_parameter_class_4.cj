


open class C {
    public static func foo() {
        return 1
    }
}

func Foo<U>(a: U) where U <: C & C1 {
    var b = U.foo() // access foo in C1
    return b
}

class C1 <: C {
    public static redef func foo() {
        return 0
    }
}

main() {
    return Foo<C1>(C1())
}
