


class A <: ToString {
    public func toString() {
        "A"
    }
}

var ff: (Int64) -> Any = {v => ()}

func test<T>(f: (Int64) -> T) where T <: ToString & A {
    ff = f
}

main() {
    if (!(ff(1) is Unit)) {
        return 1
    }
    test<A>({v => A()})
    let v = ff(1)
    if (v is Unit) {
        return 2
    }
    if (v is ToString) {
        return 0
    }
    return 3
}
