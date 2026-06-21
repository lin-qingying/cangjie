

enum C {
    c

    public func test(a: Int64) {
        a
    }
}

func bar(x: Int64) {
    1
}

main(): Int64 {
    var x = C.c
    let c1 = Option<C>.Some(x)
    let r1 = c1?.test
    return (r1 ?? bar)(0)
}
