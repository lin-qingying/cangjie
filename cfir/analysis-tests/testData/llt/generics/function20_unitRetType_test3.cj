


func test<T1, T2, T3>(f: (T1) -> T2, g: (T2) -> T3): (T1) -> T3 {
    return {x: T1 => g(f(x))}
}

func a<T>(input: T): T {
    input
}

func b<T>(input: T): T {
    input
}

main() {
    test(a<Int32>, b<Int32>)
    return 0
}
