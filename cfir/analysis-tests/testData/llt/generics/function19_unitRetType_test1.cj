

func test<T1, T2, T3>(f: (T1) -> T2, g: (T2) -> T3): (T1) -> T3 {
    return {x: T1 => g(f(x))}
}

func x(input: Unit): Unit {
    input
}

func y(input: Unit): Unit {
    input
}

main(): Int64 {
    test(x, y)
    return 0
}
