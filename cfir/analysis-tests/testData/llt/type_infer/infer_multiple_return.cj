


interface I<T> {}

struct B<T> where T <: I<T> {
    init() {}
}

func test1<T>() where T <: I<T> {
    // test for error report for join of return type
    if (true) {
        return B<Int64>()
    } else if (false) {
        return 1
    } else {
        return ()
    }
}

func test2<T>(): Unit where T <: I<T> {
    // test for error report for join of return type and check
    if (true) {
        return <!RETURN_TYPE_MISMATCH!>B<Int64>()<!>
    } else if (false) {
        return <!RETURN_TYPE_MISMATCH!>1<!>
    } else {
        return ()
    }
}

extend Int64 <: I<Int64> {}

main() {
    test1<Int64>()
    return 0
}
