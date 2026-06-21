

func test(): Int64 {
    let a: Int64 = 1
    func abc(): Int64 {
        a
    }
    func dfg(a: () -> Int64): Int64 {
        a()
    }
    dfg(abc)
}

main(): Int64 {
    func foo(a!: Int64 = (1, 2)[0]): Int64 {
        1
    }
    if (test() != 1) {
        return 1
    }
    return 0;
}
