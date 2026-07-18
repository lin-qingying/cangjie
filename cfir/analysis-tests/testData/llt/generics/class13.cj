


class A<R> {}

class B<R> {
    public let <!CLASS_UNINITIALIZED_FIELD!>a<!>: <!GENERIC_ARGUMENT_NO_MATCH!>R<R><!>
    public let <!CLASS_UNINITIALIZED_FIELD!>b<!>: A<R>
}

func test<T>() {
    var a: <!GENERIC_ARGUMENT_NO_MATCH!>T<T><!>
}

main(): Int64 {
    return 0
}
