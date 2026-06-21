


interface I {
    static func foo() {
        return 0
    }
}

func Foo<U>(a: U) where U <: I & C1 {
    var b = U.foo()
    return b
}

class C1 <: I {}

main() {
    return Foo<C1>(C1())
}
