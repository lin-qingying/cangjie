

var global = 0

func f() {
    global++
    global
}

class A {
    let x = <!EXPECT_CONST!>f<!>()
    const init() {}
}

main() {}
