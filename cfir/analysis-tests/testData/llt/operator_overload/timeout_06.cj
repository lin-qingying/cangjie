

class A {
    operator func -() {
        A()
    }
}

class B {
    operator func -() {
        B()
    }
    operator func !() {
        B()
    }
}

func f(_: (A) -> A) {}

func f(_: (B) -> B) {}

main() {
    f({a => !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-a})
}
