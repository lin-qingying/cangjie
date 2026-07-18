

func Add<T>(a: T): T {
    return a
}

main() {
    var a: Int64 = 1
    Add<Float64>(<!TYPE_MISMATCH!>a<!>)
    Add<Int64>(a)
    Add<<!UNDECLARED_TYPE_NAME!>T<!>>(a)
    Add(1)
}
