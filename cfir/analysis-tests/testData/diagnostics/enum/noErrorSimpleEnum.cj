enum Option1<T>{
    Some1(T) |
    None1
}

enum A{

    A1 |
    A1(Int64) |
    B

}

func name():Unit {
    A1
    A1(1)
    B
    Some1(1)
    None1<Int64>
    let c = A.A1
    let a :Option1<Int64> = None1
    let b  = Some1(1)
    let d = Option1<Int64>.Some1(1)
    let e = Option<Int64>.Some(1)
    let f = Some (1)
    let g = None<Int64>
}
