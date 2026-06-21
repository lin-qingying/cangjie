

struct B {
    public static let d: Int64 = 1
    public let lambda: () -> Int64 = {=> d}
}

main(): Int64 {
    if (B().lambda() == 1) {
        return 0
    }
    return 1
}
