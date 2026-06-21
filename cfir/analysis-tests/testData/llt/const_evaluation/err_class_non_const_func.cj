

class A {
    func kiwi() {
        println('🥝')
    }

    const func f() {
        <!EXPECT_CONST!>kiwi<!>
    }

    const init() {}
}

main() {}
