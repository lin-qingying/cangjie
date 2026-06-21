

const func test(a: Int64) {
    const b: Int64 = <!EXPECT_CONST!>a<!>
    return b
}

main() {}
