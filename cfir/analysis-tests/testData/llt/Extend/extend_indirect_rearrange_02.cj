

interface I<T> {
    func test(): Int64
}

open class A<T> {}

extend<T> A<T> <: I<T> {
    public func test(): Int64 {
        return 123
    }
}

class B<T> <: A<T> {}

class C <: A<Int64> {}

func test<T>(a: T): Int64 where T <: I<Int64> {
    return a.test()
}

main() {
    let v0 = B<Int64>()
    if (test(v0) != 123) {
        return 1
    }
    let v1 = C()
    if (test(v1) != 123) {
        return 2
    }
    return 0
}
