

func test<T1, T2, T3>(f: (T1) -> T2, g: (T2) -> T3): (T1) -> T3 {
    return {x: T1 => g(f(x))}
}

public class A<T> {
    public var x: T
    public init(input: T) {
        x = input
    }
    public func a(input: T): T {
        x
    }

    public func b(input: T): T {
        x
    }
}

main() {
    test(A<Int64>(1).a, A<Int64>(1).b)
    A<Int64>(1).a ~> A<Int64>(1).b
    return 0
}
