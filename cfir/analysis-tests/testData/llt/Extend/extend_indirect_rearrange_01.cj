

interface I {
    func test(): Int64
}

open class A {}

extend A <: I {
    public func test(): Int64 {
        return 123
    }
}

class B <: A {}

func test<T>(a: T): Int64 where T <: I {
    return a.test()
}

main() {
    let v = B()
    if (test(v) != 123) {
        return 1
    }
    return 0
}
