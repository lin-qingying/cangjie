

// DIAGNOSTICS: -UNUSED_VARIABLE

interface I<T> {
    func test(a: T) {
        return a
    }
}

class A <: I<Rune> & I<Int32> {}

func test<T>(a: T) where T <: I<Rune> & I<Int32> {
    let f = <!AMBIGUOUS_USE!>a.test<!>
    <!AMBIGUOUS_USE!>a.test<!>
    return 1
}

main() {
    return test(A())
}
