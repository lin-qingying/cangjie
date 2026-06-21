

func f(_: Unit, _: Array<Bool>) {
    0
}

main() {
    f(unsafe { let _ = <!UNRESOLVED_REFERENCE!>wrong<!> }, [])
}
