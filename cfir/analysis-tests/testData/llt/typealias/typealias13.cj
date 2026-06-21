

type B<T> = <!UNDECLARED_TYPE_NAME!>Cl<!><T>

func run<T>(a: T): Int64 {
    var b: B<T>
    return 0
}

main(): Int64 {
    return run<Int64>(1)
}
