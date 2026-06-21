

public class A<T> {
    public func runCases(cases: Array<(T, T)>) {
        for (i in 0..cases.size) {
            testCase(cases[i][0]);
        }
    }

    public static func testCase(input: T) {}
}

main() {
    return 0
}
