

main(): Int64 {
    return 0
}

func f(): Int32 {
    return 0
}

class A {
    public let a: () -> Int32
    public init(b: Int32) {
        a = f
    }
}
