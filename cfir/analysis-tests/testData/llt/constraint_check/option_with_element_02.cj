


// Test for type inference of E<(Option<Int64>, Int64)> with literal (Int64, Int64)
enum E<T> {
    EE(T)
}

func test(): E<(Option<Int64>, Int64)> {
    return EE((1, 1))
}

main(): Unit {
    test()
}
