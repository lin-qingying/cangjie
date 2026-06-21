

sealed interface I<T> {}

private class A <: I<Int64> {}

private class B <: I<Rune> {}

private class C<T> <: I<T> where T <: ToString {}

func f(x: I<Int64>) {
    match (x) {
        case _: A => 0
        case <!UNREACHABLE_PATTERN!>_: B<!> => -1 // unreachable
        case _: C<Int64> => 1
        case <!UNREACHABLE_PATTERN!>_: C<Rune><!> => -1 // unreachable
        case _ => -1
    }
}

main() {
    f(A())
}
