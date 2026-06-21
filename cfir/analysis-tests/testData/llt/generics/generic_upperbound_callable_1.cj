

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(f: T) where T <: (Int) -> Int {
    func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>>(g: U) where U <: T {
        func baz<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>R<!>>(h: R) where R <: U {
            h(0)
        }
        g(1)
        baz(g)
    }
    f(2)
    bar(f)
}

main() {
    foo({x: Int => x})
}
