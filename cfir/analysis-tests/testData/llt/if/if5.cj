


class A {}

class B {}

func foo1(a: A): Unit {}

func foo2(a: B): Unit {}

main() {
    let a = if (true) {
        foo1
    } else {
        foo2
    }
    if (a is Any) {
        return 0
    }
    return 1
}
