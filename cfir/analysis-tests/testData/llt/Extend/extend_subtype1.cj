

interface I {}

extend Int32 <: I {
    public func add() {
        0
    }
}

func test(a: I) {}

main() {
    let a: Int32 = 0
    test(a)
    return a.add()
}
