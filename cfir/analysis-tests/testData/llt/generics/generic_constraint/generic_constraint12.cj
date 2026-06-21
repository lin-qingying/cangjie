

var ff: (Int64) -> Any = {v => ()}

func test<T>(f: (Int64) -> T) where T <: ToString {
    ff = <!ASSIGNMENT_TYPE_MISMATCH!>f<!>
}

main() {
    println(ff(1) is Unit)
    test<Bool>({v => false})
    let v = ff(1)
    println(v is Unit)
    println(v is Bool)
}
