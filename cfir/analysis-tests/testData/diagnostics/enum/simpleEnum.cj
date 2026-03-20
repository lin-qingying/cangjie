enum Option1<T>{
    Some(T) |
    None
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
    Some(1)
    None<Int64>
    let c = A.A1
    let a :Option1<Int64> = None
    let b  = Some(1)
    let d = Option1<Int64>.Some(1)
}