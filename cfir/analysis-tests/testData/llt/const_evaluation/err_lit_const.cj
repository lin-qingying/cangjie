

const x = 0
const y = <!EXPECT_CONST!>"${x}"<!>

main() {
    println(y)
}
