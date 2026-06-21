


class B1 {
    let v = Some(B2())
    const init() {
    }
}

class B2 {
    let v = Some(B3())
    const init() {}
}

class B3 {
    let v = Some(B4())
    const init() {}
}

class B4 {
    let a = Some(1)
    const init() {}
}

const func foo() {
    let b = Some(B1())
    return <!EXPECT_CONST!>b?.v?.v?.v?.a<!>
}

main() {}
