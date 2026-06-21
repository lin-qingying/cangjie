

class A {
    static func foo<T>(a: T) {
        bar<Int64>(1)
        a
    }
}

func bar<T>(a: T): T {
    let b = {a: Any => ()}(A.foo<Int64>)
    a
}

func baz<T>(a: T): T {
    a
}

main(): Int64 {
    if (bar(1) != 1) {
        return 1
    }
    return 0
}
