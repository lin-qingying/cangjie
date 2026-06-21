


class A<R> {}

class B<R> {
    <!CLASS_UNINITIALIZED_FIELD!>public let a: <!GENERIC_ARGUMENT_NO_MATCH!>R<R><!><!>
    <!CLASS_UNINITIALIZED_FIELD!>public let b: A<R><!>
}

func test<T>() {
    var a: <!GENERIC_ARGUMENT_NO_MATCH!>T<T><!>
}

main(): Int64 {
    return 0
}
