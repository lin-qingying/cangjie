


interface I {
    const func f(): Int64
    func f1(): Int64
    static const func f2(): Int64
}

const func g<T>(i: T) where T <: I {
    i.f() + T.f2()
}

class A {
    const init() {}
}

extend A <: I {
    public const func f() {
        0
    }
    public const func f1() {
        1
    }
    public static const func f2() {
        2
    }
}

main() {
    const a = A()
    g(a)
}
