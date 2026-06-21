


class A {}

func foo(): A {
    !if (true) {
        return A()
    } else {
        return return A()
    }
}

main() {
    let x = foo()
    if (x is A) {
        return 0
    }
    return 1
}
