enum Option1<T> {
    Some1(T) |
    None1
}

enum A {

    A1 |
    A1(Int64) |
    B

}

func name(): Unit {
    let _ = A1
    let _ = A1(1)
    let _ = B
    let _ = Some1(1)
    let _ = None1<Int64>
    let _ = A.A1
    let _: Option1<Int64> = None1
    let _ = Some1(1)
    let _ = Option1<Int64>.Some1(1)
    let _ = Option<Int64>.Some(1)
    let _ = Some(1)
    let _ = None<Int64>
}
