

main() {
    while (true) {
        const x = match (0) {
            case _ where <!EXPECT_CONST!>continue<!> => 0
            case _ => 1
        }
    }
}
