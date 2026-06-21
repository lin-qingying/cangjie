// FILE: pkg/pkg.cj


package pkg

public func test<T>(a: T, b!: T = a): T {
    b
}

public class Context<X> {
    public static func test<T>(a: T,
        b!: T = {
            =>
            func test1(c!: T = a) {
                return c
            }
            func test2(c: T): T {
                return c
            }
            test1();
            test2(a);
        }()): T {
        return b
    }
}

public func run(c!: Int64 = 2): Int64 {
    var a: Int64 = test<Int64>(3)
    var b: Int32 = Context<Int64>.test<Int32>(5)
    if (a != 3 || b != 5 || c != 2) {
        return 1
    }
    return 0;
}

// FILE: main.cj


internal import pkg.*

main(): Int64 {
    var a: Int64 = test<Int64>(3)
    var b: Bool = Context<Int64>.test<Bool>(true)
    if (a != 3 || b != true || run() != 0) {
        return 1
    }
    return 0;
}
