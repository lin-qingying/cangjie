


interface I {
    static func foo() {
        return 1
    }
}

func Foo<U>(a: U) where U <: I & C1 {
    var b = U.foo()
    return b
}

class C1 <: I {
    public static func foo() {
        return 0
    }
}

main() {
    return Foo<C1>(C1())
}
