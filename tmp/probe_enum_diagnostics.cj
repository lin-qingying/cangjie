enum Option1<T> {
    Some1(T) |
    None1
}

enum A {
    A1 |
    A1(Int64) |
    B
}

func probe(): Unit {
    let b = A(1)
    let c: A = None1
    let d: A = None1<Int64>
    let fOption1: Option1<Int64> = None
}
