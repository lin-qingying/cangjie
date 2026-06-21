


interface I<T> {
    func test(a: T) {
        return a
    }
}

class A <: I<Rune> & I<Int32> {}

func goo(f: (Int32) -> Int32): Bool {
    return f(123) == 123
}

func coo(f: (Rune) -> (Rune)): Bool {
    return f(r'a') == r'a'
}

func test<T>(a: T) where T <: I<Rune> & I<Int32> {
    let f1: (Int32) -> Int32 = a.test
    let f2: (Rune) -> (Rune) = a.test
    if (f1(11) != 11 || f2(r'c') != r'c') {
        return 1
    }
    if (!goo(a.test) || !coo(a.test)) {
        return 1
    }
    return 0
}

main() {
    return test(A())
}
