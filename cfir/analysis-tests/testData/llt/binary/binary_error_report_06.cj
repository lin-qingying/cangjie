

public enum E {
    A(Int64)
}

func foo(name!: E) {
    true
}

main() {
    foo(<!NEED_NAMED_ARGUMENT!>E.A(1)<!>) + 1
}
