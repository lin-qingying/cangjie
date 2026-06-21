


struct StructA {
    public const init() {}
    public static let A: Array<String> = ["array"]
}

class ClassB {
    public const init() {}
    public static let A: Array<String> = ["array"]
}

class ClassD {
    static const init() {}
    private let A = ["array"]
}

class ClassE {
    static const init() {
    }
    const init(para: Int64) {
        b = para
    }
    static let A: Int64 = 10
    let b: Int64
}

main() {
    return
}
