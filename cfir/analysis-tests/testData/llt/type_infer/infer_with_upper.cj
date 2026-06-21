


interface I<T> {}

class A<T> where T <: I<T> {
    init() {}
}

struct B<T> where T <: I<T> {
    init() {}
}

func test<T>() where T <: I<T> {
    // test for error report for this 'A()'
    return <!UNABLE_TO_INFER_GENERIC_FUNC!>A()<!>
}

func test2<T>() where T <: I<T> {
    // test for error report for join of return type
    if (true) {
        return B<Int64>()
    } else {
        return ()
    }
}

extend Int64 <: I<Int64> {}

main() {
    test<Int64>()
    return 0
}
