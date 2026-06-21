

class A<B> {
    func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>C<!>>(): C where C <: B {
        throw Exception("no impl")
    }
}

func bar<B, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>C<!>>(a: A<B>): C where C <: B {
    throw Exception("no impl")
}

interface Super {}

interface Sub <: Super {}

func acceptSub(sub: Sub): Unit {
}

func use() {
    let a = A<Super>()
    acceptSub(a.foo())
    acceptSub(bar(a))
}

main() {}
