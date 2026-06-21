

open class B {}

class C <: B {}

class A {
    public let b = 1
    public func test() {
        let a = 3
        func g(): Int64 {
            a + b
        }
        return g
    }
}

main(): Int64 {
    var ins = A()
    var val = ins.test()()
    if (val != 4) {
        return 1
    }
    return 0
}
