var ff: (Int64) -> Any = {v => ()}

func test<T>(f: (Int64) -> T) where T <: ToString {
    ff = f
}

main() {
    test<Bool>({v => false})
    0
}
