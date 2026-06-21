

public class Test {
    public Test(private let cond: () -> Bool) {
        cond()
    }
    public const init(cond: Bool) {
        <!EXPECT_CONST!>this<!> {cond}
    }
    public func test() {
        println("ok")
    }
}

main() {
    Test(true).test()
}
