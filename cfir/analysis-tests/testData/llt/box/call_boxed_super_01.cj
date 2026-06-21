

let res = 2

open class A {
    public func test(): Int64 {
        return res
    }
}

class B <: A {}

interface I1 {
    func test(): Int64
}

extend B <: I1 {}

main() {
    let b: I1 = B()
    if (b.test() != res) {
        return 1
    }
    return 0
}
