

struct StructA {
    static const init() {}
    public static let A: Array<String> = <!EXPECT_CONST!>["array"]<!>
}

class ClassC {
    static const init() {}
    public static let A: Array<String> = <!EXPECT_CONST!>["array"]<!>
}

main() {
    return
}
