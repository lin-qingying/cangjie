

class A {}

extend Int64 {
    public operator func *(a: A): A {
        return a
    }
}

func s(b: Int32) {
    1
}

func s(b: A) {
    2
}

main() {
    let a = A()
    s(8 * a)
}
