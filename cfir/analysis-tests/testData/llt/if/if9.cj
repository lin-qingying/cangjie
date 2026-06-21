


class A {}

class B {}

main() {
    let a = if (true) {
        A()
    } else {
        B()
    }
    let b: Object = a
    if (a is Object) {
        return 0
    }
    return 1
}
