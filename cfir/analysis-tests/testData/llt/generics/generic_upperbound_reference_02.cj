

// DIAGNOSTICS: -UNUSED_VARIABLE

interface I<T> {
    func test(a: T) {
        return a
    }
}

class A <: I<Rune> & I<Int32> {}

func test<T>(a: T) where T <: I<Rune> & I<Int32> {
    let f = a.<!AMBIGUOUS_USE!>test<!>
    a.<!AMBIGUOUS_USE!>test<!>
    return 1
}

main() {
    return test(A())
}
