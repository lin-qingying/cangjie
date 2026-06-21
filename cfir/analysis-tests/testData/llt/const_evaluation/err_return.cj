

func f() {
    const y = match (0) {
        case _ where <!EXPECT_CONST!>return<!> => 0
        case _ => 1
    }
}

main() {}
